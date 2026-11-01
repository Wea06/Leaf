# STYLE.md：视觉风格说明（v0.5）

> 状态：抖动参数由用户在演示里调定；配色和画法按参考图校准；动效按主参考视频（Claude Founder House）逐帧分析；v0.5 配色与参数由用户定稿（2026-11-01）。
> 演示页：https://claude.ai/artifact/GtrK5kiX2qB9rrfPZiqzqq
> 给写代码的 agent 看：动手做任何 UI 之前先读这份文件。数值是起点，真机验收后再调，改了就更新这里。

## 1. 一句话风格

**暖色纸面上的手绘墨线 + 平涂色块，线条会“呼吸”。**
参考 Anthropic 发布视频和插画：黑色马克笔/墨水感的粗线条，形状不规整；平涂的纯色块没有描边；线条和色块常常故意错开一点（像套印没对齐）；低帧率“抖动线”（line boil），静止元素也在原地微微扭动。

原则：
1. **两种笔触各司其职**：墨线负责“画东西”（图标、勾号、手、物体轮廓）；平涂色块负责“垫底”（卡片、高亮、按钮底）。卡片 = 色块 + 墨线描边，二者错位 3dp（用户选定）。
2. **不用阴影、渐变、毛玻璃、排线**。画面是平的。
3. **克制**：正文文字不抖；强调色（橙）一屏 1–2 处。
4. **安静**：抖动幅度小、帧率低，看久了不晕。

## 2. 配色（用户定稿 2026-11-01）

只有四种颜色：**暖棕米色底、橙、黑、米白**。其余都是它们的深浅变体。先只做浅色模式（主要在手机上用）。

| 名称 | 色值 | 用途 |
|---|---|---|
| bg 底色 | **`#E3D9CD`**（用户指定） | 页面背景，暖棕米色，像素描纸 |
| card 米白 | `#FAF6F0` | 卡片、输入区、弹窗的平涂色块 |
| ink 墨黑 | `#1C1A17` | 线条、图标、标题、正文（带一点暖色的黑） |
| clay 橙 | `#D97757` | 色块用：已完成勾的底、新建按钮、强调块 |
| clayText 深橙 | `#B4573A` | 橙色**文字**专用（如“今天 18:00 提醒”），在米白上更清楚 |
| clayTint 浅橙 | `#F1D7C8` | 选中态、今日高亮的平涂底（橙色加米白调出来） |
| soft 暖灰棕 | `#76695C` | 次要文字、已完成文字、划掉线（底色的加深版） |
| rule 浅棕线 | `#CDBFAF` | 极少用：需要很淡的分隔时 |

规则：
- 不用纯白 `#FFFFFF`，不用纯黑 `#000000`。
- 不使用 Material 动态取色，配色固定。
- 橙色一屏 1–2 处；小字号的橙色一律用 clayText，不用 clay。
- 深色模式暂不做，以后再说。

## 3. 线条与色块

### 3.1 墨线
| 参数 | 值 | 说明 |
|---|---|---|
| 主线粗细 | **3.4dp**（用户定稿） | 图标、勾号、复选框、悬浮按钮 |
| 次线粗细 | 1.5dp | 划掉线、分割线 |
| 线端 | 圆头 | 像马克笔 |
| 粗细不均（开，用户选定） | 第二笔 0.75 倍粗、错开约 0.6dp、不透明 | 让线条一边厚一边薄，像墨水笔；不是铅笔那种半透明重描 |
| 端点出头 | 0–2dp | 线条在拐角处略微画过头或没接上 |
| 收笔变细（可选） | 开放线条的两端在最后 15% 长度内变细到 40% | 参考图里手的线条有这个特点；实现成本稍高，第二阶段再做 |

### 3.2 平涂色块
- 纯色，无描边，无阴影，边缘轻微不规整（和线条用同样的扰动，幅度 0.8 倍）。
- **错位**：一个元素同时有色块和墨线时（例如悬浮按钮：橙色圆 + 黑色圆圈），色块向左上、墨线向右下各偏 3dp。
- 色块边缘也一起抖动。

### 3.3 静止画框
主参考视频的标志：整个画面四周有一圈手绘墨线画框，**这圈框不抖**。App 里每个页面内容区四周也画一圈，内缩 14dp，线宽约 2.6dp，用第 0 帧固定形状。

### 3.4 纸纤维纹理（v4，用户定稿）
历史：颗粒/斑驳版被否（像抹灰）；长弯纤维版“像头发丝”；v3 短直纤维用户称“非常完美”，只是把同向改为随机。v4 规则：
- **短**：3–14dp，只有约 5% 稍长（16–30dp）。
- **直**：整根只微微弯一点，不打卷、不分叉。
- **方向完全随机**：没有统一方向（用户试过“大致同向”后不认同）。
- **两端变淡**：每根纤维中间实、两头虚。
- **嵌在纸里**：一半纤维略带柔化（约 0.6dp 模糊），一半清晰，看起来是纸里的，而不是落在屏幕上的。
- **深浅两种**：60% 比底色略深（暖棕 `rgb(110,86,62)`），40% 比底色略亮（暖白）。
- 盖在整个画面最上层（底色、卡片、墨线上都有），不抖。
- 参数（用户定稿）：**强度 30%**，**密度 1.1×**（每 170 平方 dp 约 1.1 根）。
- **设置里做成三选一**“纸面效果：纸纤维 / 方格线 / 无”，默认纸纤维（方格线见 3.5）。
- 必须按屏幕真实分辨率直接画，不能用小图放大。
- Compose 实现：启动时（或屏幕尺寸变化时）在后台线程用固定随机种子把纤维画进一张和屏幕一样大的 `ImageBitmap`（Android 用 `BlurMaskFilter` 做柔化那一半），之后在根布局最上层 `drawImage` 叠上，`alpha = 强度`，放在单独的 `graphicsLayer` 里，抖动换帧不重画它。
- **性能红线**（演示页踩过的坑）：纹理只在启动或屏幕尺寸变化时生成一次，绝不在每帧或拖动时重新生成；柔化那一半先画到一张图上再整体模糊一次，不能每根纤维单独加模糊。

### 3.5 方格线（纸面效果的另一个选项）
参考 Anthropic 宣传图背景里的浅方格。
- 和纸纤维一样盖在**整个画面最上层**，卡片里面也有方格（用户要求）。两者二选一，不同时出现。
- 格子 24dp 见方，线宽 1 个物理像素（细到不能再细），颜色墨黑、透明度约 9%（对应“效果强度”30%）。
- 格线对齐屏幕像素，保证清晰不发糊；不抖。
- Compose：和纸纤维同一层，在根布局最上层的独立 `graphicsLayer` 里画一次横竖线，抖动换帧不重画，成本极低。

## 4. 字体（用户定稿）

**只用两款字体，标题、正文、按钮全部一样：**

| 用途 | 字体 | 备注 |
|---|---|---|
| 英文和数字 | **Newsreader**（开源 OFL，Google Fonts） | 用户要的是 Anthropic 宣传图里那种衬线字（Anthropic 自有字体，不能打包）。对比 16 款开源衬线字后，Newsreader 最像：字距紧凑、楔形衬线、x 高度大 |
| 全部中文 | **霞鹜文楷 LXGW WenKai**（简体版，开源 OFL） | 标题、正文、按钮一律用文楷 |
| 时间、数字列 | Newsreader + 等宽数字（`fontFeatureSettings = "tnum"`） | |

- 字重：正文 Regular 400，标题 SemiBold 600（文楷用 Medium，它没有 Bold）。
- Compose：**不能**只把两套字体放进一个 `FontFamily`。Compose 只会按字重挑其中一个文件，缺字时直接掉到系统字体，不会逐字回退到文楷。
  做法（M1 已实现，见 `ui/theme/Type.kt`）：用 `android.graphics.Typeface.CustomFallbackBuilder`（API 29+，所以 minSdk 29），Newsreader 为主、文楷为 fallback，分别建“正文”（Regular + Regular）和“标题”（SemiBold + Medium）两个 Typeface，再用 `FontFamily(typeface)` 包给 Compose。
- 体积（用户定的目标）：文楷**打包常用字 3500 字起、最多 5000 字**（以《通用规范汉字表》一级字 3500 为底，再补常用二级字），连同常用标点和全角符号一起做成子集字体放进 App；不在子集里的字自动回退系统字体。具体怎么裁剪到时候再做。
- 字号（sp）：大标题 26 / 标题 20 / 正文 16 / 次要 13。行高 1.5 倍。

## 5. 动效参数

### 5.1 抖动线（line boil，用户已选定，视频实测吻合）
视频逐帧实测：图形每 4–10 个 60fps 帧变一次，平均约每秒 8 次（原片 24fps，一拍二、一拍三混用）。抖动是整条线低频地“晃一下形”，不是锯齿状毛刺，所以扰动间隔用大值（12dp）。

| 参数 | 值 | 说明 |
|---|---|---|
| 帧率 | **8 fps**（每 125ms 换一帧） | |
| 循环帧数 | **3 帧** | 预先算好 3 个版本轮流播放 |
| 抖动幅度 | **0.8dp** | 小元素（复选框、勾）用 0.5–0.6 倍 |
| 扰动间隔 | **12dp** | |

### 5.2 动效语汇（来自主参考视频）
- **画出来**：线条从一个点开始沿路径生长出来（视频开头那根线就是这样出现）。新内容出现一律用这个，不用淡入。
- **弹出**：实心圆点/色块先小后大、略微过冲再回落（0.6 → 1.15 → 1，各一帧）。
- **变形**：一个形状直接变成另一个（网状分子收缩成一个圆点），不做交叉淡化。
- **散开**：圆点碎成一串小点飘走后消失，用于删除。
- **全部按 12fps 或 8fps 跳帧播放**，不用 60fps 平滑插值；这比抖动本身更决定“手翻书感”。

### 5.3 交互动效
- **打勾**：先“啪”地出现橙色块（1 帧），勾号沿路径画出，220ms。
- **划掉已完成事项**：淡墨线从左画到右，260ms。
- **页面切换**：淡入淡出，按 12fps 跳帧，保持手绘感。
- **删除**：条目色块碎成小点散开消失（见 5.2“散开”），约 4–5 帧。

### 5.4 什么不抖
视频实测：**标题文字、页脚小字、四周画框完全不动**，只有插画图形在抖。App 里同理：所有文字、静止画框、滚动中的列表都不抖。

## 6. 在 Jetpack Compose 里怎么实现

### 6.1 全局“抖动时钟”
只在 App 最外层放一个计时器，帧号 0→1→2 循环，通过 `CompositionLocal` 往下传。**只在绘制阶段读帧号**，换帧只重画、不重组。

```kotlin
val LocalBoilFrame = staticCompositionLocalOf<State<Int>> { mutableIntStateOf(0) }

@Composable
fun BoilClock(enabled: Boolean, content: @Composable () -> Unit) {
    val frame = remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { // 切后台自动停
            while (true) {
                delay(125)
                frame.intValue = (frame.intValue + 1) % 3
            }
        }
    }
    CompositionLocalProvider(LocalBoilFrame provides frame, content = content)
}
```

### 6.2 把规整的形状“画歪”
用 `PathMeasure` 每隔 12dp 取一个点，沿法线方向加随机偏移，再用 `quadraticTo` 平滑连接。每帧用不同随机种子（seed = 元素 id × 常数 + 帧号），得到 3 个略有差别的版本。

```kotlin
fun roughen(src: Path, seed: Int, amplitudePx: Float, stepPx: Float): Path {
    val measure = PathMeasure().apply { setPath(src, false) }
    val out = Path()
    val rnd = java.util.Random(seed * 7919L)
    var d = 0f
    while (d <= measure.length) {
        val p = measure.getPosition(d)
        val t = measure.getTangent(d)
        val off = (rnd.nextFloat() * 2 - 1) * amplitudePx
        val x = p.x - t.y * off   // 沿法线偏移
        val y = p.y + t.x * off
        if (d == 0f) out.moveTo(x, y) else out.lineTo(x, y)
        d += stepPx
    }
    return out
}
```

### 6.3 缓存 + 两种画法
```kotlin
// 平涂色块（卡片）：填充，不描边
fun Modifier.sketchFill(color: Color) = composed {
    val frame = LocalBoilFrame.current
    var cache by remember { mutableStateOf<Pair<Size, List<Path>>?>(null) }
    drawBehind {
        if (cache?.first != size) cache = size to List(3) { buildRoughRect(size, seed = it, closed = true) }
        drawPath(cache!!.second[frame.value], color)          // style 默认 Fill
    }
}

// 墨线：主笔 + 0.75 倍粗细、错开 0.6dp 的第二笔，都不透明
fun DrawScope.inkStroke(path: Path, alt: Path, color: Color, width: Float) {
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    translate(0.6.dp.toPx(), 0.4.dp.toPx()) {
        drawPath(alt, color, style = Stroke(width * 0.75f, cap = StrokeCap.Round))
    }
}
```
组件：`SketchCard`（sketchFill）、`SketchCheckbox`（墨线框 + 完成时橙色色块错位）、`SketchFab`（橙色圆色块 + 墨线圆 + 墨线加号，错位 3dp）、`SketchDivider`、`SketchIcon`（线条图标 SVG 路径 → roughen → inkStroke）。

## 7. 性能与耗电

| 措施 | 做法 |
|---|---|
| 预计算 | 每个形状只算 3 帧路径并缓存，尺寸变了才重算 |
| 只重画不重组 | 帧号只在 draw 阶段读取 |
| 低帧率 | 8fps，绘制量约为普通 60fps 动画的 1/7 |
| 后台停止 | `repeatOnLifecycle(RESUMED)` |
| 滚动暂停 | `LazyListState.isScrollInProgress` 为真时停 |
| 省电模式 / 系统关闭动画 | 自动静止 |
| 设置开关 | “线条抖动：开 / 关”，关掉显示第 0 帧，手绘感仍在 |

验收（真机）：开启抖动后列表滚动不卡；放置 10 分钟，耗电与关闭抖动时没有明显差别。

## 8. 组件速查

| 组件 | 画法 |
|---|---|
| 卡片 / 记事条目 | 象牙白平涂色块 + 墨线描边（错位 3dp，线宽 0.7 倍主线），边缘微抖，无阴影 |
| 选中 / 今日 | 蜜桃色平涂 |
| 复选框 | 墨线小方框；完成时橙色块错位垫底 + 墨线勾；文字变次要色并被划掉 |
| 提醒中的条目 | 复选框用橙 clay，时间文字用深橙 clayText |
| 悬浮按钮（新建） | 圆色块 + 墨线圆圈 + 墨线加号，错位 3dp，色块用橙 |
| 按钮 | 主按钮：橙或墨黑平涂 + 文字；次按钮：只有墨线框 |
| 输入框 | 象牙白色块，或一根墨线下划线 |
| 分割线 | 尽量用留白代替；需要时一根 1.5dp 淡墨线 |
| 图标 | 3.4dp 墨线图标，可从 Lucide 取 SVG 再 roughen |
| 空状态插画 | 参考图那种：一两个平涂色块 + 一个墨线小物件（铅笔、纸、手），会抖 |

## 8.1 间距（要宽松）
用户反馈：元素之间不能挤。
| 位置 | 值 |
|---|---|
| 静止画框离屏幕边 | 12dp |
| 内容离画框（左右） | ≥ 22dp（即离屏幕边 ≥ 34dp） |
| 卡片之间 | 20dp |
| 卡片高度（两行） | 60dp |
| 复选框离卡片左边 | 18dp |
| 复选框和文字之间 | 20dp |
| 悬浮按钮离画框 | 右、下各 ≥ 30dp，且不压住最后一张卡片 |

## 9. 已确认
- [x] 抖动参数（定稿）：8fps / 3 帧循环 / 幅度 0.8dp / 扰动间隔 12dp / 主线 3.4dp；抖动、色块错位、卡片描边、墨线粗细不均、静止画框 全开
- [x] 配色（定稿）：底色 #E3D9CD + 橙 + 墨黑 + 米白，见第 2 节
- [x] 先只做浅色模式
- [x] 字体：英文 Newsreader，中文全部霞鹜文楷，常用字 3500–5000 打包（第 4 节）
- [x] 纸面效果三选一：纸纤维 / 方格线 / 无（3.4、3.5 节）
- [x] 纸纤维纹理：v4，强度 30%、密度 1.1×，设置里可开关（3.4 节）
- [x] 间距放宽，见 8.1
- [x] 画法：墨线 + 平涂色块 + 错位，Newsreader + 文楷
- [x] 主参考：Claude Founder House 视频的画框、文字不抖、“画出来/弹出/变形/散开”动效（不再用它的橙色整页底）

## 10. 待办
- [ ] 深色模式（以后再说）
