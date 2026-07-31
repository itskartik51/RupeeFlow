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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var checkState by remember { mutableStateOf("CHECKING") }
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
            Toast.makeText(context, "Notifications disabled. You won't be alerted when download finishes.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        withContext(Dispatchers.IO) {
            try {
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
                                releaseNotes = json.optString("release_notes", "Bug fixes and improvements."),
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Update", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    // Applied Premium Bounce Here
                    IconButton(onClick = onBackClick, modifier = Modifier.bounceClick()) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SystemUpdate, contentDescription = "Update", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(50.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("RupeeFlow", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Text("Current Version: v$currentVersionName", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(40.dp))

            when (checkState) {
                "CHECKING" -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Checking for updates...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
                "UP_TO_DATE" -> {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Updated", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("You're on the latest version!", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                    Text("No new updates available.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
                "AVAILABLE" -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NewReleases, contentDescription = "New", tint = Color(0xFFE65100))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Version ${updateInfo?.versionName} Available!", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("What's New:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(updateInfo?.releaseNotes ?: "", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = {
                            checkState = "DOWNLOADING"
                            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            val uri = Uri.parse(updateInfo?.apkUrl)
                            val request = DownloadManager.Request(uri)
                                .setTitle("RupeeFlow Update")
                                .setDescription("Downloading latest version...")
                                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "RupeeFlow_Update_${updateInfo?.versionCode}.apk")
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            
                            val downloadId = downloadManager.enqueue(request)
                            
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
                                            val downloadedFile = File(Uri.parse(localUriStr).path!!)
                                            val finalUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", downloadedFile)
                                            
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
                        modifier = Modifier.fillMaxWidth().height(52.dp).bounceClick(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download Update", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                "DOWNLOADING" -> {
                    Text("Downloading in background...", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${(downloadProgress * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("You can navigate away, the download will continue.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                "READY" -> {
                    Icon(Icons.Default.Inventory, contentDescription = "Ready", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Update Ready to Install", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                installLauncher.launch(intent)
                                Toast.makeText(context, "Please allow RupeeFlow to install updates.", Toast.LENGTH_LONG).show()
                            } else {
                                downloadedApkUri?.let { uri -> installApk(context, uri) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp).bounceClick(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Install Now", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
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
        ).apply {
            description = "Notifications for RupeeFlow version updates"
        }
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
        .setContentText("Download complete. Tap here to install the latest version.")
        .setPriority(NotificationCompat.PRIORITY_HIGH) 
        .setAutoCancel(true) 
        .setContentIntent(pendingIntent) 
        .build()

    notificationManager.notify(1001, notification)
}
