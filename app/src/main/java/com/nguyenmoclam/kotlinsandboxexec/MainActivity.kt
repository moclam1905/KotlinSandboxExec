package com.nguyenmoclam.kotlinsandboxexec

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.nguyenmoclam.kotlinsandboxexec.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var  kotlinExecutor : KotlinCompilerExecutor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        kotlinExecutor = KotlinCompilerExecutor(this)

        setupNavigation()
    }

    override fun onDestroy() {
        super.onDestroy()
        kotlinExecutor.shutdown()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Setup bottom navigation with NavController
        binding.bottomNavigation.setupWithNavController(navController)

        // Add custom navigation listener for handling navigation events
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            try {
                navController.navigate(item.itemId)
                true
            } catch (e: IllegalArgumentException) {
                // Handle case where navigation fails
                android.util.Log.e("MainActivity", getString(R.string.system_error, e.message))
                false
            }
        }

        // Add destination change listener to update UI state
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.menu.forEach { menuItem ->
                if (menuItem.itemId == destination.id) {
                    menuItem.isChecked = true
                }
            }
        }
    }

    fun executeKotlinCode(code: String): String {
        return try {
            // Get execution settings from preferences
            val prefs = getSharedPreferences("kotlin_sandbox_settings", MODE_PRIVATE)
            val timeoutMs = prefs.getInt("execution_timeout", 10) * 1000L
            val memoryLimit = prefs.getInt("memory_limit", 50)

            // Execute the code with settings
            kotlinExecutor.execute(code, timeoutMs, memoryLimit)
        } catch (e: Exception) {
            getString(R.string.execution_error, e.message ?: "Unknown error")
        }
    }
}