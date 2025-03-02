package com.optuze.recordings.ui.templates

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.optuze.recordings.R
import com.optuze.recordings.databinding.FragmentProcessedTemplateBinding
import org.json.JSONObject

class ProcessedTemplateFragment : Fragment() {
    private var _binding: FragmentProcessedTemplateBinding? = null
    private val binding get() = _binding!!
    
    private var templateName: String? = null
    private var templateContent: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            templateName = it.getString(ARG_TEMPLATE_NAME)
            templateContent = it.getString(ARG_TEMPLATE_CONTENT)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProcessedTemplateBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        
        binding.tvTemplateName.text = templateName ?: "Template"
        
        // Parse and display the template content
        templateContent?.let { content ->
            try {
                // Parse the JSON string
                val jsonObject = JSONObject(content)
                
                // Display different fields based on template type
                when (templateName?.lowercase()) {
                    "blog" -> displayBlogTemplate(jsonObject)
                    "summary" -> displaySummaryTemplate(jsonObject)
                    "memo" -> displayMemoTemplate(jsonObject)
                    // Add more template types as needed
                    else -> displayGenericTemplate(jsonObject)
                }
            } catch (e: Exception) {
                binding.tvContent.text = content
            }
        }
        
        binding.btnCopy.setOnClickListener {
            copyToClipboard(binding.tvContent.text.toString())
        }
    }
    
    private fun displayBlogTemplate(json: JSONObject) {
        val title = json.optString("title", "")
        val summary = json.optString("summary", "")
        val content = json.optString("content", "")
        val tags = json.optJSONArray("tags")
        val readTime = json.optInt("estimatedReadTime", 0)
        
        val tagsText = StringBuilder()
        for (i in 0 until (tags?.length() ?: 0)) {
            tagsText.append("#").append(tags?.getString(i)).append(" ")
        }
        
        val formattedContent = """
            # $title
            
            $summary
            
            $content
            
            Tags: $tagsText
            
            Estimated reading time: $readTime minute(s)
        """.trimIndent()
        
        binding.tvContent.text = formattedContent
    }
    
    private fun displaySummaryTemplate(json: JSONObject) {
        val title = json.optString("title", "")
        val summary = json.optString("summary", "")
        
        val formattedContent = """
            # $title
            
            $summary
        """.trimIndent()
        
        binding.tvContent.text = formattedContent
    }
    
    private fun displayMemoTemplate(json: JSONObject) {
        val title = json.optString("title", "")
        val content = json.optString("content", "")
        
        val formattedContent = """
            # $title
            
            $content
        """.trimIndent()
        
        binding.tvContent.text = formattedContent
    }
    
    private fun displayGenericTemplate(json: JSONObject) {
        val formattedContent = StringBuilder()
        
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = json.get(key)
            formattedContent.append("$key: $value\n\n")
        }
        
        binding.tvContent.text = formattedContent.toString()
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Template Content", text)
        clipboard.setPrimaryClip(clip)
        
        Toast.makeText(requireContext(), "Content copied to clipboard", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        private const val ARG_TEMPLATE_NAME = "template_name"
        private const val ARG_TEMPLATE_CONTENT = "template_content"
        
        fun newInstance(templateName: String, templateContent: String): ProcessedTemplateFragment {
            return ProcessedTemplateFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TEMPLATE_NAME, templateName)
                    putString(ARG_TEMPLATE_CONTENT, templateContent)
                }
            }
        }
    }
} 