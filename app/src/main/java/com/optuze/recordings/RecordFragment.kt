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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.optuze.recordings.databinding.FragmentRecordBinding
import com.optuze.recordings.ui.templates.TemplateSelectionDialog
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordFragment : Fragment() {
    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!
    
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var outputFile: String? = null
    
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
    
    private fun startRecording() {
        try {
            // Create output file
            val recordingsDir = File(
                requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "Recordings"
            )
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            outputFile = "${recordingsDir.absolutePath}/recording_$timestamp.mp3"
            
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
            
            Log.d(TAG, "Recording started: $outputFile")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start recording", e)
            Toast.makeText(
                requireContext(),
                "Failed to start recording: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
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
            
            // Show template selection dialog
            showTemplateSelectionDialog()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            Toast.makeText(
                requireContext(),
                "Failed to stop recording: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun updateUI() {
        if (isRecording) {
            binding.btnRecord.setImageResource(R.drawable.ic_stop)
            binding.tvRecordingStatus.text = "Recording..."
            binding.tvRecordingStatus.visibility = View.VISIBLE
        } else {
            binding.btnRecord.setImageResource(R.drawable.ic_record)
            binding.tvRecordingStatus.visibility = View.GONE
        }
    }
    
    private fun showTemplateSelectionDialog() {
        val dialog = TemplateSelectionDialog.newInstance(outputFile)
        dialog.show(childFragmentManager, "TemplateSelectionDialog")
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        if (isRecording) {
            stopRecording()
        }
        _binding = null
    }
    
    companion object {
        private const val TAG = "RecordFragment"
    }
} 