data class Call(
    val callId: String,
    val fileName: String,
    val createdAt: String,
    val transcription: String?,
    val templates: Map<String, String>?,
    val durationSeconds: Int? = null
) {
    

    fun getFormattedDuration(): String {
        if (durationSeconds == null) return "00:00"
        
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
} 