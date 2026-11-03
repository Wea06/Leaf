package com.leaftodo.app.ui.sketch

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.layout.layout
import com.leaftodo.app.data.*
import com.leaftodo.app.ui.theme.*
import kotlin.math.*

private class PaintPath(p: Path, c: Color, sw: Float = 0f) { val path = p; val color = c; val width = sw }

/** Coordinates are dp; all randomness is calculated once in drawWithCache. */
class SketchPen internal constructor(frameIndex: Int, widthDp: Float, heightDp: Float) {
    private val frame = frameIndex
    val w = widthDp
    val h = heightDp
    private val paths = mutableListOf<PaintPath>()
    private fun trace(p: List<RoughPoint>, move: Boolean = true, path: Path) {
        if (move) path.moveTo(p[0].x, p[0].y) else path.lineTo(p[0].x, p[0].y)
        for (i in 1 until p.lastIndex) path.quadraticTo(p[i].x, p[i].y,
            (p[i].x + p[i+1].x)/2, (p[i].y + p[i+1].y)/2)
        path.lineTo(p.last().x, p.last().y)
    }
    private fun ink(id: Int, color: Color, width: Float, build: (Int) -> Path) {
        val seed = id * 977 + frame * 131
        paths += PaintPath(build(seed), color, width)
        val r = rng(seed + 9)
        val alt = build(seed + 55)
        alt.translate(androidx.compose.ui.geometry.Offset(((r()-.5)*1.2).toFloat(), ((r()-.5)*1.2).toFloat()))
        paths += PaintPath(alt, color, width * .75f)
    }
    fun line(x: Float, y: Float, x2: Float, y2: Float, id: Int, color: Color = ink,
        width: Float = 3.4f, amp: Float = .8f, over: Boolean = true, progress: Float = 1f) {
        if (progress <= 0) return
        ink(id, color, width) { seed -> Path().also { p ->
            val points = roughPoints(x,y,x2,y2,seed,amp,12f,over)
            trace(points.take(max(2, ceil(points.size * progress.coerceAtMost(1f)).toInt())), path = p)
        } }
    }
    /** Multiple strokes share one ink seed; subsequent strokes use s+1, as in the demos. */
    fun strokes(id: Int, segments: List<FloatArray>, color: Color = ink, width: Float = 3.4f,
        amp: Float = .4f, over: Boolean = true, progress: List<Float> = List(segments.size) { 1f }) {
        ink(id,color,width) { seed -> Path().also { path ->
            segments.forEachIndexed { i, e ->
                val fraction=progress[i]
                if(fraction>0f) {
                    val points=roughPoints(e[0],e[1],e[2],e[3],seed+i,amp,12f,over)
                    trace(points.take(max(2,ceil(points.size*fraction.coerceAtMost(1f)).toInt())),path=path)
                }
            }
        } }
    }
    fun rect(x: Float, y: Float, width: Float, height: Float, id: Int,
        color: Color = ink, stroke: Float = 2.38f, amp: Float = .8f, fill: Boolean = false) {
        fun build(seed: Int) = Path().also { p ->
            listOf(floatArrayOf(x,y,x+width,y), floatArrayOf(x+width,y,x+width,y+height),
                floatArrayOf(x+width,y+height,x,y+height), floatArrayOf(x,y+height,x,y)).forEachIndexed { i, e ->
                trace(roughPoints(e[0],e[1],e[2],e[3],seed+i,amp,12f,!fill), i == 0 || !fill, p)
            }
            if (fill) p.close()
        }
        if (fill) paths += PaintPath(build(id*733+frame*97), color) else ink(id,color,stroke,::build)
    }
    fun circle(cx: Float, cy: Float, radius: Float, id: Int, color: Color,
        width: Float = 0f, amp: Float = .8f) {
        fun build(seed: Int) = Path().also { p ->
            roughCirclePoints(cx,cy,radius,seed,amp,12f,width==0f).forEachIndexed { i, point ->
                if (i==0) p.moveTo(point.x,point.y) else p.lineTo(point.x,point.y)
            }
            if (width==0f) p.close()
        }
        if (width==0f) paths += PaintPath(build(id*733+frame*97),color) else ink(id,color,width,::build)
    }
    fun capsule(id: Int, color: Color) {
        fun build(seed: Int) = Path().also { path ->
            val r = rng(seed)
            val points = (0..24).map { i ->
                val angle = i * Math.PI * 2 / 24
                val radius = 12 + (r()*2-1)*.3
                RoughPoint(((if(cos(angle)>=0) 28 else 12)+cos(angle)*radius).toFloat(),(12+sin(angle)*radius).toFloat())
            }
            trace(points,path=path); path.close()
        }
        paths += PaintPath(build(id*733+frame*97),color)
        ink(id+1,com.leaftodo.app.ui.theme.ink,1.7f,::build)
    }
    internal fun paint(scope: DrawScope) = with(scope) {
        paths.forEach { drawPath(it.path,it.color,style = if (it.width==0f) Fill else Stroke(it.width,cap=StrokeCap.Round,join=StrokeJoin.Round)) }
    }
}

@Composable
fun Modifier.sketchDrawing(seed: Int = 1, static: Boolean = false, progress: State<Float>? = null,
    build: SketchPen.(Float, Int) -> Unit): Modifier {
    val frame = if (static) null else LocalBoilFrame.current
    return drawWithCache {
        val d = density
        val phases = if (progress == null) listOf(1f) else listOf(0f, 1000f/12/330, 2000f/12/330, 3000f/12/330, 1f)
        val cache = List(if (static) 1 else 3) { f -> phases.map { p -> SketchPen(f,size.width/d,size.height/d).apply { build(p,seed) } } }
        onDrawBehind {
            val phase = if (progress == null) 0 else when { progress.value >= 1f -> 4; progress.value >= .75f -> 3; progress.value >= .5f -> 2; progress.value > 0f -> 1; else -> 0 }
            scale(d,d, pivot=androidx.compose.ui.geometry.Offset.Zero) { cache[frame?.value ?: 0][phase].paint(this) }
        }
    }
}

@Composable
fun Modifier.sketchFill(color: Color, seed: Int = 1): Modifier = sketchDrawing(seed) { _, id -> rect(0f,0f,w,h,id,color,amp=.64f,fill=true) }

@Composable
fun Modifier.sketchCard(fill: Color = card, seed: Int = 1): Modifier = sketchDrawing(seed) { _, id ->
    rect(0f,0f,w,h,id,fill,amp=.64f,fill=true)
    rect(3f,3f,w,h,id+20)
}

@Composable
fun Modifier.sketchClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    return sketchCard(if(pressed) clayTint else card).clickable(source,indication=null,enabled=enabled,onClick=onClick)
}

@Composable
fun SketchCard(modifier: Modifier = Modifier, fill: Color = card, seed: Int = 1, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.sketchCard(fill,seed), content = content)
}

@Composable
fun StaticFrame(modifier: Modifier = Modifier) {
    Box(modifier.sketchDrawing(99,static=true) { _, id -> rect(0f,0f,w,h,id,stroke=2.72f,amp=.4f) })
}

@Composable
fun SketchCheckbox(status: TaskStatus, timed: Boolean, seed: Int, progress: State<Float>, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clickable(interactionSource=remember { MutableInteractionSource() }, indication=null,
        role=Role.Checkbox, onClickLabel="完成或撤回", onClick=onClick)
        .semantics { toggleableState = ToggleableState(status==TaskStatus.DONE) }, contentAlignment=Alignment.Center) {
        Box(Modifier.size(20.dp).sketchDrawing(seed,progress=progress) { p,id ->
            if (status==TaskStatus.DONE) {
                val z=24*popScale(p); val c=(24-z)/2
                rect(c,c,z,z,id+40,clay,amp=.48f,fill=true)
            }
            rect(0f,0f,20f,20f,id+60,if(timed && status==TaskStatus.OPEN) clay else ink,2.89f,.48f)
            if (status==TaskStatus.DONE) {
                strokes(id+80,listOf(floatArrayOf(3f,10f,9f,17f),floatArrayOf(9f,17f,22f,-2f)),
                    progress=listOf(p*3,(p-.34f)*1.5f))
            }
            if (status==TaskStatus.DROPPED) line(4f,10f,16f,10f,id+80,width=3.06f,amp=.4f,progress=p*1.6f)
        })
    }
}

@Composable
fun SketchFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.clickable(interactionSource=remember { MutableInteractionSource() },indication=null,role=Role.Button,onClick=onClick)
        .sketchDrawing(70) { _,id ->
            val x=w/2; val y=h/2
            circle(x-1.5f,y-1.5f,26f,id,clay)
            circle(x+1.5f,y+1.5f,26f,id,ink,3.4f)
            strokes(id+1,listOf(floatArrayOf(x-8.5f,y+1.5f,x+11.5f,y+1.5f),
                floatArrayOf(x+1.5f,y-8.5f,x+1.5f,y+11.5f)),amp=.48f)
        })
}

@Composable
fun SketchSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)? = null) {
    val touch = if (onCheckedChange == null) Modifier else Modifier.toggleable(checked,role=Role.Switch,onValueChange=onCheckedChange)
    Box(touch.size(48.dp,48.dp).semantics { role=Role.Switch; toggleableState=ToggleableState(checked) },contentAlignment=Alignment.Center) {
        Box(Modifier.size(40.dp,24.dp).sketchDrawing { _,id ->
            // Capsule: two circles joined by a flat centre, all cached as one frame.
            val color=if(checked) clay else rule
            capsule(id,color)
            // Use a single capsule path for the outline, without interior seams.
            val x=if(checked) 28f else 12f
            circle(x,12f,9f,id+5,card,amp=.3f); circle(x,12f,9f,id+6,ink,1.7f,.3f)
        })
    }
}

@Composable
fun SketchDivider(modifier: Modifier = Modifier) { Box(modifier.fillMaxWidth().height(2.dp).sketchDrawing { _,id -> line(0f,1f,w,1f,id,soft,1.5f) }) }

@Composable
fun SketchStrike(text: String, color: Color, struck: Boolean, seed: Int, progress: State<Float>) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val modifier = if (!struck || layout == null) Modifier else Modifier.sketchDrawing(seed,progress=progress) { p,id ->
        val l=layout!!
        line(l.getLineLeft(0)/density-2, (l.getLineTop(0)+l.getLineBottom(0))/2/density-1,
            l.getLineRight(0)/density+3, (l.getLineTop(0)+l.getLineBottom(0))/2/density-1,id,soft,1.5f,.64f,progress=p)
    }
    Text(text,color=color,style=MaterialTheme.typography.bodyLarge,modifier=modifier,onTextLayout={layout=it})
}

@Composable
fun SketchButton(text: String, onClick: () -> Unit, primary: Boolean = true) {
    Box(Modifier.heightIn(min=48.dp).then(if(primary) Modifier.sketchFill(clay) else Modifier.sketchDrawing { _,id -> rect(0f,0f,w,h,id) })
        .clickable(onClick=onClick).padding(16.dp),contentAlignment=Alignment.Center) { Text(text,color=ink) }
}

/** Material pickers have a fixed preferred width; fit the whole control without clipping columns. */
fun Modifier.fitSketchPicker(preferredWidth: Dp = 360.dp): Modifier = layout { measurable, constraints ->
    val width = constraints.maxWidth
    val natural = max(width, preferredWidth.roundToPx())
    val scale = width.toFloat() / natural
    val child = measurable.measure(androidx.compose.ui.unit.Constraints(
        minWidth=natural,maxWidth=natural,
        maxHeight=if(constraints.maxHeight==androidx.compose.ui.unit.Constraints.Infinity) constraints.maxHeight else (constraints.maxHeight/scale).toInt()))
    layout(width,(child.height*scale).roundToInt()) {
        child.placeWithLayer(0,0) { scaleX=scale; scaleY=scale; transformOrigin=TransformOrigin(0f,0f) }
    }
}

/** Shared dialog shell; keep the existing picker and confirmation callbacks. */
@Composable
fun LeafDialog(onDismissRequest: () -> Unit, confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null, title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null, containerColor: Color = card,
    tonalElevation: Dp = 0.dp, titleContentColor: Color = ink, textContentColor: Color = ink) {
    Dialog(onDismissRequest=onDismissRequest, properties=DialogProperties(usePlatformDefaultWidth=false)) {
        SketchCard(Modifier.padding(horizontal=16.dp).widthIn(max=560.dp).fillMaxWidth()) {
            Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                if(title!=null) ProvideTextStyle(MaterialTheme.typography.headlineSmall.copy(color=titleContentColor)) { title() }
                if(text!=null) ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(color=textContentColor)) { text() }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End,verticalAlignment=Alignment.CenterVertically) {
                    dismissButton?.invoke(); confirmButton()
                }
            }
        }
    }
}
