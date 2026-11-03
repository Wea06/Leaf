package com.leaftodo.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 界面上的中文都要在裁剪后的 Leaf Kai（文楷子集）里，否则那个字会掉到系统字体，看起来不是楷体。
 * 报错时：在仓库根目录跑 `python tools/fonts/subset_kai.py`（先 pip install fonttools），再提交新字体和 leaf-kai-chars.txt。
 */
class FontCoverageTest {
    private val han = Regex("[㐀-䶿一-鿿豈-﫿]")

    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null && !File(dir, "tools/fonts/leaf-kai-chars.txt").exists()) dir = dir.parentFile
        return requireNotNull(dir) { "找不到 tools/fonts/leaf-kai-chars.txt" }
    }

    @Test fun everyChineseCharacterInAppSourceIsInTheFontSubset() {
        val root = repoRoot()
        val inFont = File(root, "tools/fonts/leaf-kai-chars.txt").readLines(Charsets.UTF_8)
            .filterNot { it.startsWith("#") }.joinToString("").let { text -> han.findAll(text).map { it.value }.toSet() }
        assertTrue("字表太小，可能没生成好", inFont.size >= 3500)
        val missing = sortedMapOf<String, MutableSet<String>>()
        File(root, "app/src/main").walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "xml") }
            .forEach { file ->
                han.findAll(file.readText(Charsets.UTF_8)).map { it.value }.filterNot { it in inFont }.forEach {
                    missing.getOrPut(it) { sortedSetOf() }.add(file.name)
                }
            }
        assertTrue(
            "这些字不在 Leaf Kai 字体里：${missing.entries.joinToString("，") { "${it.key}（${it.value.joinToString()}）" }}。" +
                "请在仓库根目录跑 python tools/fonts/subset_kai.py 重新裁剪字体",
            missing.isEmpty(),
        )
    }
}
