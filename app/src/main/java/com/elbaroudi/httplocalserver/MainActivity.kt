package com.elbaroudi.httplocalserver

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.compose.setContent
import androidx.core.os.LocaleListCompat
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.elbaroudi.httplocalserver.server.LocalHttpServer
import com.elbaroudi.httplocalserver.server.ServerService
import com.elbaroudi.httplocalserver.ui.theme.HttpLocalServerTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HttpLocalServerTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var isRunning by remember { mutableStateOf(LocalHttpServer.isRunning()) }
    var showSettings by remember { mutableStateOf(false) }
    val serverIp = remember { getLocalIpAddress() ?: "127.0.0.1" }
    val serverUrl = "http://$serverIp:8080"
    val qrBitmap = remember(serverUrl) { generateQRCode(serverUrl) }
    
    val rootDir = File(context.getExternalFilesDir(null), "uploads")
    var files by remember { mutableStateOf(rootDir.listFiles()?.toList() ?: emptyList()) }

    // Refresh file list periodically if running
    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (true) {
                files = rootDir.listFiles()?.toList() ?: emptyList()
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Server Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRunning) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Filled.PlayArrow else Icons.Filled.Stop,
                        contentDescription = null,
                        tint = if (isRunning) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (isRunning) stringResource(R.string.server_running) else stringResource(R.string.server_stopped),
                        fontWeight = FontWeight.SemiBold,
                        color = if (isRunning) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }

            if (isRunning) {
                // URL & QR Section (Scrollable if needed, but here contained)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(serverUrl))
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(serverUrl, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }

                    qrBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .size(140.dp)
                                .padding(4.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        )
                    }
                }

                HorizontalDivider()

                // File Explorer Section
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Text(
                        stringResource(R.string.file_explorer),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    if (files.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.no_files), color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(files) { file ->
                                FileItem(file, context) {
                                    files = rootDir.listFiles()?.toList() ?: emptyList()
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.scan_qr), color = Color.Gray)
            }

            // Action Button
            Button(
                onClick = {
                    val intent = Intent(context, ServerService::class.java)
                    if (isRunning) {
                        context.stopService(intent)
                    } else {
                        context.startService(intent)
                    }
                    isRunning = !isRunning
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFFF44336) else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isRunning) stringResource(R.string.stop_server) else stringResource(R.string.start_server))
            }
        }
    }

    if (showSettings) {
        SettingsDialog(onDismiss = { showSettings = false })
    }
}

@Composable
fun FileItem(file: File, context: Context, onUpdate: () -> Unit) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(file.name, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("${file.length() / 1024} KB", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { shareFile(file, context) }) {
                    Icon(Icons.Filled.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { exportFile(file, context) }) {
                    Icon(Icons.Filled.Download, contentDescription = "Export", tint = Color(0xFF4CAF50))
                }
                IconButton(onClick = { showConfirmDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFF44336))
                }
            }
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.confirm_delete)) },
            confirmButton = {
                TextButton(onClick = {
                    if (file.delete()) {
                        onUpdate()
                    }
                    showConfirmDelete = false
                }) {
                    Text(stringResource(R.string.delete), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Language Section
                Text("App Language / لغة التطبيق", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("ar")
                            AppCompatDelegate.setApplicationLocales(appLocale)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (AppCompatDelegate.getApplicationLocales().toLanguageTags() == "ar") 
                                MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Text(stringResource(R.string.language_ar))
                    }
                    
                    OutlinedButton(
                        onClick = { 
                            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("en")
                            AppCompatDelegate.setApplicationLocales(appLocale)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (AppCompatDelegate.getApplicationLocales().toLanguageTags().contains("en")) 
                                MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Text(stringResource(R.string.language_en))
                    }
                }

                HorizontalDivider()

                // Instagram
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/abdohigh"))
                        context.startActivity(intent)
                    }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Link, contentDescription = null)
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.follow_on_instagram))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

fun shareFile(file: File, context: Context) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share File"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun exportFile(file: File, context: Context) {
    try {
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val targetFile = File(downloadsDir, file.name)
        file.copyTo(targetFile, overwrite = true)
        Toast.makeText(context, context.getString(R.string.file_copied), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error exporting file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun generateQRCode(text: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

fun getLocalIpAddress(): String? {
    try {
        val en = NetworkInterface.getNetworkInterfaces()
        while (en.hasMoreElements()) {
            val intf = en.nextElement()
            val enumIpAddr = intf.inetAddresses
            while (enumIpAddr.hasMoreElements()) {
                val inetAddress = enumIpAddr.nextElement()
                if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                    return inetAddress.hostAddress
                }
            }
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
    return null
}