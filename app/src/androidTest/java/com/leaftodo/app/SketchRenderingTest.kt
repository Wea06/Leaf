package com.leaftodo.app

import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario

import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.leaftodo.app.data.*
import com.leaftodo.app.ui.sketch.*
import com.leaftodo.app.ui.theme.*
import org.junit.*
import org.junit.Assert.*
import java.time.*
import java.util.concurrent.ConcurrentHashMap

/** Isolated UI fixture: never opens or modifies the user's database. */
class SketchRenderingTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText() }

    @Test fun stationaryBoilDoesNotRecomposeAndThirtyCardsScroll() {
        val counts = ConcurrentHashMap<String, Int>()
        val today = LocalDate.of(2026,11,3)
        val rows = (1L..30L).map { newListTask("测试待办 $it",Instant.EPOCH).copy(id=it) }
        var frame: State<Int>? = null
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
        scenario.onActivity { activity -> activity.setContent {
            LeafTheme {
                CompositionLocalProvider(LocalCompositionProbe provides { key -> counts.merge(key,1,Int::plus); Unit }) {
                    BoilClock(true) {
                        frame=LocalBoilFrame.current
                        DrawingPage {
                            Column(Modifier.fillMaxSize().padding(34.dp)) {
                                Row { (0..6).forEach { offset ->
                                    CalendarDay(today.plusDays(offset.toLong()),today,if(offset==0) DayMark.TODAY else DayMark.UPCOMING,
                                        offset==0,Modifier.weight(1f)) {}
                                } }
                                val state=rememberLazyListState()
                                PauseBoilWhileScrolling(state)
                                LazyColumn(Modifier.testTag("cards"),state=state,verticalArrangement=Arrangement.spacedBy(20.dp)) {
                                    items(rows,key={it.id}) { task -> TaskCard(task,today,true,{},{},{},{}) }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
        instrumentation.waitForIdleSync()
        Thread.sleep(500)
        val before=counts.toMap()
        val frames=mutableSetOf<Int>()
        repeat(12) { Thread.sleep(125); instrumentation.runOnMainSync { frames += frame!!.value } }
        assertTrue("Clock must actually boil during the check",frames.size > 1)
        assertEquals(before,counts.toMap())
        Log.i("LeafSketchQA","stationary 1500ms: TaskCard and CalendarDay recomposition delta=0, frames=$frames")
        val pkg=InstrumentationRegistry.getInstrumentation().targetContext.packageName
        shell("dumpsys gfxinfo $pkg reset")
        val metrics=instrumentation.targetContext.resources.displayMetrics
        val x=metrics.widthPixels/2; val top=metrics.heightPixels/3; val bottom=metrics.heightPixels*3/4
        repeat(8) { shell("input swipe $x $bottom $x $top 180"); Thread.sleep(150) }
        repeat(8) { shell("input swipe $x $top $x $bottom 180"); Thread.sleep(150) }
        Thread.sleep(500)
        Log.i("LeafSketchQA",shell("dumpsys gfxinfo $pkg"))
        }
    }
}
