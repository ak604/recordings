package com.optuze.recordings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.optuze.recordings.data.AppConfigManager
import com.optuze.recordings.data.SessionManager
import com.optuze.recordings.databinding.FragmentProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get user profile from AppConfigManager
        updateUserProfile()
        
        // Add a refresh button or pull-to-refresh if needed
        binding.btnRefresh?.setOnClickListener {
            refreshUserProfile()
        }
    }
    
    private fun updateUserProfile() {
        val userProfile = AppConfigManager.getUserProfile()
        
        if (userProfile != null) {
            binding.tvUserName.text = userProfile.name
            binding.tvUserEmail.text = userProfile.email
            binding.tvUserAccessLevel.text = userProfile.accessLevel
            
            // Format wallet display with gold coin icon
            binding.tvWalletGold.text = "${userProfile.wallet.gold} Gold"
            
            // Show last reward time if available
            userProfile.lastRewardTime?.let { lastRewardTime ->
                try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
                    val date = dateFormat.parse(lastRewardTime)
                    
                    val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    binding.tvLastReward.text = "Last Reward: ${displayFormat.format(date)}"
                    binding.tvLastReward.visibility = View.VISIBLE
                } catch (e: Exception) {
                    binding.tvLastReward.visibility = View.GONE
                }
            } ?: run {
                binding.tvLastReward.visibility = View.GONE
            }
            
            // Load profile picture with Glide
            Glide.with(this)
                .load(userProfile.picture)
                .circleCrop()
                .into(binding.ivUserProfile)
        }
    }
    
    private fun refreshUserProfile() {
        // Show loading indicator if available
        binding.progressBar?.visibility = View.VISIBLE
        
        // Create a new scope for this operation
        lifecycleScope.launch {
            try {
                // Get a new session manager instance
                val sessionManager = SessionManager(requireContext())
                
                // Reload app config data
                AppConfigManager.loadAppConfig(sessionManager)
                
                // Wait a moment for data to be loaded
                delay(1000)
                
                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    updateUserProfile()
                    Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to refresh: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBar?.visibility = View.GONE
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 