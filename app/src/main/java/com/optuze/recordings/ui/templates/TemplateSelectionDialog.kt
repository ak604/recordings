package com.optuze.recordings.ui.templates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.optuze.recordings.R
import com.optuze.recordings.data.models.Template
import com.optuze.recordings.databinding.DialogTemplateSelectionBinding

class TemplateSelectionDialog : DialogFragment() {
    private var _binding: DialogTemplateSelectionBinding? = null
    private val binding get() = _binding!!
    private lateinit var templateAdapter: TemplateAdapter
    private var recordingFilePath: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
        recordingFilePath = arguments?.getString(ARG_RECORDING_PATH)
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
        // TODO: Navigate to editor with recording and template
        // For now, just show a message and dismiss
        dismiss()
        
        // Here you would typically navigate to an editor activity/fragment
        // with the recording file path and template information
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        private const val ARG_RECORDING_PATH = "recording_path"
        
        fun newInstance(recordingPath: String?): TemplateSelectionDialog {
            val fragment = TemplateSelectionDialog()
            val args = Bundle()
            args.putString(ARG_RECORDING_PATH, recordingPath)
            fragment.arguments = args
            return fragment
        }
    }
} 