# AGENTS.md

你在开发 Leaf：一个自用的安卓待办 App。用户不会写代码，只在真机上验收。

## 动手前
- 先读 SPEC.md（需求）和 STYLE.md（外观）、ROADMAP.md（当前在哪一步）。和对话冲突时以这些文件为准。
- 只做本次任务说的事，不顺手改别的、不提前做后面里程碑的功能。

## 硬规矩
- 永远不加 INTERNET 权限，不加任何联网、统计、崩溃上报的库。
- Room 永远不用 fallbackToDestructiveMigration；exportSchema = true；改表必须数据库版本 +1 并写 Migration 和测试。
- 任何代码都不能用“当前时间”去填用户没填的时间；改标题/内容不改日期和时间。
- 日期显示统一走 formatDate 函数（“M月d日 周X”）。
- 加新依赖前先说明为什么、有没有更小的办法。
- 不提交 keystore、密码、local.properties。

## 自用阶段（v0.1.0 起）
- 用户手机上的**正式版**（包名 com.leaftodo.app，正式 keystore 签名，桌面名“Leaf”）里是用户的真实数据。
- 开发、调试、仪器测试只用 **debug 版**（包名 com.leaftodo.app.debug，桌面名“Leaf 开发版”），和正式版互不影响。
- 永远不要对 com.leaftodo.app 执行卸载、清除数据（adb uninstall、pm clear 等），也不要往它上面装 debug 版。
- 数据库结构已冻结：app/schemas 里已有的 json 永远不改。改表 = 数据库版本 +1 + Migration + 用 MigrationTestHelper 写迁移测试 + 按 SPEC 7.1 处理备份格式（format +1，新版仍能导入旧备份）。CI 会拦。
- 每次发新正式版：versionCode +1；提醒用户装之前先在旧版里导出一份备份；用同一个 keystore 签名覆盖安装，不卸载。

## 技术
- Kotlin + Jetpack Compose (Material 3) + Room + DataStore；单 Activity。
- 能写单元测试的逻辑（日期格式化、近 7 天筛选、日历标记计算、导出导入）都要写测试。

## 交付
- 在新分支上工作，分支名“里程碑号-做什么”，例如 m2-room-task。
- 小步 commit，信息格式 feat:/fix:/docs:/refactor: + 中文说明。
- 结束前必须跑 ./gradlew assembleDebug testDebugUnitTest 并通过。
- 最后用中文告诉用户：做了什么、改了哪些文件、**在手机上怎么验收（一步一步）**、有什么没做或不确定。
