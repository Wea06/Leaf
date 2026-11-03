package com.leaftodo.app.ui.sketch

import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

val LocalBoilFrame = staticCompositionLocalOf<State<Int>> { mutableIntStateOf(0) }
val LocalDrawingAnimations = staticCompositionLocalOf { true }
object BoilPause { var count by mutableIntStateOf(0) }

@Composable
fun PauseBoilWhileScrolling(state: ScrollableState) {
    val actions = LocalDrawingActions.current
    LaunchedEffect(state) {
        var held = false
        try {
            snapshotFlow { state.isScrollInProgress }.collect { scrolling ->
                if (scrolling != held) {
                    BoilPause.count += if (scrolling) 1 else -1
                    held = scrolling
                    if (scrolling) actions?.finish()
                }
            }
        } finally { if (held) BoilPause.count-- }
    }
}

@Composable
fun BoilClock(enabled: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val frame = remember { mutableIntStateOf(0) }
    var animations by remember { mutableStateOf(true) }
    LaunchedEffect(lifecycle, enabled) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            animations = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
            val powerSave = context.getSystemService(PowerManager::class.java).isPowerSaveMode
            try {
                snapshotFlow { enabled && animations && !powerSave && BoilPause.count == 0 }.collectLatest { running ->
                    frame.intValue = 0
                    if (running) while (true) { delay(125); frame.intValue = (frame.intValue + 1) % 3 }
                }
            } finally { frame.intValue = 0 }
        }
    }
    CompositionLocalProvider(LocalBoilFrame provides frame, LocalDrawingAnimations provides animations, content = content)
}
