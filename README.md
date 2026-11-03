# 🍃 Leaf

一个手绘风格的小待办 App。只做待办这一件事，完全不联网，数据只存在你自己的手机里。

## 它能做什么

- 点 ＋ 记一件事。不选日期，它就待在“清单”里；选了日期，那天会出现在“今天”；再选个时间，到点会提醒你。
- 所有日期都写成“10月20日 周一”这样，带星期，不用自己去数。
- 做完了点一下打勾；不想做了长按划掉；点错了可以撤回。
- 每条事都能展开写详细内容。时间默认是空的，你不选就不填，改标题也不会动时间。
- 左右滑切换 今天 / 清单 / 日历，日历上能看到每天有没有事。
- 过了日期没做完的事不会丢：今天页列出近 7 天没做完的，清单里有“过期没做完”。
- 每晚 23:00 弹一条回顾，告诉你今天做完了几条（设置里可以改时间或关掉）。
- 设置里可以导出 / 导入备份。换手机、重装前记得先导出。

## 下载安装

到 [Releases](../../releases) 下载最新的 `.apk`，在手机上打开安装。需要 Android 10 或更新的系统。

- 安装时手机提示“未知来源 / 有风险”，点继续安装就行（Leaf 不在应用商店里）。
- 装好后先做两步，不然提醒可能不弹：
  1. 打开 Leaf → 右上角“设置”，允许通知；
  2. 设置里“自启动和后台活动”那一条，照着提示打开（允许自启动、后台不限制）。

## 隐私

Leaf 没有联网权限（每次改代码都有自动检查保证这一点），不收集任何数据，没有统计和广告。你记的事只在你手机里，备份文件也只存到你自己选的位置。

## 自己编译

用 Android Studio 打开本仓库，等 Gradle 同步完，连上手机点 Run 即可。会装上“Leaf 开发版”（包名 `com.leaftodo.app.debug`），和正式版数据互不影响。

需求、外观和开发过程的文档：

- [SPEC.md](SPEC.md)：需求
- [STYLE.md](STYLE.md)：外观和手绘风格
- [ROADMAP.md](ROADMAP.md)：开发计划
- [docs/DEVLOG.md](docs/DEVLOG.md)：每一步的开发记录和验收方法

技术：Kotlin、Jetpack Compose、Room，只用标准 Android 接口，不针对某个牌子的手机。

## 许可

- 代码：[MIT](LICENSE)。
- 字体不在 MIT 范围内，各自按 [SIL Open Font License 1.1](https://openfontlicense.org) 授权，许可全文在 [app/src/main/assets/licenses](app/src/main/assets/licenses)：
  - 中文：[霞鹜文楷](https://github.com/lxgw/LxgwWenKai) 的常用字子集，按 OFL 要求改名为 “Leaf Kai”；
  - 英文和数字：[Newsreader](https://github.com/productiontype/Newsreader)。
