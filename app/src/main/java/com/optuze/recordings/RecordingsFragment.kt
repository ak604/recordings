package com.optuze.recordings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.optuze.recordings.data.NetworkModule
import com.optuze.recordings.data.SessionManager
import com.optuze.recordings.data.api.CallService
import com.optuze.recordings.data.models.Call
import com.optuze.recordings.databinding.FragmentRecordingsBinding
import com.optuze.recordings.ui.recordings.CallAdapter
import com.optuze.recordings.ui.templates.ProcessedTemplateFragment
import com.optuze.recordings.ui.templates.TemplateSelectionListener
import kotlinx.coroutines.launch
import org.json.JSONObject

class RecordingsFragment : Fragment(), TemplateSelectionListener {
    private var _binding: FragmentRecordingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var callAdapter: CallAdapter
    private lateinit var callService: CallService
    private lateinit var sessionManager: SessionManager
    
    private var nextToken: String? = null
    private var isLoading = false
    private val calls = mutableListOf<Call>()
    
    companion object {
        private const val TAG = "RecordingsFragment"
        private const val PAGE_SIZE = 10
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sessionManager = SessionManager(requireContext())
        callService = NetworkModule.createCallService(sessionManager)
        
        setupRecyclerView()
        setupLoadMoreButton()
        
        // Load initial data
        loadCalls()
    }
    
    private fun setupRecyclerView() {
        callAdapter = CallAdapter { call ->
            onCallSelected(call)
        }
        
        binding.rvCalls.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = callAdapter
        }
    }
    
    private fun setupLoadMoreButton() {
        binding.btnLoadMore.setOnClickListener {
            loadCalls()
        }
    }
    
    private fun loadCalls() {
        if (isLoading) return
        
        isLoading = true
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val response = callService.getCalls(PAGE_SIZE, nextToken)
                
                if (response.isSuccessful && response.body() != null) {
                    val callsResponse = response.body()!!
                    
                    if (callsResponse.success) {
                        val newCalls = callsResponse.data
                        nextToken = callsResponse.nextToken
                        
                        // Add new calls to the list
                        calls.addAll(newCalls)
                        callAdapter.submitList(calls.toList())
                        
                        // Show/hide empty state
                        binding.tvEmptyState.visibility = if (calls.isEmpty()) View.VISIBLE else View.GONE
                        
                        // Show/hide load more button based on nextToken
                        binding.btnLoadMore.visibility = if (nextToken != null) View.VISIBLE else View.GONE
                    } else {
                        showError("Failed to load recordings")
                    }
                } else {
                    showError("Error: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading calls", e)
                showError("Network error: ${e.message}")
            } finally {
                isLoading = false
                showLoading(false)
            }
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLoadMore.visibility = if (!isLoading && nextToken != null) View.VISIBLE else View.GONE
    }
    
    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
    
    private fun onCallSelected(call: Call) {
        // Get the blog template content if available
        val blogTemplateJson = call.templates?.get("blog")
        
        if (blogTemplateJson != null) {
            // Navigate to processed template fragment
            val templateName = "Blog"
            val fragment = ProcessedTemplateFragment.newInstance(templateName, blogTemplateJson)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        } else {
            showError("No template content available for this recording")
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    override fun onTemplateProcessed(templateName: String, templateContent: String) {
        val fragment = ProcessedTemplateFragment.newInstance(templateName, templateContent)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
} 