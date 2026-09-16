package com.taskerflow.app.ui.block

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskerflow.app.MainActivity

class BlockerActivity : ComponentActivity() {

    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startVibrationLoop()
        setContent {
            BlockerContent(
                blockedAppName = intent.getStringExtra("blocked_app") ?: "this app",
                onOpenTasker = {
                    stopVibration()
                    val i = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(i)
                    finish()
                },
                onGoHome = {
                    stopVibration()
                    val i = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(i)
                    finish()
                }
            )
        }
    }

    private fun startVibrationLoop() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            mgr.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        // Pattern: [delay, vibrate, sleep] with repeat from index 0
        val pattern = longArrayOf(0L, 400L, 400L)
        val effect = VibrationEffect.createWaveform(pattern, 0)
        v.vibrate(effect)
    }

    private fun stopVibration() {
        vibrator?.cancel()
    }

    override fun onPause() {
        super.onPause()
        stopVibration()
    }

    override fun onResume() {
        super.onResume()
        startVibrationLoop()
    }

    override fun onDestroy() {
        stopVibration()
        super.onDestroy()
    }
}

@Composable
private fun BlockerContent(
    blockedAppName: String,
    onOpenTasker: () -> Unit,
    onGoHome: () -> Unit
) {
    Surface(color = Color(0xFF090A10)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF2A1313), Color(0xFF090A10))
                    )
                )
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🔒", fontSize = 72.sp)
            Spacer(Modifier.height(20.dp))
            Text(
                "FOCUS LOCK ACTIVE",
                color = Color(0xFFFF3D57),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "$blockedAppName is blocked",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your Health is below 50%.\nComplete a task in Tasker Flow to recover and unlock your apps.",
                color = Color(0xFF9E9E9E),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(36.dp))
            Button(
                onClick = onOpenTasker,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB300),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("OPEN TASKER FLOW", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = onGoHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go to Home Screen", color = Color(0xFF9E9E9E))
            }
        }
    }
}
