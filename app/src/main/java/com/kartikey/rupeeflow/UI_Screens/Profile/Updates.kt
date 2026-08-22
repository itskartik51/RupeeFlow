package com.kartikey.rupeeflow.UI_Screens.Profile

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val apkUrl: String
)

/**
 * Auto-Cleanup Engine: Deletes previously downloaded APK file once the user updates to that version or higher.
 */
fun cleanOldUpdateApks(context: Context) {
    try {
        val sharedPrefs = context.getSharedPreferences("RupeeFlow_Updates", Context.MODE_PRIVATE)
        val savedPath = sharedPrefs.getString("last_downloaded_apk_path", null)
        val downloadedVersionCode = sharedPrefs.getInt("last_downloaded_version_code", -1)
        val downloadId = sharedPrefs.getLong("last_update_download_id", -1L)

        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            packageInfo.versionCode
        }

        if (downloadedVersionCode != -1 && currentVersionCode >= downloadedVersionCode) {
            // Delete the physical APK file from private storage
            if (savedPath != null) {
                val file = File(savedPath)
                if (file.exists()) {
                    file.delete()
                }
            }
            // Remove download record and notification entry from DownloadManager
            if (downloadId != -1L) {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                dm?.remove(downloadId)
            }
            sharedPrefs.edit().clear().apply()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

suspend fun checkIsUpdateAvailable(context: Context): Boolean = withContext(Dispatchers.IO) {
    cleanOldUpdateApks(context)

    try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            packageInfo.versionCode
        }
        
        val request = Request.Builder()
            .url("https://raw.githubusercontent.com/itskartik51/RupeeFlow/main/Updates/version.json")
            .build()
        val response = OkHttpClient().newCall(request).execute()
        val jsonData = response.body?.string()

        if (jsonData != null) {
            val json = JSONObject(jsonData)
            val serverCode = json.optInt("latest_version_code", 0)
            return@withContext serverCode > currentVersionCode
        }
    } catch (e: Exception) {
        return@withContext false
    }
    return@withContext false
}

@Composable
fun AppUpdateRow(isUpdateAvailableBadge: Boolean) {
    var updateExpanded by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var checkState by remember { mutableStateOf("IDLE") }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedApkUri by remember { mutableStateOf<Uri?>(null) }

    val packageInfo = remember { context.packageManager.getPackageInfo(context.packageName, 0) }
    val currentVersionCode = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode.toInt() else packageInfo.versionCode
    }
    val currentVersionName = remember { packageInfo.versionName ?: "1.0" }

    val installLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (context.packageManager.canRequestPackageInstalls()) {
            downloadedApkUri?.let { uri -> installApk(context, uri) }
        } else {
            Toast.makeText(context, "Permission Denied. Cannot Install.", Toast.LENGTH_SHORT).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notifications disabled.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(updateExpanded) {
        if (updateExpanded && (checkState == "IDLE" || checkState == "UP_TO_DATE")) {
            checkState = "CHECKING"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            withContext(Dispatchers.IO) {
                try {
                    delay(600)
                    
                    val request = Request.Builder()
                        .url("https://raw.githubusercontent.com/itskartik51/RupeeFlow/main/Updates/version.json")
                        .build()
                    val response = OkHttpClient().newCall(request).execute()
                    val jsonData = response.body?.string()

                    if (jsonData != null) {
                        val json = JSONObject(jsonData)
                        val serverCode = json.optInt("latest_version_code", 0)
                        
                        withContext(Dispatchers.Main) {
                            if (serverCode > currentVersionCode) {
                                updateInfo = UpdateInfo(
                                    versionCode = serverCode,
                                    versionName = json.optString("latest_version_name", ""),
                                    releaseNotes = json.optString("release_notes", ""),
                                    apkUrl = json.optString("apk_url", "")
                                )
                                checkState = "AVAILABLE"
                            } else {
                                checkState = "UP_TO_DATE"
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) { checkState = "UP_TO_DATE" }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { checkState = "UP_TO_DATE" }
                }
            }
        }
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .bounceClick(scaleDown = 0.97f) { updateExpanded = !updateExpanded } 
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = "App Update", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text("App Update", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            
            if (isUpdateAvailableBadge && !updateExpanded && checkState != "DOWNLOADING" && checkState != "READY") {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        
        AnimatedVisibility(visible = updateExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, bottom = 16.dp, end = 12.dp),
                verticalAlignment = Alignment.Top 
            ) {
                // LEFT SIDE FIXED
                Column(
                    modifier = Modifier.weight(0.35f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.height(50.dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = "Update", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.height(18.dp), contentAlignment = Alignment.Center) {
                        Text("RupeeFlow", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                    Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.Center) {
                        Text("v$currentVersionName", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // RIGHT SIDE DYNAMIC
                Box(
                    modifier = Modifier.weight(0.65f).padding(start = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(targetState = checkState, label = "UpdateState") { state ->
                        when (state) {
                            "IDLE", "CHECKING" -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.height(50.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.height(18.dp), contentAlignment = Alignment.Center) {
                                        Text("Checking...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    }
                                    Box(modifier = Modifier.height(16.dp)) 
                                }
                            }
                            "UP_TO_DATE" -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.height(50.dp), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Updated", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(50.dp))
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.height(18.dp), contentAlignment = Alignment.Center) {
                                        Text("You're on the latest version!", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
                                    }
                                    Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.Center) {
                                        Text("No new updates available.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    }
                                }
                            }
                            "AVAILABLE" -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.height(50.dp), contentAlignment = Alignment.Center) {
                                        Text("v${updateInfo?.versionName} Available", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            checkState = "DOWNLOADING"
                                            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                            val uri = Uri.parse(updateInfo?.apkUrl)
                                            val fileName = "RupeeFlow_Update_${updateInfo?.versionCode}.apk"
                                            
                                            // Downloads directly to App-Controlled Private Storage
                                            val request = DownloadManager.Request(uri)
                                                .setTitle("RupeeFlow Update")
                                                .setDescription("Downloading latest version...")
                                                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                            
                                            val downloadId = downloadManager.enqueue(request)
                                            
                                            context.getSharedPreferences("RupeeFlow_Updates", Context.MODE_PRIVATE)
                                                .edit()
                                                .putLong("last_update_download_id", downloadId)
                                                .apply()
                                            
                                            coroutineScope.launch(Dispatchers.IO) {
                                                var isDownloading = true
                                                while (isDownloading) {
                                                    val query = DownloadManager.Query().setFilterById(downloadId)
                                                    val cursor = downloadManager.query(query)
                                                    if (cursor != null && cursor.moveToFirst()) {
                                                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                                                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                                            isDownloading = false
                                                            val localUriStr = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                                                            val downloadedFile = File(Uri.parse(localUriStr).path ?: "")
                                                            val finalUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", downloadedFile)
                                                            
                                                            // Store path and version for boot cleaner
                                                            context.getSharedPreferences("RupeeFlow_Updates", Context.MODE_PRIVATE)
                                                                .edit()
                                                                .putString("last_downloaded_apk_path", downloadedFile.absolutePath)
                                                                .putInt("last_downloaded_version_code", updateInfo?.versionCode ?: 0)
                                                                .apply()

                                                            withContext(Dispatchers.Main) {
                                                                downloadedApkUri = finalUri
                                                                checkState = "READY"
                                                                showUpdateReadyNotification(context, finalUri)
                                                            }
                                                        } else if (status == DownloadManager.STATUS_FAILED) {
                                                            isDownloading = false
                                                            withContext(Dispatchers.Main) {
                                                                checkState = "AVAILABLE"
                                                                Toast.makeText(context, "Download Failed", Toast.LENGTH_SHORT).show()
                                                            }
                                                        } else {
                                                            val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                                                            val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                                                            if (bytesTotal > 0) {
                                                                withContext(Dispatchers.Main) { downloadProgress = bytesDownloaded.toFloat() / bytesTotal.toFloat() }
                                                            }
                                                        }
                                                    }
                                                    cursor?.close()
                                                    if (isDownloading) delay(500)
                                                }
                                            }
                                        },
                                        modifier = Modifier.height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Download", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            }
                            "DOWNLOADING" -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.height(50.dp), contentAlignment = Alignment.Center) {
                                        Text("Downloading...", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.height(18.dp), contentAlignment = Alignment.Center) {
                                        LinearProgressIndicator(
                                            progress = { downloadProgress },
                                            modifier = Modifier.fillMaxWidth().padding(end = 16.dp).height(6.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            strokeCap = StrokeCap.Round 
                                        )
                                    }
                                    Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.Center) {
                                        Text("${(downloadProgress * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            "READY" -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.height(50.dp), contentAlignment = Alignment.Center) {
                                        Text("Ready to Install", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                                                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                                    data = Uri.parse("package:${context.packageName}")
                                                }
                                                installLauncher.launch(intent)
                                                Toast.makeText(context, "Allow installs to update", Toast.LENGTH_LONG).show()
                                            } else {
                                                downloadedApkUri?.let { uri -> installApk(context, uri) }
                                            }
                                        },
                                        modifier = Modifier.height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Install", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    }
}

fun installApk(context: Context, apkUri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Error starting installer.", Toast.LENGTH_SHORT).show()
    }
}

fun showUpdateReadyNotification(context: Context, apkUri: Uri) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "rupeeflow_update_channel"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "App Updates",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }

    val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        installIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download_done) 
        .setContentTitle("\uD83D\uDE80 RupeeFlow Update Ready")
        .setContentText("Download complete. Tap here to install.")
        .setPriority(NotificationCompat.PRIORITY_HIGH) 
        .setAutoCancel(true) 
        .setContentIntent(pendingIntent) 
        .build()

    notificationManager.notify(1001, notification)
}
