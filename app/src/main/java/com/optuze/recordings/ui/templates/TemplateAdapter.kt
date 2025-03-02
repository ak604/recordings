package com.optuze.recordings.ui.templates

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.optuze.recordings.data.models.Template
import com.optuze.recordings.databinding.ItemTemplateBinding

class TemplateAdapter(
    private val onTemplateClick: (Template) -> Unit
) : ListAdapter<Template, TemplateAdapter.TemplateViewHolder>(TemplateDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val binding = ItemTemplateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TemplateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TemplateViewHolder(
        private val binding: ItemTemplateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTemplateClick(getItem(position))
                }
            }
        }

        fun bind(template: Template) {
            binding.apply {
                ivTemplateIcon.setImageResource(template.iconResId)
                ivTemplateIcon.setColorFilter(
                    ContextCompat.getColor(root.context, template.colorResId),
                    PorterDuff.Mode.SRC_IN
                )
                tvTemplateTitle.text = template.title
                tvTemplateDescription.text = template.description
            }
        }
    }

    private class TemplateDiffCallback : DiffUtil.ItemCallback<Template>() {
        override fun areItemsTheSame(oldItem: Template, newItem: Template): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Template, newItem: Template): Boolean {
            return oldItem == newItem
        }
    }
} 