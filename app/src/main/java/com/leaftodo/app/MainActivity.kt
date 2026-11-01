package com.leaftodo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.leaftodo.app.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        window.isNavigationBarContrastEnforced = false
        setContent { LeafTheme { Surface(color = bg, contentColor = ink) { LeafApp() } } }
    }
}

@Composable
private fun LeafApp() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val pages = listOf("今天", "清单", "日历")
    Column(
        Modifier.fillMaxSize().background(bg).windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 34.dp, vertical = 32.dp)) {
                Text(pages[selected], style = MaterialTheme.typography.headlineLarge)
                if (selected == 0) {
                    Spacer(Modifier.height(20.dp))
                    Text("11月1日 周日 · Leaf 测试 0123456789", style = MaterialTheme.typography.bodyLarge)
                }
            }
            FloatingActionButton(
                onClick = {},
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 42.dp, bottom = 42.dp)
                    .size(56.dp).semantics { contentDescription = "新建一条事" },
                shape = CircleShape,
                containerColor = clay, contentColor = ink,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            ) {
                Text("+", style = MaterialTheme.typography.headlineLarge)
            }
        }
        Row(
            Modifier.fillMaxWidth().selectableGroup().padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            pages.forEachIndexed { index, label ->
                Box(
                    Modifier.weight(1f).heightIn(min = 48.dp)
                        .background(if (selected == index) clayTint else Color.Transparent, RoundedCornerShape(16.dp))
                        .selectable(selected == index, role = Role.Tab, onClick = { selected = index })
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = if (selected == index) ink else soft,
                        style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

