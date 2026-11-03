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

## 自用阶段（v0.1.0 起，用户手机上的正式版里是真数据）
- 数据库结构冻结：`app/schemas/` 里已有的 json 一个字都不能改、不能删（自动检查会报红叉）。要改表：数据库版本 +1、生成新的 schema 文件、写 Migration 和迁移测试，备份格式按 SPEC 7.1 处理。
- 真机测试只装开发版（debug，包名 com.leaftodo.app.debug，桌面名“Leaf 开发版”）。不要卸载、不要覆盖、不要往正式版 com.leaftodo.app 上跑测试。
- 不改 release 签名配置，不把 keystore 或密码写进任何文件。
- 正式版不开 R8 压缩（v0.1.0 开了会一启动就闪退）。要重新打开，必须先在真机上装 release 版跑通，不能只看编译通过。
- 要出新正式版时 versionCode +1、versionName 按里程碑改，并在交付说明里提醒用户“装前先在正式版里导出一份备份”。

## 技术
- Kotlin + Jetpack Compose (Material 3) + Room + SharedPreferences（设置）；单 Activity。
- 能写单元测试的逻辑（日期格式化、近 7 天筛选、清单分块（listSections）、日历标记计算、导出导入）都要写测试。

## 交付
- 在新分支上工作，分支名“里程碑号-做什么”，例如 m2-room-task。
- 小步 commit，信息格式 feat:/fix:/docs:/refactor: + 中文说明。
- 结束前必须跑 ./gradlew assembleDebug testDebugUnitTest 并通过。
- 最后用中文告诉用户：做了什么、改了哪些文件、**在手机上怎么验收（一步一步）**、有什么没做或不确定。
