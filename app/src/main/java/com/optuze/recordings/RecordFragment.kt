package com.optuze.recordings

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
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
            updateUI()
            
            Log.d(TAG, "Recording stopped. Preparing upload...")
            uploadRecording()
            
        } catch (e: Exception) {
            handleRecordingError("Failed to stop recording: ${e.message}")
        }
    }
    
    private fun uploadRecording() {
        val file = outputFile?.let { File(it) }
        if (file == null || !file.exists()) {
            handleRecordingError("Recording file not found")
            return
        }

        binding.tvRecordingStatus.text = "Preparing upload..."
        binding.tvRecordingStatus.visibility = View.VISIBLE
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRecord.isEnabled = false

        lifecycleScope.launch {
            try {
                // Generate final filename with metadata
                val userId = sessionManager.getUser()?.userId ?: "anonymous"
                val packageId = requireContext().packageName
                val uniqueId = UUID.randomUUID().toString().substring(0, 8)
                val fileName = "${uniqueId}.mp3"

                // Get presigned URL
                val presignedResponse = s3Service.getPresignedUrl(fileName, "audio/mp3")
                if (!presignedResponse.isSuccessful || presignedResponse.body() == null) {
                    throw Exception("Failed to get upload URL: ${presignedResponse.errorBody()?.string()}")
                }

                // Upload file
                val presignedUrl = presignedResponse.body()!!.uploadURL
                val success = FileUploader.uploadFile(file, presignedUrl)

                if (success) {
                    Log.d(TAG, "File uploaded successfully as $fileName")
                    showTemplateSelectionDialog(fileName, uniqueId)
                } else {
                    throw Exception("Upload failed with unknown error")
                }
            } catch (e: Exception) {
                handleRecordingError("Upload failed: ${e.message}")
            } finally {
                // Clean up temp file
                file?.delete()
                
                withContext(Dispatchers.Main) {
                    binding.tvRecordingStatus.visibility = View.GONE
                    binding.progressBar.visibility = View.GONE
                    binding.btnRecord.isEnabled = true
                }
            }
        }
    }
    
    private fun updateUI() {
        binding.progressBar.visibility = View.GONE
        binding.btnRecord.isEnabled = true
        
        if (isRecording) {
            binding.btnRecord.setImageResource(R.drawable.ic_stop)
            binding.tvRecordingStatus.text = "Recording..."
            binding.tvRecordingStatus.visibility = View.VISIBLE
        } else {
            binding.btnRecord.setImageResource(R.drawable.ic_record)
            binding.tvRecordingStatus.visibility = View.GONE
        }
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
    
    companion object {
        private const val TAG = "RecordFragment"
    }
} 