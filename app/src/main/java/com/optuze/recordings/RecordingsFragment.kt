package com.optuze.recordings

import CallAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.optuze.recordings.data.NetworkModule
import com.optuze.recordings.data.SessionManager
import com.optuze.recordings.data.api.CallService
import com.optuze.recordings.data.models.Call
import com.optuze.recordings.databinding.FragmentRecordingsBinding
import com.optuze.recordings.ui.templates.ProcessedTemplateFragment
import com.optuze.recordings.ui.templates.TemplateSelectionDialog
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
        callAdapter = CallAdapter(
            onPlayClickListener = { call ->
                playRecording(call)
            },
            onTemplateClickListener = { call ->
                showTemplateSelectionDialog(call)
            },
            onTemplateChipClickListener = { call, templateName ->
                showProcessedTemplate(call, templateName)
            },
            onTranscriptionChipClickListener = { call ->
                showTranscription(call)
            },
            onDeleteClickListener = { call ->
                confirmDeleteRecording(call)
            }
        )
        
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
    
    private fun playRecording(call: Call) {
        // Implement audio playback logic
        Toast.makeText(requireContext(), "Playing ${call.fileName}", Toast.LENGTH_SHORT).show()
        // Code to play the audio file from S3 or local cache
    }
    
    private fun showTemplateSelectionDialog(call: Call) {
        // Show template selection dialog
        val dialog = TemplateSelectionDialog.newInstance(call.fileName, call.callId)
        dialog.show(childFragmentManager, "TemplateSelectionDialog")
    }
    
    private fun showProcessedTemplate(call: Call, templateName: String) {
        // Show the processed template
        val templateContent = call.templates?.get(templateName)
        if (templateContent != null) {
            val displayName = templateName.split("_").joinToString(" ") { 
                it.replaceFirstChar { char -> char.uppercase() } 
            }
            
            onTemplateProcessed(displayName, templateContent)
        } else {
            Toast.makeText(requireContext(), "Template content not available", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun confirmDeleteRecording(call: Call) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Recording")
            .setMessage("Are you sure you want to delete this recording?")
            .setPositiveButton("Delete") { _, _ ->
                deleteRecording(call)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteRecording(call: Call) {
        // Implementation for deleting the recording
        // This would involve an API call to your backend
        Toast.makeText(requireContext(), "Deleting recording...", Toast.LENGTH_SHORT).show()
        
        // Example implementation:
        /*
        lifecycleScope.launch {
            try {
                val response = callService.deleteCall(call.callId)
                if (response.isSuccessful && response.body()?.success == true) {
                    // Remove from list
                    val updatedList = calls.filterNot { it.callId == call.callId }
                    calls.clear()
                    calls.addAll(updatedList)
                    callAdapter.submitList(calls.toList())
                    
                    // Show success message
                    Toast.makeText(requireContext(), "Recording deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to delete recording", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        */
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
    
    private fun showTranscription(call: Call) {
        // Create a dialog to show the full transcription
        AlertDialog.Builder(requireContext())
            .setTitle("Transcription")
            .setMessage(call.transcription ?: "No transcription available")
            .setPositiveButton("Close", null)
            .show()
    }
} 