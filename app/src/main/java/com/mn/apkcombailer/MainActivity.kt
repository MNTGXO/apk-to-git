package com.mn.apkcombailer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GitHub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation\nimport androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Background = Color(0xFF090B10)
private val Card = Color(0xFF151923)
private val Accent = Color(0xFF9277FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MnTheme { HomeScreen() } }
    }
}

@Composable
private fun MnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            background = Background,
            surface = Card,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

@Composable
private fun HomeScreen() {
    var repository by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Paste a GitHub repository URL to get started.") }
    var busy by remember { mutableStateOf(false) }
    var downloadedPath by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()\n    val context = LocalContext.current

    Surface(Modifier.fillMaxSize(), color = Background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Accent.copy(alpha = .16f), shape = RoundedCornerShape(15.dp)) {
                    Icon(Icons.Default.Code, null, tint = Accent, modifier = Modifier.padding(12.dp).size(30.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("MN APK", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("COMBAILER", color = Accent, fontSize = 11.sp, letterSpacing = 4.sp)
                }
                Spacer(Modifier.weight(1f))
                Text("LOCAL", color = Color(0xFF7CE0AE), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Build your APK.", fontSize = 29.sp, fontWeight = FontWeight.Bold)
                Text("Import Android projects from GitHub.", color = Color.LightGray, fontSize = 14.sp)
            }

            InputCard("GITHUB REPOSITORY") {
                OutlinedTextField(
                    value = repository,
                    onValueChange = { repository = it; downloadedPath = null },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://github.com/owner/repository") },
                    leadingIcon = { Icon(Icons.Default.GitHub, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            InputCard("PERSONAL ACCESS TOKEN · OPTIONAL") {
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Required only for private repositories") },
                    leadingIcon = { Icon(Icons.Default.Key, null) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Token is used for this download and is not saved.", color = Color.Gray, fontSize = 11.sp)
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        status = "Downloading repository…"
                        downloadedPath = null
                        try {
                            val folder = GitHubDownloader.download(
                                context = context,
                                repositoryUrl = repository,
                                token = token
                            )
                            downloadedPath = folder.absolutePath
                            status = "Repository downloaded and extracted successfully."
                        } catch (e: Exception) {
                            status = e.message ?: "Download failed."
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(19.dp), color = Color.White, strokeWidth = 2.dp)
                else Icon(Icons.Default.Download, null)
                Spacer(Modifier.width(9.dp))
                Text(if (busy) "DOWNLOADING…" else "DOWNLOAD REPOSITORY", fontWeight = FontWeight.Bold)
            }

            InputCard("BUILD STATUS") {
                Text(status, color = Color(0xFFD2D3E0), fontSize = 13.sp)
                downloadedPath?.let {
                    Text("Saved locally:", color = Color.Gray, fontSize = 11.sp)
                    Text(it, color = Color(0xFF9F8AFF), fontSize = 11.sp)
                }
            }

            InputCard("APK BUILD") {
                Text(
                    "This Android app downloads and extracts the source locally. It does not yet run a complete Android SDK/Gradle build on-device. Use the included GitHub Actions workflow to compile a selected repository and download its APK artifact.",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }

            Text("MN APK COMBAILER  •  VERSION 1.0", color = Color.DarkGray, fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun InputCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Card, RoundedCornerShape(17.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Text(title, color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        content()
    }
}
