package com.leaftodo.app.ui.sketch

import androidx.compose.runtime.*

/** Optional local observer for on-device rendering tests; no work in normal use. */
val LocalCompositionProbe = staticCompositionLocalOf<((String) -> Unit)?> { null }

@Composable
fun CompositionProbe(name: String) {
    val probe = LocalCompositionProbe.current
    if (probe != null) SideEffect { probe(name) }
}
