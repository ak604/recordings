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
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
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
            initiateRecording()
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
                initiateRecording()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                Toast.makeText(
                    requireContext(),
                    "Audio recording permission is required",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    
    private fun initiateRecording() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRecord.isEnabled = false
        
        val callId = UUID.randomUUID().toString().take(8)
        val fileName = "${callId}.mp3"
        
        Log.d(TAG, "Generated filename: $fileName with callId: $callId")
        
        // Get presigned URL
        lifecycleScope.launch {
            try {
                val response = s3Service.getPresignedUrl(fileName, "audio/mp3")
                
                if (response.isSuccessful && response.body() != null) {
                    presignedUrlResponse = response.body()
                    Log.d(TAG, "Got presigned URL: ${presignedUrlResponse?.uploadURL}")
                    startRecording(fileName, callId)
                } else {
                    Log.e(TAG, "Failed to get presigned URL: ${response.errorBody()?.string()}")
                    Toast.makeText(
                        requireContext(),
                        "Failed to prepare recording",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = View.GONE
                    binding.btnRecord.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting presigned URL", e)
                Toast.makeText(
                    requireContext(),
                    "Error preparing recording: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                binding.progressBar.visibility = View.GONE
                binding.btnRecord.isEnabled = true
            }
        }
    }
    
    private fun startRecording(fileName: String, callId: String) {
        try {
            // Create output file
            val recordingsDir = File(
                requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "Recordings"
            )
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs()
            }
            
            outputFile = "${recordingsDir.absolutePath}/$fileName"
            
            // Initialize MediaRecorder
            mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(requireContext())
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile)
                prepare()
                start()
            }
            
            isRecording = true
            updateUI()
            
            Log.d(TAG, "Recording started: $outputFile with callId: $callId")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start recording", e)
            Toast.makeText(
                requireContext(),
                "Failed to start recording: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            binding.progressBar.visibility = View.GONE
            binding.btnRecord.isEnabled = true
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
            
            Log.d(TAG, "Recording stopped: $outputFile")
            
            // Upload the recorded file
            uploadRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            Toast.makeText(
                requireContext(),
                "Failed to stop recording: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            binding.progressBar.visibility = View.GONE
        }
    }
    
    private fun uploadRecording() {
        val file = outputFile?.let { File(it) }
        if (file != null && file.exists() && presignedUrlResponse != null) {
            binding.tvRecordingStatus.text = "Uploading..."
            binding.tvRecordingStatus.visibility = View.VISIBLE
            binding.progressBar.visibility = View.VISIBLE
            binding.btnRecord.isEnabled = false
            
            lifecycleScope.launch {
                val uploadUrl = presignedUrlResponse?.uploadURL ?: ""
                val success = FileUploader.uploadFile(file, uploadUrl)
                
                if (success) {
                    Log.d(TAG, "File uploaded successfully")
                    // Directly show template selection after upload
                    showTemplateSelectionDialog(
                        presignedUrlResponse?.fileName ?: "",
                        extractCallId(presignedUrlResponse?.fileName ?: "")
                    )
                } else {
                    Log.e(TAG, "Failed to upload file")
                    Toast.makeText(
                        requireContext(),
                        "Failed to upload recording",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                binding.tvRecordingStatus.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                binding.btnRecord.isEnabled = true
            }
        } else {
            Log.e(TAG, "File does not exist or presigned URL is null")
            Toast.makeText(
                requireContext(),
                "Error: Recording file not found",
                Toast.LENGTH_SHORT
            ).show()
            binding.progressBar.visibility = View.GONE
            binding.btnRecord.isEnabled = true
        }
    }
    
    private fun extractCallId(fileName: String): String {
        // Extract the callId from the fileName
        // Assuming format: packageId_userId_timestamp_callId.mp3
        return try {
            fileName.substringBeforeLast(".").split("_").last()
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting callId from fileName: $fileName", e)
            UUID.randomUUID().toString().take(8) // Fallback to a new random ID
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
    
    companion object {
        private const val TAG = "RecordFragment"
    }
} 