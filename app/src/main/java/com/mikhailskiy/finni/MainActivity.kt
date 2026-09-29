package com.mikhailskiy.finni

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mikhailskiy.finni.data.StepCounterService
import com.mikhailskiy.finni.ui.FinniApp
import com.mikhailskiy.finni.ui.FinniViewModel
import com.mikhailskiy.finni.ui.theme.FinniTheme

class MainActivity : ComponentActivity() {
    private val viewModel: FinniViewModel by viewModels()
    private var stepSensorAvailable by mutableStateOf(false)
    private var stepPermissionGranted by mutableStateOf(false)

    private val activityRecognitionPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        stepPermissionGranted = granted
        if (granted) StepCounterService.start(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        stepSensorAvailable = StepCounterService.hasStepCounter(this)
        stepPermissionGranted = StepCounterService.hasActivityRecognitionPermission(this)
        if (stepSensorAvailable && stepPermissionGranted) StepCounterService.start(this)
        setContent {
            FinniTheme {
                FinniApp(
                    viewModel = viewModel,
                    stepSensorAvailable = stepSensorAvailable,
                    stepPermissionGranted = stepPermissionGranted,
                    onRequestStepPermission = ::requestStepTracking,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        stepPermissionGranted = StepCounterService.hasActivityRecognitionPermission(this)
        if (stepSensorAvailable && stepPermissionGranted) StepCounterService.start(this)
    }

    private fun requestStepTracking() {
        if (!stepSensorAvailable) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            stepPermissionGranted = true
            StepCounterService.start(this)
            return
        }
        if (StepCounterService.hasActivityRecognitionPermission(this)) {
            stepPermissionGranted = true
            StepCounterService.start(this)
        } else {
            activityRecognitionPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }
}
