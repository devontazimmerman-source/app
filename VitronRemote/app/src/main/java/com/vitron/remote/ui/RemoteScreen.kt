package com.vitron.remote.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitron.remote.Conn
import com.vitron.remote.UiState
import com.vitron.remote.net.Keys
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val KeyGrey = Color(0xFF2B2B2B)
private val KeyBorder = Color(0xFF404040)
private val PowerRed = Color(0xFFC8203A)
private val YouTubeRed = Color(0xFFD6204A)
private val Cream = Color(0xFFEDE6D8)
private val ColorRed = Color(0xFFE53935)
private val ColorGreen = Color(0xFF2E9E5B)
private val ColorYellow = Color(0xFFF2D21B)
private val ColorBlue = Color(0xFF2F82C9)

@Composable
fun RemoteScreen(
    state: UiState,
    onKey: (Int) -> Unit,
    onLaunch: (String) -> Unit,
    onChangeTv: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Connection status
        Row(
            modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val (label, color) = when (state.connection) {
                Conn.Ready -> "Connected" to Color(0xFF4CAF50)
                Conn.Connecting -> "Connecting…" to Color(0xFFFFC107)
                Conn.Disconnected -> "Not connected (tap any key to retry)" to Color(0xFFFF7043)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                Text("  $label", color = Color(0xFFCCCCCC), fontSize = 12.sp)
            }
            TextButton(onClick = onChangeTv) { Text("Change TV", fontSize = 12.sp) }
        }
        state.message?.let {
            Text(it, color = Color(0xFFFF8A80), fontSize = 12.sp, modifier = Modifier.widthIn(max = 360.dp))
        }

        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val k = { code: Int -> { onKey(code) } }

            // Power + mute
            KeyRow {
                RKey(Modifier.weight(1f), icon = Icons.Filled.PowerSettingsNew, container = PowerRed, action = k(Keys.POWER))
                RKey(Modifier.weight(1f), icon = Icons.Filled.VolumeOff, action = k(Keys.MUTE))
            }
            // Picture / sound modes (TV-specific, not available over Wi-Fi)
            KeyRow {
                RKey(Modifier.weight(1f), label = "MODE", supported = false)
                RKey(Modifier.weight(1f), label = "S.MODE", supported = false)
                RKey(Modifier.weight(1f), label = "ASPECT", supported = false)
            }
            KeyRow {
                RKey(Modifier.weight(1f), label = "SUB.T\nCC", action = k(Keys.CAPTIONS))
                RKey(Modifier.weight(1f), label = "TV", action = k(Keys.TV))
                RKey(Modifier.weight(1f), label = "AUDIO\nMTS", supported = false)
            }
            KeyRow {
                RKey(Modifier.weight(1f), label = "T.SHIFT", supported = false)
                RKey(Modifier.weight(1f), label = "EPG", action = k(Keys.GUIDE))
                RKey(Modifier.weight(1f), label = "REC", supported = false)
            }

            Spacer8()

            // Number pad
            for (row in 0..2) {
                KeyRow {
                    for (col in 1..3) {
                        val n = row * 3 + col
                        RKey(Modifier.weight(1f), label = "$n", big = true, action = k(Keys.digit(n)))
                    }
                }
            }
            KeyRow {
                RKey(Modifier.weight(1f), label = "FAV", supported = false)
                RKey(Modifier.weight(1f), label = "0", big = true, action = k(Keys.digit(0)))
                RKey(Modifier.weight(1f), icon = Icons.Filled.SwapHoriz, action = k(Keys.LAST_CHANNEL))
            }

            Spacer8()

            // Navigation pad
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF161616))
            ) {
                CornerKey("SOURCE", Modifier.align(Alignment.TopStart), k(Keys.INPUT))
                CornerKey("INFO", Modifier.align(Alignment.TopEnd), k(Keys.INFO))
                CornerKey("MENU", Modifier.align(Alignment.BottomStart), k(Keys.MENU))
                CornerKey("EXIT", Modifier.align(Alignment.BottomEnd), k(Keys.BACK))

                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(196.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF242424))
                        .border(2.dp, Color(0xFF3A3A3A), CircleShape)
                ) {
                    DpadArrow(Icons.Filled.KeyboardArrowUp, Modifier.align(Alignment.TopCenter), k(Keys.UP))
                    DpadArrow(Icons.Filled.KeyboardArrowDown, Modifier.align(Alignment.BottomCenter), k(Keys.DOWN))
                    DpadArrow(Icons.Filled.KeyboardArrowLeft, Modifier.align(Alignment.CenterStart), k(Keys.LEFT))
                    DpadArrow(Icons.Filled.KeyboardArrowRight, Modifier.align(Alignment.CenterEnd), k(Keys.RIGHT))
                    PressBox(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF101010))
                            .border(2.dp, Color(0xFF3A3A3A), CircleShape),
                        action = k(Keys.OK)
                    ) {
                        Text("OK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }

            Spacer8()

            // CH / HOME / VOL
            Row(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    RKey(Modifier.fillMaxWidth().weight(1f), icon = Icons.Filled.KeyboardArrowUp, label = "CH", repeat = true, action = k(Keys.CH_UP))
                    Box(Modifier.height(8.dp))
                    RKey(Modifier.fillMaxWidth().weight(1f), icon = Icons.Filled.KeyboardArrowDown, label = "CH", repeat = true, action = k(Keys.CH_DOWN))
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    RKey(Modifier.fillMaxWidth().weight(1f), icon = Icons.Filled.Home, label = "HOME", action = k(Keys.HOME))
                    Box(Modifier.height(8.dp))
                    RKey(Modifier.fillMaxWidth().height(44.dp), label = "🖱", supported = false)
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    RKey(Modifier.fillMaxWidth().weight(1f), icon = Icons.Filled.Add, label = "VOL", repeat = true, action = k(Keys.VOL_UP))
                    Box(Modifier.height(8.dp))
                    RKey(Modifier.fillMaxWidth().weight(1f), icon = Icons.Filled.Remove, label = "VOL", repeat = true, action = k(Keys.VOL_DOWN))
                }
            }

            // App shortcuts
            KeyRow {
                RKey(Modifier.weight(1f), label = "NFX", container = Cream, content = PowerRed, big = true,
                    action = { onLaunch("https://www.netflix.com") })
                RKey(Modifier.weight(1f), label = "You▶", container = YouTubeRed, big = true,
                    action = { onLaunch("https://www.youtube.com") })
                RKey(Modifier.weight(1f), label = "Facebook", container = Cream, content = PowerRed,
                    action = { onLaunch("https://www.facebook.com") })
            }

            // Media / teletext block (icons follow the printed symbols on the original remote)
            KeyRow {
                RKey(Modifier.weight(1f), label = "MEDIA", supported = false)
                RKey(Modifier.weight(1f), label = "SETTING", action = k(Keys.SETTINGS))
                RKey(Modifier.weight(1f), label = "SCREENSHOT", supported = false)
            }
            KeyRow {
                RKey(Modifier.weight(1f), icon = Icons.Filled.SkipPrevious, label = "INDEX", action = k(Keys.PREV))
                RKey(Modifier.weight(1f), label = "▶❚❚\nTEXT", action = k(Keys.PLAY_PAUSE))
                RKey(Modifier.weight(1f), icon = Icons.Filled.SkipNext, label = "REVEAL", action = k(Keys.NEXT))
            }
            KeyRow {
                RKey(Modifier.weight(1f), icon = Icons.Filled.FastRewind, label = "HOLD", action = k(Keys.REWIND))
                RKey(Modifier.weight(1f), icon = Icons.Filled.Stop, label = "SIZE", action = k(Keys.STOP))
                RKey(Modifier.weight(1f), icon = Icons.Filled.FastForward, label = "SUBPAGE", action = k(Keys.FAST_FORWARD))
            }
            KeyRow {
                RKey(Modifier.weight(1f), label = "FREEZE", supported = false)
                RKey(Modifier.weight(1f), label = "REPEAT", supported = false)
                RKey(Modifier.weight(1f), label = "SLEEP", supported = false)
            }

            // Colour keys
            KeyRow {
                RKey(Modifier.weight(1f), container = ColorRed, action = k(Keys.RED))
                RKey(Modifier.weight(1f), container = ColorGreen, action = k(Keys.GREEN))
                RKey(Modifier.weight(1f), container = ColorYellow, action = k(Keys.YELLOW))
                RKey(Modifier.weight(1f), container = ColorBlue, action = k(Keys.BLUE))
            }

            Text(
                "VITRON",
                color = Color.White,
                fontSize = 22.sp,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp)
            )
        }
        Box(Modifier.height(16.dp))
    }
}

@Composable
private fun Spacer8() = Box(Modifier.height(6.dp))

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}

/** A tap target that sends once, or repeats while held. */
@Composable
private fun PressBox(
    modifier: Modifier = Modifier,
    supported: Boolean = true,
    repeat: Boolean = false,
    action: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pressed by remember { mutableStateOf(false) }
    val currentAction by rememberUpdatedState(action)

    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (pressed) 0.55f else 1f }
            .pointerInput(supported, repeat) {
                detectTapGestures(onPress = {
                    if (!supported) {
                        Toast.makeText(context, "Not available over Wi-Fi", Toast.LENGTH_SHORT).show()
                    } else {
                        pressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentAction()
                        if (repeat) {
                            val job = scope.launch {
                                delay(450)
                                while (true) {
                                    currentAction()
                                    delay(110)
                                }
                            }
                            tryAwaitRelease()
                            job.cancel()
                        } else {
                            tryAwaitRelease()
                        }
                        pressed = false
                    }
                })
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun RKey(
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: ImageVector? = null,
    container: Color = KeyGrey,
    content: Color = Color.White,
    supported: Boolean = true,
    repeat: Boolean = false,
    big: Boolean = false,
    action: () -> Unit = {}
) {
    val shape = RoundedCornerShape(9.dp)
    val bg = if (supported) container else container.copy(alpha = 0.45f)
    val fg = if (supported) content else content.copy(alpha = 0.45f)
    PressBox(
        modifier = modifier
            .heightIn(min = 46.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, KeyBorder, shape),
        supported = supported,
        repeat = repeat,
        action = action
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (icon != null) Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(22.dp))
            if (label != null) {
                Text(
                    label,
                    color = fg,
                    fontSize = if (big) 20.sp else 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = if (big) 22.sp else 11.sp
                )
            }
        }
    }
}

@Composable
private fun DpadArrow(icon: ImageVector, modifier: Modifier, action: () -> Unit) {
    PressBox(modifier = modifier.size(62.dp).clip(CircleShape), repeat = true, action = action) {
        Icon(icon, contentDescription = null, tint = Color(0xFFDDDDDD), modifier = Modifier.size(34.dp))
    }
}

@Composable
private fun CornerKey(label: String, modifier: Modifier, action: () -> Unit) {
    PressBox(
        modifier = modifier
            .padding(6.dp)
            .heightIn(min = 44.dp)
            .widthIn(min = 64.dp)
            .clip(RoundedCornerShape(10.dp)),
        action = action
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
