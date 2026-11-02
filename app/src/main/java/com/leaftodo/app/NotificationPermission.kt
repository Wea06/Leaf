package com.leaftodo.app

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.leaftodo.app.reminder.*

@Composable
internal fun rememberEnableNotifications(onResult: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onResult)
    val prefs = remember(context) { context.getSharedPreferences("reminders", Context.MODE_PRIVATE) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { callback() }
    return {
        val activity = context.findActivity()
        val requested = prefs.getBoolean("notificationRequested", false)
        if (Build.VERSION.SDK_INT >= 33 && !notificationPermissionGranted(context) &&
            (!requested || (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)))) {
            prefs.edit().putBoolean("notificationRequested", true).commit()
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else openNotificationSettings(context)
    }
}
