package com.vitron.remote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitron.remote.PairPhase
import com.vitron.remote.UiState

@Composable
fun SetupScreen(
    state: UiState,
    onPair: (String) -> Unit,
    onSubmitCode: (String) -> Unit,
    onCancel: () -> Unit
) {
    var manualIp by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Vitron Remote", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)

        when (state.phase) {
            PairPhase.Idle -> {
                Text(
                    "Make sure your phone and TV are on the same Wi-Fi, and the TV is switched on. Then pick your TV.",
                    color = Color(0xFFCCCCCC)
                )
                if (state.found.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Searching for TVs…", color = Color(0xFF999999))
                    }
                }
                state.found.forEach { tv ->
                    Button(onClick = { onPair(tv.host) }, modifier = Modifier.fillMaxWidth()) {
                        Text("${tv.name}  (${tv.host})")
                    }
                }
                HorizontalDivider(color = Color(0xFF333333))
                Text(
                    "Not listed? Enter the TV's IP address (TV Settings > Network > your Wi-Fi > IP address).",
                    color = Color(0xFFCCCCCC)
                )
                OutlinedTextField(
                    value = manualIp,
                    onValueChange = { manualIp = it.trim() },
                    label = { Text("e.g. 192.168.1.50") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onPair(manualIp) },
                    enabled = manualIp.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Pair with this IP") }
            }

            PairPhase.Connecting -> Busy("Contacting ${state.pairingHost}…")

            PairPhase.WaitingForCode -> {
                Text(
                    "A 6-character code is now showing on your TV. Type it here.",
                    color = Color(0xFFCCCCCC)
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { v -> code = v.filter { it.isLetterOrDigit() }.take(6).uppercase() },
                    label = { Text("Code from TV") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onSubmitCode(code) },
                    enabled = code.length == 6,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Pair") }
                TextButton(onClick = { code = ""; onCancel() }) { Text("Cancel") }
            }

            PairPhase.Verifying -> Busy("Checking the code…")
        }

        state.message?.let { Text(it, color = Color(0xFFFF8A80)) }
    }
}

@Composable
private fun Busy(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        Text(text, color = Color(0xFFCCCCCC))
    }
}
