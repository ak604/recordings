package com.optuze.recordings.ui.templates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.optuze.recordings.R
import com.optuze.recordings.data.models.Template
import com.optuze.recordings.databinding.FragmentTemplateBinding

class TemplateFragment : Fragment() {
    private var _binding: FragmentTemplateBinding? = null
    private val binding get() = _binding!!
    private lateinit var templateAdapter: TemplateAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTemplateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadTemplates()
    }

    private fun setupRecyclerView() {
        templateAdapter = TemplateAdapter { template ->
            onTemplateSelected(template)
        }
        binding.rvTemplates.adapter = templateAdapter
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
        // Handle template selection
        Toast.makeText(context, "Selected: ${template.title}", Toast.LENGTH_SHORT).show()
        // TODO: Navigate to template detail/editor screen
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 