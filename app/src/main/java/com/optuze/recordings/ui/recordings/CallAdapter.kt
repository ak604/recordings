import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.optuze.recordings.R
import com.optuze.recordings.data.models.Call
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.TimeZone

class CallAdapter(
    private val onPlayClickListener: (Call) -> Unit,
    private val onTemplateClickListener: (Call) -> Unit,
    private val onDeleteClickListener: (Call) -> Unit,
    private val onTranscriptionChipClickListener: (Call) -> Unit,
    private val onTemplateChipClickListener: (Call, String) -> Unit
) : ListAdapter<Call, CallAdapter.CallViewHolder>(CallDiffCallback()) {

    private var currentlyPlayingCallId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_call, parent, false)
        return CallViewHolder(view)
    }

    override fun onBindViewHolder(holder: CallViewHolder, position: Int) {
        val call = getItem(position)
        holder.bind(call)
    }

    fun setCurrentlyPlayingCallId(callId: String?) {
        val oldPlayingCallId = currentlyPlayingCallId
        currentlyPlayingCallId = callId
        
        // Update UI for previously playing item (if any)
        if (oldPlayingCallId != null) {
            val oldPosition = currentList.indexOfFirst { it.callId == oldPlayingCallId }
            if (oldPosition >= 0) {
                notifyItemChanged(oldPosition)
            }
        }
        
        // Update UI for currently playing item (if any)
        if (callId != null) {
            val newPosition = currentList.indexOfFirst { it.callId == callId }
            if (newPosition >= 0) {
                notifyItemChanged(newPosition)
            }
        }
    }

    inner class CallViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvCallDate)
        private val tvRecordingLabel: TextView = itemView.findViewById(R.id.tvRecordingLabel)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvCallDuration)
        private val btnPlayAudio: MaterialButton = itemView.findViewById(R.id.btnPlayAudio)
        private val btnAddTemplate: MaterialButton = itemView.findViewById(R.id.btnAddTemplate)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)
        private val chipGroupAll: ChipGroup = itemView.findViewById(R.id.chipGroupAll)

        fun bind(call: Call) {
            tvDuration.text = call.getFormattedDuration()
            tvDate.text = call.getFormattedDate()
            tvRecordingLabel.text = call.getShortTranscription()
            // Set up chips (transcription first, then templates)
            setupChips(call)
            
            // Set up button listeners
            btnPlayAudio.setOnClickListener { onPlayClickListener(call) }
            btnAddTemplate.setOnClickListener { onTemplateClickListener(call) }
            btnDelete.setOnClickListener { onDeleteClickListener(call) }
            
            // Update play button appearance based on playback state
            if (call.callId == currentlyPlayingCallId) {
                btnPlayAudio.setIconResource(android.R.drawable.ic_media_pause)
            } else {
                btnPlayAudio.setIconResource(android.R.drawable.ic_media_play)
            }
        }
        
        private fun setupChips(call: Call) {
            chipGroupAll.removeAllViews()
            
            // Add transcription chip first
            val transcriptionChip = Chip(itemView.context).apply {
                text = "Transcription"
                isClickable = true
                isCheckable = false
                chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, android.R.color.holo_blue_light)
                )
                chipIcon = ContextCompat.getDrawable(context, R.drawable.ic_transcription)
                setOnClickListener { 
                    onTranscriptionChipClickListener(call)
                }
            }
            chipGroupAll.addView(transcriptionChip)
            
            // Add template chips
            call.templates?.keys?.forEach { templateName ->
                val chip = Chip(itemView.context).apply {
                    text = templateName.capitalize()
                    isClickable = true
                    isCheckable = false
                    setOnClickListener { 
                        onTemplateChipClickListener(call, templateName)
                    }
                }
                chipGroupAll.addView(chip)
            }
            
            // If no templates, just show the transcription chip
            if (call.templates.isNullOrEmpty()) {
                // We already added the transcription chip, so we're good
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
    
    private fun String.capitalize(): String {
        return this.split("_").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
} 