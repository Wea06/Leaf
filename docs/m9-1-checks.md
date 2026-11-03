# M9-1 实现与检查记录

2026-11-03，分支 `m9-sketch-ui`，基线 main `31b0b6b`（PR #14），开工前 `git pull --ff-only` 确认最新。

## 实现

- 手绘路径按两份定稿 HTML 的 mulberry32、pts、trace、ink、blob、circlePath 移植；路径缓存三帧，8fps 抖动；静止画框固定第 0 帧。
- 主页面、卡片、复选框、悬浮按钮、标签、撤回条、编辑、设置、菜单和所有应用弹窗换装；日期/时间选择器保留原状态和转换逻辑，在弹窗中完整适配宽度。
- 本页用户操作触发 330ms、12fps 的打勾/划掉；撤回直接消失。页面切换和列表换位仍即时完成。
- 日历正式使用方案 A；设置 → 外观加入默认开启的“线条抖动”，仅存 SharedPreferences。

## 改动文件

以下路径均相对仓库根目录：

- `app/src/main/java/com/leaftodo/app/data/RoughLogic.kt`：纯随机路径、跳帧和弹出逻辑。
- `app/src/main/java/com/leaftodo/app/ui/sketch/Sketch.kt`：绘制缓存、双笔墨线、平涂和公共组件、弹窗及选择器宽度适配。
- `app/src/main/java/com/leaftodo/app/ui/sketch/Boil.kt`：生命周期时钟、系统限制和全局滚动暂停。
- `app/src/main/java/com/leaftodo/app/ui/sketch/DrawingActions.kt`：本页操作进度，跨分块保留，离页销毁，滚动结束绘制。
- `app/src/main/java/com/leaftodo/app/ui/sketch/CompositionProbe.kt`：可选测试观察器；正常使用时没有观察器，不记录日志。
- `app/src/main/java/com/leaftodo/app/MainActivity.kt`：画框、时钟、标签、悬浮按钮与自定义撤回提示条。
- `app/src/main/java/com/leaftodo/app/TaskListScreen.kt`、`SectionedTaskList.kt`：卡片换装、操作动画、标题反馈、滚动暂停。
- `app/src/main/java/com/leaftodo/app/CalendarScreen.kt`：正式日历标记、月份箭头、年月弹窗、滚动暂停。
- `app/src/main/java/com/leaftodo/app/EditTaskScreen.kt`：输入和选择行、清空叉号、公共弹窗。
- `app/src/main/java/com/leaftodo/app/SettingsScreen.kt`、`ReviewSettingsBlock.kt`、`ReminderSettingsBlock.kt`：设置卡片、手绘开关、外观入口和公共弹窗。
- `app/src/main/java/com/leaftodo/app/settings/LeafSettings.kt`：boilEnabled 持久化与 StateFlow。
- `app/src/test/java/com/leaftodo/app/data/RoughLogicTest.kt`：JS 固定种子前五个数、路径与动画逻辑测试。
- `app/src/androidTest/java/com/leaftodo/app/SketchRenderingTest.kt`：独立界面的重组和 30 条卡片滚动测试，不打开用户数据库。
- `SPEC.md`、`STYLE.md`、`ROADMAP.md` 和本记录。

## 编译和单元测试

已通过：`./gradlew.bat assembleDebug testDebugUnitTest assembleRelease assembleDebugAndroidTest`。

154 项单元测试，0 失败，包含 FontCoverageTest；无需重新裁剪字体。Release Lint 同样通过。

本机 JDK socket 默认临时目录导致构建连接失败，执行时使用环境变量解决，没有修改工程配置或升级工具链：

```powershell
$env:JAVA_HOME='<Android Studio 安装目录>\jbr'
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=<仓库目录>\build'
./gradlew.bat assembleDebug testDebugUnitTest assembleRelease assembleDebugAndroidTest
```

Lint 的 FIR 分析器曾在新组件构造属性上报内部异常；改为普通构造参数和类体属性后完整检查通过，没有屏蔽 Lint。

## 真机检查

荣耀 Magic6，Android 17 / API 37，1264×2800，系统动画缩放 1.0。

- 最终 APK 的隔离渲染测试 `OK (1 test)`。
- 静置采样 1500ms，确认时钟实际遍历 `[2, 0, 1]`；TaskCard 和 CalendarDay 的组合提交次数增量都是 0。
- 30 条真实 TaskCard 组件、LazyColumn、滚动暂停和 7 个 CalendarDay，向上/下各快速滑动 8 次，每次 180ms；没有向手机数据库写入测试数据。
- 最终一次 `dumpsys gfxinfo com.leaftodo.app`：755 帧，Janky frames 7（0.93%），legacy 7（0.93%）；95th percentile 12ms，99th percentile 19ms。
- 前两次同类测试分别为 1.05% 和 0.76%；不是零掉帧保证，也不是长期功耗测试。测试界面没有叠加纸纤维层，完整页面的主观流畅度仍需用户验收。
- 真机截图复核今天页、日历、日期/时间弹窗，修复日期选择器右侧列裁切。截图留在忽略的 `build/qa/`，没有提交手机截图。
- 初次尝试 Compose 测试规则触发既有 Espresso 对 API 37 的反射接口不兼容；最终测试使用标准 ActivityScenario 和系统触摸输入，无需新依赖。

复测命令（手机先解锁并保持前台）：

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.leaftodo.app.SketchRenderingTest com.leaftodo.app.test/androidx.test.runner.AndroidJUnitRunner
adb logcat -d -s LeafSketchQA:I
```

## 边界核对

- schema `1.json`、Manifest、依赖、工具链版本和应用版本未变。
- 合并后 Manifest 的 5 项权限与原有应用权限一致；没有 INTERNET。
- 数据库、备份、提醒和回顾业务逻辑未改。
- 没有 Material Snackbar、DatePickerDialog、Material Switch 或 FloatingActionButton 调用；菜单阴影为 0。
- 没有加入 M9-2 的切页/翻月移动动画、拖切页、HorizontalPager、animateItem、卡片下沉浮起或移动阴影。
- 没有提交 local.properties、gradle-daemon-jvm.properties、签名或密码；没有 push。

## 手机最值得看的几项

1. 今天/清单/日历对照演示，观察静止文字与画框、轻微抖动的图形。
2. 展开“已结束”，点待办方框、长按划掉，再撤回；检查勾/短横/标题线的绘制，以及换位仍瞬间发生。
3. 设置 → 外观 → 线条抖动，关闭后立即静止；切页和重启仍保存设置。
4. 新建页打开日期和时间选择器，检查周日和最右数字完整；取消退出，不保存测试数据。
5. 日历对照各种状态的小记号；上下快速滑动后，停下观察抖动恢复。

长时间观感、10 分钟功耗、所有字体缩放和极端窗口尺寸尚未完成真机验收；最终视觉以用户验收为准。
