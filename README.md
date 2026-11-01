# Leaf

自用的纯本地 Android 待办 App，目前完成 M2 Room 数据层与清单最小可用版；M1 已真机验收，M2 待真机验收。

## 本地运行

1. Android Studio 打开本仓库根目录，等待 Gradle 同步。
2. SDK 使用本地 local.properties 配置（不提交）；当前机器为 E:\Software\android\sdk。
3. Gradle JDK 选择 Android Studio 自带的 jbr。
4. 连接已开启 USB 调试的手机，选择 app 和手机设备，点击 Run。

构建与测试：` .\gradlew.bat assembleDebug testDebugUnitTest `。
M2 共 5 项 JUnit4 单元测试，已通过；覆盖日期格式、创建、状态转换、排序和 schema 存在性。

构建版本沿用 Hello 测试工程：Gradle 9.6.0、AGP 9.5.0、
Kotlin 2.2.10、Compose BOM 2026.02.01，compileSdk / targetSdk 37。
minSdk 29，包名 com.leaftodo.app。

若本机 JDK 25 报 Unable to establish loopback connection，可仅在当前 PowerShell 会话设置：
`$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=$PWD"`，再运行构建。
该参数只调整构建进程的本机 socket 临时路径。

## M1 真机验收

- 桌面名称 Leaf；默认进入今天；今天、清单、日历三个标签切换正确。
- 背景和系统栏为米棕色，状态栏、系统导航图标为深色。
- 中文为文楷；测试文字的英文和数字为 Newsreader 衬线字。
- 选中标签为浅橙底、墨黑字；未选中为暖灰棕字。
- 三页都有橙色圆形加号，点击无操作。
- 系统切换深色模式后，Leaf 仍保持相同浅色。
- M1 时尚无数据库和列表；M2 已加入 Room 和清单操作。编辑、提醒、设置、手绘效果留待后续里程碑。

字体未裁剪：文楷 Regular / Medium，Newsreader 14pt Regular / SemiBold。
资源文件位于 app/src/main/res/font，授权说明位于 app/src/main/assets/licenses。


## M2 数据层与清单

- Room 2.8.5：runtime、ktx、compiler（KSP）、testing（仅仪器测试）；Room Gradle 插件 2.8.5。
- KSP 2.3.12 已构建通过，未切换 2.3.13；保留 AGP 内置 Kotlin。
- lifecycle-viewmodel-compose、lifecycle-runtime-compose：复用 2.6.1 版本声明。
- 数据库 leaf.db，版本 1；结构在 app/schemas/com.leaftodo.app.data.LeafDatabase/1.json。
- 清单仅显示无日期事项，支持新增、完成、划掉和撤回；创建时日期、时间为空。
- 未结束按创建时间升序，已结束按结束时间降序。今天页使用真实日期。

仪器测试：手机开启 USB 调试、连接电脑并允许调试后运行：
` .\gradlew.bat connectedDebugAndroidTest `。
TaskDaoTest 有 3 项测试：无日期筛选、状态持久化、日期时间往返。
本次测试 APK 编译通过，但执行受阻：`com.android.builder.testing.api.DeviceException: No connected devices!`。

手机验收：
1. Android Studio 选择 app 与手机，点击 Run 安装。
2. 清单页点 ＋，依次添加“买牛奶”“取快递”“给妈妈打电话”，确认顺序。
3. 点“买牛奶”的方框，确认橙底勾和灰字，移到“已结束”。
4. 长按“取快递”，选择“划掉（不做了）”，确认减号、灰字和删除线。
5. 从最近任务划掉 App 再打开，确认三条及状态保留。
6. 点“买牛奶”方框撤回；长按“取快递”选择“撤回成待办”。
7. 空白或全空格标题不能添加；键盘完成可以添加有效标题。
8. 今天页日期和星期正确；今天和日历的 ＋ 暂无操作。

M2 不包含编辑、日期时间选择、删除、今天列表、折叠、创建日期展示或手绘效果。
