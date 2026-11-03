package com.leaftodo.app.ui.sketch

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Clip all pages inside the fixed frame without changing layout or consuming insets. */
@Composable
fun Modifier.clipToStaticFrame(): Modifier {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val insets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    // Frame centre is 12dp from the safe area; 2dp keeps content inside its ink stroke.
    val innerEdge = with(density) { 14.dp.toPx() }
    val left = insets.getLeft(density, direction) + innerEdge
    val top = insets.getTop(density) + innerEdge
    val right = insets.getRight(density, direction) + innerEdge
    val bottom = insets.getBottom(density) + innerEdge
    val shape = remember(left, top, right, bottom) {
        object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                val x = left.coerceAtMost(size.width)
                val y = top.coerceAtMost(size.height)
                return Outline.Rectangle(Rect(x, y, (size.width-right).coerceAtLeast(x), (size.height-bottom).coerceAtLeast(y)))
            }
        }
    }
    return clip(shape)
}
