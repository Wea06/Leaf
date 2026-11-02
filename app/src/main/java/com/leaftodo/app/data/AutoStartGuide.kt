package com.leaftodo.app.data

data class AutoStartGuide(val vendor: String, val steps: String, val targets: List<Pair<String, String>>)

fun autoStartGuide(manufacturer: String, brand: String): AutoStartGuide {
    val names = listOf(manufacturer.lowercase(), brand.lowercase())
    fun matches(vararg keywords: String) = keywords.any { keyword -> names.any { keyword in it } }
    return when {
        matches("honor", "huawei") -> AutoStartGuide(
            "荣耀/华为",
            "应用启动管理 → 找到 Leaf → 关掉“自动管理” → 打开 允许自启动、允许关联启动、允许后台活动",
            listOf(
                "com.hihonor.systemmanager" to "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
                "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            ),
        )
        matches("xiaomi", "redmi", "poco") -> AutoStartGuide(
            "小米", "打开 Leaf 的“自启动”；再到 应用信息 → 省电策略 → 选“无限制”",
            listOf("com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        )
        matches("oppo", "oneplus", "realme") -> AutoStartGuide(
            "OPPO/一加/realme", "应用信息 → 耗电管理（或电池）→ 打开 允许完全后台行为、允许自启动",
            listOf(
                "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
                "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
                "com.oplus.safecenter" to "com.oplus.safecenter.permission.startup.StartupAppListActivity",
            ),
        )
        matches("vivo", "iqoo") -> AutoStartGuide(
            "vivo/iQOO", "打开 Leaf 的“自启动”；再到 电池 → 后台耗电管理 → Leaf → 允许后台高耗电",
            listOf(
                "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
                "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager",
            ),
        )
        else -> AutoStartGuide(
            "这台手机", "应用信息 → 电池 → 选“不限制”或“允许后台活动”；有“自启动”开关也打开", emptyList(),
        )
    }
}
