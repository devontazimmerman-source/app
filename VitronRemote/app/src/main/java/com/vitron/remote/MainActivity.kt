package com.vitron.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitron.remote.ui.RemoteScreen
import com.vitron.remote.ui.SetupScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val vm: RemoteViewModel = viewModel()
                val state by vm.ui.collectAsStateWithLifecycle()

                if (state.pairedHost != null) {
                    LaunchedEffect(state.pairedHost) { vm.connect() }
                    RemoteScreen(
                        state = state,
                        onKey = vm::press,
                        onLaunch = vm::launch,
                        onChangeTv = vm::forgetTv
                    )
                } else {
                    DisposableEffect(Unit) {
                        vm.startDiscovery()
                        onDispose { vm.stopDiscovery() }
                    }
                    SetupScreen(
                        state = state,
                        onPair = vm::startPairing,
                        onSubmitCode = vm::submitCode,
                        onCancel = vm::cancelPairing
                    )
                }
            }
        }
    }
}
