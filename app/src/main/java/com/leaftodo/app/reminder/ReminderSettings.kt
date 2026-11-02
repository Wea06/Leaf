package com.leaftodo.app.reminder

import android.app.Activity
import android.content.*
import android.net.Uri
import android.provider.Settings

private fun Context.tryOpen(intent: Intent): Boolean = try {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
} catch (_: ActivityNotFoundException) { false } catch (_: SecurityException) { false }

fun openAppDetails(context: Context) {
    context.tryOpen(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
}
fun openNotificationSettings(context: Context) {
    if (!context.tryOpen(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))) openAppDetails(context)
}
fun openExactAlarmSettings(context: Context) {
    if (android.os.Build.VERSION.SDK_INT >= 31 && context.tryOpen(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))) return
    openAppDetails(context)
}
fun openBatterySettings(context: Context) {
    if (context.tryOpen(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")))) return
    if (!context.tryOpen(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) openAppDetails(context)
}
fun openHonorStartup(context: Context) {
    for (pkg in listOf("com.hihonor.systemmanager", "com.huawei.systemmanager")) {
        if (context.tryOpen(Intent().setComponent(ComponentName(pkg, "$pkg.startupmgr.ui.StartupNormalAppListActivity")))) return
    }
    openAppDetails(context)
}
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
