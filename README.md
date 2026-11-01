# Leaf

自用的纯本地 Android 待办 App，目前完成 M1 应用骨架，待真机验收。

## 本地运行

1. Android Studio 打开本仓库根目录，等待 Gradle 同步。
2. SDK 使用本地 local.properties 配置（不提交）；当前机器为 E:\Software\android\sdk。
3. Gradle JDK 选择 Android Studio 自带的 jbr。
4. 连接已开启 USB 调试的手机，选择 app 和手机设备，点击 Run。

构建与测试：` .\gradlew.bat assembleDebug testDebugUnitTest `。
M1 没有业务逻辑单元测试，testDebugUnitTest 当前为 NO-SOURCE。

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
- 尚无数据库、编辑、列表、提醒、设置、手绘效果。

字体未裁剪：文楷 Regular / Medium，Newsreader 14pt Regular / SemiBold。
资源文件位于 app/src/main/res/font，授权说明位于 app/src/main/assets/licenses。
