package com.optuze.recordings.ui.recordings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.optuze.recordings.R
import com.optuze.recordings.data.models.Call

class CallAdapter(private val itemClickListener: (Call) -> Unit) : 
    ListAdapter<Call, CallAdapter.CallViewHolder>(CallDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_call, parent, false)
        return CallViewHolder(view, itemClickListener)
    }

    override fun onBindViewHolder(holder: CallViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CallViewHolder(
        itemView: View, 
        private val itemClickListener: (Call) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val tvTitle: TextView = itemView.findViewById(R.id.tvCallTitle)
        private val tvDate: TextView = itemView.findViewById(R.id.tvCallDate)
        private val tvTranscription: TextView = itemView.findViewById(R.id.tvCallTranscription)
        
        fun bind(call: Call) {
            tvTitle.text = "Recording: ${call.callId}"
            tvDate.text = call.getFormattedDate()
            tvTranscription.text = call.getShortTranscription()
            
            itemView.setOnClickListener {
                itemClickListener(call)
            }
        }
    }
    
    private class CallDiffCallback : DiffUtil.ItemCallback<Call>() {
        override fun areItemsTheSame(oldItem: Call, newItem: Call): Boolean {
            return oldItem.callId == newItem.callId
        }

        override fun areContentsTheSame(oldItem: Call, newItem: Call): Boolean {
            return oldItem == newItem
        }
    }
} 