package com.optuze.recordings

import android.os.Bundle
import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.optuze.recordings.databinding.ActivityMainBinding
import com.optuze.recordings.ui.templates.ProcessedTemplateFragment
import com.optuze.recordings.ui.templates.TemplateSelectionListener
import com.optuze.recordings.data.AppConfigManager
import com.optuze.recordings.data.SessionManager
import com.optuze.recordings.data.models.UserReward
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import androidx.lifecycle.lifecycleScope
import android.util.Log

class MainActivity : AppCompatActivity(), TemplateSelectionListener {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize session manager
        val sessionManager = SessionManager(this)
        
        // Load app config data
        AppConfigManager.loadAppConfig(sessionManager)

        // Set default fragment
        if (savedInstanceState == null) {
            loadFragment(RecordFragment())
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.navigation_record -> RecordFragment()
                R.id.navigation_recordings -> RecordingsFragment()
                R.id.navigation_templates -> TemplatesFragment()
                R.id.navigation_profile -> ProfileFragment()
                else -> RecordFragment()
            }
            loadFragment(fragment)
            true
        }

        // Setup reward observer
        setupRewardObserver()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    
    // Implement the callback method
    override fun onTemplateProcessed(templateName: String, templateContent: String) {
        val fragment = ProcessedTemplateFragment.newInstance(templateName, templateContent)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("main_processed_template")
            .commit()
    }

    private fun setupRewardObserver() {
        lifecycleScope.launch {
            AppConfigManager.rewardsFlow.collect { rewards ->
                if (!rewards.isNullOrEmpty()) {
                    showRewardDialog(rewards)
                    // Clear the rewards after showing to prevent duplicates
                    AppConfigManager.clearShownRewards()
                }
            }
        }
    }

    private fun showRewardDialog(rewards: List<UserReward>) {
        // Calculate total reward
        val totalGold = rewards.sumOf { it.tokens.gold }
        
        if (totalGold > 0) {
            Log.d("Rewards", "Showing reward dialog for $totalGold gold")
            
            // Create and show reward dialog
            val dialog = AlertDialog.Builder(this)
                .setTitle("Daily Rewards!")
                .setMessage("You've received $totalGold gold as daily rewards!")
                .setIcon(R.drawable.ic_gold_coin)
                .setPositiveButton("Awesome!") { dialog, _ -> dialog.dismiss() }
                .create()
            
            dialog.show()
        }
    }
} 