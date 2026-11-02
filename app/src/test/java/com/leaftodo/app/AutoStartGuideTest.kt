package com.leaftodo.app

import com.leaftodo.app.data.autoStartGuide
import org.junit.Assert.*
import org.junit.Test

class AutoStartGuideTest {
    @Test fun honor() {
        val guide = autoStartGuide("HONOR", "HONOR")
        assertEquals("荣耀/华为", guide.vendor)
        assertEquals("com.hihonor.systemmanager", guide.targets.first().first)
    }
    @Test fun huawei() { assertEquals("荣耀/华为", autoStartGuide("HUAWEI", "HUAWEI").vendor) }
    @Test fun xiaomiRedmi() { assertEquals("小米", autoStartGuide("Xiaomi", "Redmi").vendor) }
    @Test fun xiaomiPoco() { assertEquals("小米", autoStartGuide("Xiaomi", "POCO").vendor) }
    @Test fun onePlus() { assertEquals("OPPO/一加/realme", autoStartGuide("OnePlus", "OnePlus").vendor) }
    @Test fun realme() { assertEquals("OPPO/一加/realme", autoStartGuide("realme", "realme").vendor) }
    @Test fun vivoIqoo() { assertEquals("vivo/iQOO", autoStartGuide("vivo", "iQOO").vendor) }
    @Test fun unknown() {
        val guide = autoStartGuide("Google", "google")
        assertEquals("这台手机", guide.vendor)
        assertTrue(guide.targets.isEmpty())
        assertEquals("应用信息 → 电池 → 选“不限制”或“允许后台活动”；有“自启动”开关也打开", guide.steps)
    }
    @Test fun mixedCase() { assertEquals("荣耀/华为", autoStartGuide("honor", "HONOR").vendor) }
    @Test fun oppo() { assertEquals("OPPO/一加/realme", autoStartGuide("OPPO", "").vendor) }
    @Test fun brandAloneCanMatchEveryAlias() {
        listOf("honor", "huawei", "xiaomi", "redmi", "poco", "oppo", "oneplus", "realme", "vivo", "iqoo").forEach {
            assertEquals(autoStartGuide(it, ""), autoStartGuide("unknown", it))
        }
    }
    @Test fun substringMatch() { assertEquals("小米", autoStartGuide("unknown", "My-REDMI-Phone").vendor) }
    @Test fun firstMatchingGroupWinsRegardlessOfArgumentOrder() {
        assertEquals("荣耀/华为", autoStartGuide("Xiaomi", "HONOR").vendor)
        assertEquals("小米", autoStartGuide("OPPO", "POCO").vendor)
        assertEquals("OPPO/一加/realme", autoStartGuide("vivo", "realme").vendor)
    }
    @Test fun emptyInputsUseFallback() { assertEquals(autoStartGuide("Google", "google"), autoStartGuide("", "")) }
    @Test fun targetsPreservePackageClassAndFallbackOrder() {
        assertEquals(listOf(
            "com.hihonor.systemmanager" to "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
        ), autoStartGuide("honor", "").targets)
        assertEquals(listOf(
            "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
        ), autoStartGuide("xiaomi", "").targets)
        assertEquals(listOf(
            "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
            "com.oplus.safecenter" to "com.oplus.safecenter.permission.startup.StartupAppListActivity",
        ), autoStartGuide("oppo", "").targets)
        assertEquals(listOf(
            "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager",
        ), autoStartGuide("vivo", "").targets)
    }
    @Test fun stepsMatchEachVendor() {
        assertEquals("应用启动管理 → 找到 Leaf → 关掉“自动管理” → 打开 允许自启动、允许关联启动、允许后台活动", autoStartGuide("honor", "").steps)
        assertEquals("打开 Leaf 的“自启动”；再到 应用信息 → 省电策略 → 选“无限制”", autoStartGuide("xiaomi", "").steps)
        assertEquals("应用信息 → 耗电管理（或电池）→ 打开 允许完全后台行为、允许自启动", autoStartGuide("oppo", "").steps)
        assertEquals("打开 Leaf 的“自启动”；再到 电池 → 后台耗电管理 → Leaf → 允许后台高耗电", autoStartGuide("vivo", "").steps)
    }
}
