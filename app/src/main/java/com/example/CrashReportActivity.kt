package com.example

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * CrashReportActivity
 * ───────────────────
 * Standalone diagnostic activity completely isolated from Hilt dependency injection.
 * Safely invoked on severe startup crashes to expose detailed stack traces on-device,
 * preventing generic app lock-outs and facilitating immediate root-cause fixes.
 */
class CrashReportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val errorMessage = intent.getStringExtra("error_message") ?: "No error message"
        val stackTrace = intent.getStringExtra("stack_trace") ?: "No stacktrace available"

        setContent {
            CrashReportScreen(
                errorMessage = errorMessage,
                stackTrace = stackTrace,
                onCopyClick = {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("RetailDost Crash Log", stackTrace)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, "Diagnostics copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                onRestartClick = {
                    // Try to clear preferences/corrupted files
                    try {
                        getSharedPreferences("retaildost_secure_prefs", Context.MODE_PRIVATE).edit().clear().commit()
                        getSharedPreferences("retaildost_secure_prefs_fallback", Context.MODE_PRIVATE).edit().clear().commit()
                    } catch (e: Throwable) {}
                    
                    val restartIntent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    startActivity(restartIntent)
                    finish()
                }
            )
        }
    }
}

@Composable
fun CrashReportScreen(
    errorMessage: String,
    stackTrace: String,
    onCopyClick: () -> Unit,
    onRestartClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "RetailDost Diagnostics",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF4D4F),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "We detected a startup conflict on this device (Android 12+). Please copy these logs and share them so we can fix it!",
                fontSize = 13.sp,
                color = Color(0xFFAAAAAA),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Exception Context:",
                        color = Color(0xFFFFD666),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFFA39E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 3
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF000000), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        val scrollState = rememberScrollState()
                        Text(
                            text = stackTrace,
                            color = Color(0xFF52C41A),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCopyClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D2D2D)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Copy Log", color = Color.White)
                }
                
                Button(
                    onClick = onRestartClick,
                    modifier = Modifier.weight(1.3f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4F)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Reset & Restart", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
