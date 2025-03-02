package com.optuze.recordings.ui.templates

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.optuze.recordings.R
import com.optuze.recordings.data.NetworkModule
import com.optuze.recordings.data.SessionManager
import com.optuze.recordings.data.api.TemplateService
import com.optuze.recordings.data.models.Template
import com.optuze.recordings.databinding.DialogTemplateSelectionBinding
import kotlinx.coroutines.launch

class TemplateSelectionDialog : DialogFragment() {
    private var _binding: DialogTemplateSelectionBinding? = null
    private val binding get() = _binding!!
    private lateinit var templateAdapter: TemplateAdapter
    
    private var fileName: String? = null
    private var callId: String? = null
    
    private lateinit var templateService: TemplateService
    private lateinit var sessionManager: SessionManager
    
    private var listener: TemplateSelectionListener? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
        
        fileName = arguments?.getString(ARG_FILENAME)
        callId = arguments?.getString(ARG_CALL_ID)
        
        sessionManager = SessionManager(requireContext())
        templateService = NetworkModule.createTemplateService(sessionManager)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTemplateSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.tvDialogTitle.text = "Select a Template"
        binding.btnClose.setOnClickListener { dismiss() }
        
        setupRecyclerView()
        loadTemplates()
    }
    
    private fun setupRecyclerView() {
        templateAdapter = TemplateAdapter { template ->
            onTemplateSelected(template)
        }
        binding.rvTemplates.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = templateAdapter
        }
    }
    
    private fun loadTemplates() {
        val templates = listOf(
            Template(
                id = 1,
                title = "Quick Memo",
                description = "Create a quick note or memo",
                iconResId = R.drawable.ic_memo,
                colorResId = R.color.template_memo
            ),
            Template(
                id = 2,
                title = "Summary",
                description = "Generate a concise summary",
                iconResId = R.drawable.ic_summary,
                colorResId = R.color.template_summary
            ),
            Template(
                id = 3,
                title = "Casual Email",
                description = "Write a friendly, informal email",
                iconResId = R.drawable.ic_email_casual,
                colorResId = R.color.template_email_casual
            ),
            Template(
                id = 4,
                title = "Formal Email",
                description = "Compose a professional business email",
                iconResId = R.drawable.ic_email_formal,
                colorResId = R.color.template_email_formal
            ),
            Template(
                id = 5,
                title = "Tweet",
                description = "Create an engaging tweet",
                iconResId = R.drawable.ic_twitter,
                colorResId = R.color.template_twitter
            ),
            Template(
                id = 6,
                title = "LinkedIn Post",
                description = "Write a professional social media post",
                iconResId = R.drawable.ic_linkedin,
                colorResId = R.color.template_linkedin
            ),
            Template(
                id = 7,
                title = "Blog",
                description = "Draft a blog post or article",
                iconResId = R.drawable.ic_blog,
                colorResId = R.color.template_blog
            )
        )
        
        templateAdapter.submitList(templates)
    }
    
    private fun onTemplateSelected(template: Template) {
        binding.progressBar.visibility = View.VISIBLE
        
        val contextId = requireContext().packageName
        val templateName = template.title.lowercase().replace(" ", "_")
        
        lifecycleScope.launch {
            try {
                val response = templateService.processTemplate(
                    callId ?: "",
                    templateName,
                    contextId
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val processingResponse = response.body()!!
                    
                    // Get the content for the selected template
                    val templateContent = processingResponse.data.templates[templateName]
                    
                    if (templateContent != null) {
                        // Trigger the callback with both template name and content
                        listener?.onTemplateProcessed(template.title, templateContent)
                        dismiss()
                    } else {
                        showError("Template content not found")
                    }
                } else {
                    showError("Failed to process template: ${response.message()}")
                }
            } catch (e: Exception) {
                showError("Error processing template: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun showError(message: String) {
        Log.e("TemplateSelectionDialog", message)
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = when {
            parentFragment is TemplateSelectionListener -> parentFragment as TemplateSelectionListener
            context is TemplateSelectionListener -> context
            else -> null
        }
        
        if (listener == null) {
            Log.w("TemplateSelectionDialog", "No listener found!")
        }
    }
    
    companion object {
        private const val ARG_FILENAME = "file_name"
        private const val ARG_CALL_ID = "call_id"
        private const val TAG = "TemplateSelectionDialog"
        
        fun newInstance(fileName: String?, callId: String?): TemplateSelectionDialog {
            val fragment = TemplateSelectionDialog()
            val args = Bundle()
            args.putString(ARG_FILENAME, fileName)
            args.putString(ARG_CALL_ID, callId)
            fragment.arguments = args
            return fragment
        }
    }
} 