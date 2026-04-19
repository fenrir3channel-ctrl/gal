package com.minimal.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.minimal.gallery.data.SettingsRepository
import com.minimal.gallery.data.repository.MediaRepository
import com.minimal.gallery.ui.screens.home.HomeScreen
import com.minimal.gallery.ui.theme.AppTheme
import com.minimal.gallery.ui.theme.MinimalGalleryTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var mediaRepository: MediaRepository
    
    private var currentTheme = AppTheme.SYSTEM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        settingsRepository = SettingsRepository(this)
        mediaRepository = MediaRepository(this)
        
        // Check and request permissions
        checkPermissions {
            initContent()
        }
    }
    
    private fun checkPermissions(onGranted: () -> Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        val allGranted = permissions.all {
            checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (allGranted) {
            onGranted()
        } else {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { _ ->
                onGranted()
            }
            requestPermissionLauncher.launch(permissions)
        }
    }
    
    private fun initContent() {
        lifecycleScope.launch {
            currentTheme = settingsRepository.themeFlow.first()
            
            setContent {
                MinimalGalleryTheme(appTheme = currentTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        HomeScreen(
                            mediaRepository = mediaRepository,
                            settingsRepository = settingsRepository,
                            onThemeChange = { newTheme ->
                                currentTheme = newTheme
                                lifecycleScope.launch {
                                    settingsRepository.setTheme(newTheme)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
