package com.nguyenmoclam.kotlinsandboxexec

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.nguyenmoclam.kotlinsandboxexec.databinding.ActivityLogBinding

class LogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLogBinding
    private var currentCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_execution_log)

        // Get code from intent or saved state
        currentCode = savedInstanceState?.getString("current_code") ?: intent.getStringExtra(
            EXTRA_CODE
        ) ?: ""
        val logOutput = savedInstanceState?.getString("log_output") ?: intent.getStringExtra(
            EXTRA_LOG
        ) ?: ""
        binding.logOutput.text = logOutput

        setupButtons()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("current_code", currentCode)
        outState.putString("log_output", binding.logOutput.text.toString())
    }

    private fun setupButtons() {
        // Clear log button
        binding.clearButton.setOnClickListener {
            binding.logOutput.text = ""
        }

        // Rerun button
        binding.rerunButton.setOnClickListener {
            rerunCode()
        }
    }

    private fun rerunCode() {
        if (currentCode.isEmpty()) {
            binding.logOutput.text = getString(R.string.no_code_to_run)
            return
        }

        binding.rerunButton.isEnabled = false
        binding.clearButton.isEnabled = false
        binding.logOutput.text = getString(R.string.running)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val mainActivity = getMainActivity()
                if (mainActivity == null) {
                    withContext(Dispatchers.Main) {
                        binding.logOutput.text = getString(R.string.restart_required)
                        binding.rerunButton.isEnabled = true
                        binding.clearButton.isEnabled = true
                        return@withContext
                    }
                    return@launch
                }
                
                try {
                    val result = mainActivity.executeKotlinCode(currentCode)
                    withContext(Dispatchers.Main) {
                        if (result.isBlank()) {
                            binding.logOutput.text = getString(R.string.execution_completed)
                        } else {
                            binding.logOutput.text = result
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        binding.logOutput.text = getString(R.string.execution_error, e.message ?: "An unexpected error occurred during code execution")
                    }
                }
            } catch (e: SecurityException) {
                withContext(Dispatchers.Main) {
                    binding.logOutput.text = getString(R.string.security_error)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.logOutput.text = getString(R.string.system_error, e.message ?: "An unexpected system error occurred")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.rerunButton.isEnabled = true
                    binding.clearButton.isEnabled = true
                }
            }
        }
    }

    private fun getMainActivity(): MainActivity? {
        // Get the parent activity that launched this activity
        val parentActivity = parent as? MainActivity
        if (parentActivity != null) {
            return parentActivity
        }

        // If parent activity is not available, try to find MainActivity in the task stack
        return try {
            val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            val runningTasks = activityManager.getRunningTasks(1)
            if (runningTasks.isNotEmpty()) {
                val topActivity = runningTasks[0].topActivity
                if (topActivity?.className == MainActivity::class.java.name) {
                    // Instead of using 'activity', we should use the application context to get the MainActivity
                    val currentActivity = applicationContext as? MainActivity
                    currentActivity
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val EXTRA_CODE = "extra_code"
        const val EXTRA_LOG = "extra_log"
    }
}