package com.leaftodo.app.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily as AndroidFontFamily
import android.graphics.fonts.FontStyle
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.leaftodo.app.R

// Build explicit Android fallback chains: Latin uses Newsreader, Chinese uses WenKai.
private fun mixedFamily(context: Context, latin: Int, chinese: Int, weight: Int): FontFamily {
    fun family(resource: Int) = AndroidFontFamily.Builder(
        Font.Builder(context.resources, resource).setWeight(weight).build()
    ).build()
    val typeface = Typeface.CustomFallbackBuilder(family(latin))
        .addCustomFallback(family(chinese))
        .setSystemFallback("sans-serif")
        .setStyle(FontStyle(weight, FontStyle.FONT_SLANT_UPRIGHT))
        .build()
    return FontFamily(typeface)
}

fun leafTypography(context: Context): Typography {
    val body = mixedFamily(context, R.font.newsreader_regular, R.font.lxgw_wenkai_regular, 400)
    val heading = mixedFamily(context, R.font.newsreader_semibold, R.font.lxgw_wenkai_medium, 600)
    fun bodyStyle(size: Int) = TextStyle(fontFamily = body, fontSize = size.sp, lineHeight = (size * 1.5).sp)
    fun headingStyle(size: Int) = TextStyle(fontFamily = heading, fontSize = size.sp, lineHeight = (size * 1.5).sp)
    return Typography(
        displayLarge = headingStyle(26), displayMedium = headingStyle(26), displaySmall = headingStyle(26),
        headlineLarge = headingStyle(26), headlineMedium = headingStyle(26), headlineSmall = headingStyle(26),
        titleLarge = headingStyle(20), titleMedium = headingStyle(20), titleSmall = headingStyle(20),
        bodyLarge = bodyStyle(16), bodyMedium = bodyStyle(16), bodySmall = bodyStyle(13),
        labelLarge = bodyStyle(16), labelMedium = bodyStyle(13), labelSmall = bodyStyle(13),
    )
}
