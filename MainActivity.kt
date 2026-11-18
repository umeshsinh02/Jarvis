package com.jarvis.assistant

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.jarvis.assistant.presentation.navigation.JarvisNavGraph
import com.jarvis.assistant.presentation.ui.theme.DarkNavy
import com.jarvis.assistant.presentation.ui.theme.JarvisTheme
import com.jarvis.assistant.presentation.viewmodel.JarvisViewModel
import com.jarvis.assistant.service.ScreenshotService
import com.jarvis.assistant.service.WakeWordService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val jarvisViewModel: JarvisViewModel by viewModels()

    private val screenshotLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = Intent(this, ScreenshotService::class.java).apply {
                putExtra(ScreenshotService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenshotService.EXTRA_RESULT_DATA, result.data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            jarvisViewModel.onScreenshotStarted()
        } else {
            jarvisViewModel.onScreenshotDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleWakeWordIntent(intent)

        setContent {
            JarvisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkNavy
                ) {
                    JarvisNavGraph(
                        jarvisViewModel = jarvisViewModel,
                        onRequestScreenshot = { launchMediaProjectionPrompt() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleWakeWordIntent(intent)
    }

    private fun handleWakeWordIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(WakeWordService.EXTRA_WAKE_TRIGGERED, false) == true) {
            jarvisViewModel.startListening()
        }
    }

    private fun launchMediaProjectionPrompt() {
        val projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenshotLauncher.launch(projectionManager.createScreenCaptureIntent())
    }
}
