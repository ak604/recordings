package com.optuze.recordings

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.optuze.recordings.data.FileUploader
import com.optuze.recordings.data.NetworkModule
import com.optuze.recordings.data.SessionManager
import com.optuze.recordings.data.api.S3Service
import com.optuze.recordings.data.models.PresignedUrlResponse
import com.optuze.recordings.data.AppConfigManager
import com.optuze.recordings.databinding.FragmentRecordBinding
import com.optuze.recordings.ui.templates.ProcessedTemplateFragment
import com.optuze.recordings.ui.templates.TemplateSelectionDialog
import com.optuze.recordings.ui.templates.TemplateSelectionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.UUID

class RecordFragment : Fragment(), TemplateSelectionListener {
    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!
    
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var outputFile: String? = null
    private var presignedUrlResponse: PresignedUrlResponse? = null
    private var audioFile: File? = null
    
    private lateinit var sessionManager: SessionManager
    private lateinit var s3Service: S3Service
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(
                requireContext(),
                "Audio recording permission denied",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sessionManager = SessionManager(requireContext())
        s3Service = NetworkModule.createS3Service(sessionManager)

        binding.btnRecord.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                checkPermissionAndRecord()
            }
        }

        binding.btnDiscard.setOnClickListener { discardRecording() }
        binding.btnConvertContainer.setOnClickListener { prepareAndUploadForConversion() }
        binding.btnFinishContainer.setOnClickListener { uploadWithoutTemplates() }
    }
    
    private fun checkPermissionAndRecord() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startRecording()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                showPermissionRationale()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    
    private fun startRecording() {
        try {
            // Create temp file with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val tempFileName = "temp_recording_${timestamp}.mp3"
            
            val recordingsDir = File(
                requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "Recordings"
            ).apply { mkdirs() }
            
            outputFile = "${recordingsDir.absolutePath}/$tempFileName"
            
            // Initialize MediaRecorder
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile)
                prepare()
                start()
            }
            
            isRecording = true
            updateUI()
            Log.d(TAG, "Recording started to temp file: $outputFile")
            
        } catch (e: IOException) {
            handleRecordingError("Failed to start recording: ${e.message}")
        }
    }
    
    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            
            // Store reference to the audio file
            audioFile = outputFile?.let { File(it) }
            if (audioFile == null || !audioFile!!.exists()) {
                handleRecordingError("Recording file not found")
                return
            }
            
            // Just update UI to show buttons - no upload here
            updateUI()
            Log.d(TAG, "Recording stopped successfully")
            
        } catch (e: Exception) {
            handleRecordingError("Failed to stop recording: ${e.message}")
        }
    }
    
    private fun updateUI() {
        binding.progressBar.visibility = View.GONE
        binding.btnRecord.isEnabled = true
        
        if (isRecording) {
            // Recording in progress
            binding.btnRecord.setImageResource(R.drawable.ic_stop)
            binding.tvRecordingStatus.text = "Recording..."
            binding.tvRecordingStatus.visibility = View.VISIBLE
            
            // Hide action buttons and costs
            binding.btnDiscard.visibility = View.GONE
            binding.btnConvertContainer.visibility = View.GONE
            binding.btnFinishContainer.visibility = View.GONE
            binding.layoutConvertCost.visibility = View.GONE
            binding.layoutFinishCost.visibility = View.GONE
            
        } else if (audioFile != null) {
            // Recording completed, show action buttons
            binding.btnRecord.setImageResource(R.drawable.ic_record)
            binding.tvRecordingStatus.text = "Recording completed"
            binding.tvRecordingStatus.visibility = View.VISIBLE
            
            // Show action buttons and costs
            binding.btnDiscard.visibility = View.VISIBLE
            binding.btnConvertContainer.visibility = View.VISIBLE
            binding.btnFinishContainer.visibility = View.VISIBLE
            binding.layoutConvertCost.visibility = View.VISIBLE
            binding.layoutFinishCost.visibility = View.VISIBLE
            
        } else {
            // Initial state
            binding.btnRecord.setImageResource(R.drawable.ic_record)
            binding.tvRecordingStatus.visibility = View.GONE
            
            // Hide action buttons and costs
            binding.btnDiscard.visibility = View.GONE
            binding.btnConvertContainer.visibility = View.GONE
            binding.btnFinishContainer.visibility = View.GONE
            binding.layoutConvertCost.visibility = View.GONE
            binding.layoutFinishCost.visibility = View.GONE
        }
    }
    
    private fun discardRecording() {
        audioFile?.delete()
        audioFile = null
        outputFile = null
        resetUI()
        Toast.makeText(requireContext(), "Recording discarded", Toast.LENGTH_SHORT).show()
    }
    
    private fun prepareAndUploadForConversion() {
        if (audioFile == null || !audioFile!!.exists()) {
            handleRecordingError("Recording file not found")
            return
        }
        uploadRecording(true)  // Upload with template selection
    }
    
    private fun uploadWithoutTemplates() {
        if (audioFile == null || !audioFile!!.exists()) {
            handleRecordingError("Recording file not found")
            return
        }
        uploadRecording(false)  // Upload without template selection
    }
    
    private fun uploadRecording(showTemplates: Boolean) {
        // Show loading state
        binding.tvRecordingStatus.text = "Preparing upload..."
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRecord.isEnabled = false
        
        // Hide action buttons during upload
        binding.btnDiscard.visibility = View.GONE
        binding.btnConvertContainer.visibility = View.GONE
        binding.btnFinishContainer.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val uniqueId = "${Instant.now().epochSecond}_${UUID.randomUUID().toString().substring(0, 8)}"
                val fileName = "${uniqueId}.mp3"
                val durationSeconds = getAudioDurationInSeconds(audioFile!!.absolutePath)

                val presignedResponse = s3Service.getPresignedUrl(
                    fileName = fileName,
                    fileType = "audio/mp3",
                    durationSeconds = durationSeconds
                )

                if (!presignedResponse.isSuccessful || presignedResponse.body() == null) {
                    throw Exception("Failed to get upload URL: ${presignedResponse.errorBody()?.string()}")
                }

                val success = FileUploader.uploadFile(audioFile!!, presignedResponse.body()!!.uploadURL)

                if (success) {
                    if (showTemplates) {
                        // Show template selection dialog for Convert flow
                        showTemplateSelectionDialog(fileName, uniqueId)
                    } else {
                        // Direct upload for Finish flow
                        Toast.makeText(requireContext(), "Recording uploaded successfully", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                } else {
                    throw Exception("Upload failed")
                }
            } catch (e: Exception) {
                handleRecordingError("Upload failed: ${e.message}")
            } finally {
                // Cleanup
                audioFile?.delete()
                audioFile = null
                outputFile = null
                
                withContext(Dispatchers.Main) {
                    updateUI()
                }
            }
        }
    }
    
    private fun resetUI() {
        audioFile = null
        outputFile = null
        updateUI()
    }
    
    private fun showTemplateSelectionDialog(fileName: String, callId: String) {
        val dialog = TemplateSelectionDialog.newInstance(fileName, callId)
        dialog.show(childFragmentManager, "TemplateSelectionDialog")
    }
    
    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        if (isRecording) {
            stopRecording()
        }
        _binding = null
    }
    
    // Implement the callback method
    override fun onTemplateProcessed(templateName: String, templateContent: String) {
        val fragment = ProcessedTemplateFragment.newInstance(templateName, templateContent)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("processed_template")
            .commit()
    }
    
    private fun handleRecordingError(message: String) {
        Log.e(TAG, message)
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        binding.progressBar.visibility = View.GONE
        binding.btnRecord.isEnabled = true
    }
    
    private fun showPermissionRationale() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Needed")
            .setMessage("This app needs access to your microphone to record audio.")
            .setPositiveButton("OK") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
    
    private fun getAudioDurationInSeconds(filePath: String): Int {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            return (durationStr?.toInt() ?: 0) / 1000 // Convert milliseconds to seconds
        } catch (e: Exception) {
            Log.e(TAG, "Error getting audio duration", e)
            return 0
        } finally {
            retriever.release()
        }
    }
    
    private fun updateButtonCosts(durationSeconds: Int) {
        val tokenName = AppConfigManager.getTokenName()
        val tokenCost = AppConfigManager.calculateTokenCost(durationSeconds)
        
        // Update cost labels with just the number
        binding.tvConvertCost.text = tokenCost.toString()
        binding.tvFinishCost.text = tokenCost.toString()
        
        // Log the cost for debugging
        Log.d(TAG, "Token cost for duration $durationSeconds seconds: $tokenCost $tokenName")
    }
    
    companion object {
        private const val TAG = "RecordFragment"
    }
} 