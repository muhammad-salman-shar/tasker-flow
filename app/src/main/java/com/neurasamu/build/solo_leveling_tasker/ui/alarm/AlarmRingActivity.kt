package com.neurasamu.build.solo_leveling_tasker.ui.alarm

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.solo_leveling_tasker.TaskerApp
import com.neurasamu.build.solo_leveling_tasker.data.model.AlarmEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.DismissMethod
import com.neurasamu.build.solo_leveling_tasker.worker.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AlarmRingActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var loadedAlarm by mutableStateOf<AlarmEntity?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen + wake up + max brightness
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val attrs = window.attributes
        attrs.screenBrightness = 1.0f
        window.attributes = attrs

        val alarmId = intent.getLongExtra("alarm_id", -1L)
        if (alarmId <= 0) { finish(); return }

        val app = application as TaskerApp
        scope.launch {
            val alarm = app.repository.getAlarm(alarmId)
            if (alarm == null) { finish(); return@launch }
            loadedAlarm = alarm
            startRinging(alarm)
        }

        setContent {
            val alarm = loadedAlarm
            if (alarm != null) {
                AlarmRingContent(
                    alarm = alarm,
                    onSnooze = {
                        AlarmScheduler.snoozeAlarm(this, alarm.id, alarm.snoozeMinutes)
                        stopRinging()
                        finish()
                    },
                    onDismiss = {
                        // reschedule for next occurrence if repeating
                        if (alarm.enabled && alarm.repeatRule.name != "NEVER") {
                            AlarmScheduler.scheduleAlarm(
                                this, alarm.id, alarm.hour, alarm.minute,
                                alarm.repeatRule.name, alarm.customDays
                            )
                        }
                        stopRinging()
                        finish()
                    }
                )
            } else {
                Box(
                    Modifier.fillMaxSize().background(Color(0xFF090A10)),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Color(0xFFFF1744)) }
            }
        }
    }

    private fun startRinging(alarm: AlarmEntity) {
        // Persistent notification while ringing
        com.neurasamu.build.solo_leveling_tasker.worker.NotificationHelper.showAlarmRinging(
            this, alarm.id, alarm.label, String.format("%02d:%02d", alarm.hour, alarm.minute)
        )
        // Ringtone
        try {
            val uri: Uri = if (alarm.soundUri.isBlank()) {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            } else Uri.parse(alarm.soundUri)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmRingActivity, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
            // Fallback: silent, vibrate still runs
        }

        // Vibration
        if (alarm.vibrate) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0L, 600L, 400L)
            val effect = VibrationEffect.createWaveform(pattern, 0)
            vibrator?.vibrate(effect)
        }
    }

    private fun stopRinging() {
        com.neurasamu.build.solo_leveling_tasker.worker.NotificationHelper.cancelAlarmRinging(this)
        try { mediaPlayer?.stop() } catch (_: Exception) {}
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }
}

@Composable
private fun AlarmRingContent(
    alarm: AlarmEntity,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val timeFmt = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
    val ampmFmt = remember { SimpleDateFormat("a", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEE, dd MMM", Locale.getDefault()) }
    val d = Date(now)

    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF2A0A0A), Color(0xFF090A10))
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Text("⏰ ALARM", color = Color(0xFFFF1744), fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    timeFmt.format(d),
                    color = Color.White,
                    fontSize = 84.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = -3.sp
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    ampmFmt.format(d),
                    color = Color(0xFFFFB300),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }
            Text(
                dateFmt.format(d),
                color = Color(0xFF9E9E9E),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (alarm.label.isNotBlank()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    alarm.label,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.weight(1f))

            if (alarm.dismissMethod == DismissMethod.PIN) {
                // ---- PIN method ----
                Text(
                    "Enter PIN to dismiss",
                    color = Color(0xFFFFAB91),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(16.dp))
                PinDots(filled = pinInput.length, total = alarm.pinCode.length.coerceAtLeast(6))
                if (pinError) {
                    Spacer(Modifier.height(8.dp))
                    Text("Wrong PIN. Try again.", color = Color(0xFFFF1744), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
                PinPad(
                    onDigit = { d ->
                        if (pinInput.length < alarm.pinCode.length) {
                            pinInput += d
                            pinError = false
                            if (pinInput == alarm.pinCode) {
                                onDismiss()
                            } else if (pinInput.length >= alarm.pinCode.length) {
                                pinError = true
                                pinInput = ""
                            }
                        }
                    },
                    onBackspace = { pinInput = pinInput.dropLast(1); pinError = false }
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "No snooze available in PIN mode.",
                    color = Color(0xFF79829C), fontSize = 11.sp
                )
            } else {
                // ---- Easy method ----
                Text(
                    "Swipe up to dismiss",
                    color = Color(0xFF79829C),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    "▲",
                    color = Color(0xFFFFB300),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDismiss() }
                        .padding(20.dp)
                )
                Spacer(Modifier.height(20.dp))

                if (alarm.snoozeEnabled) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1C1C24))
                            .clickable { onSnooze() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "SNOOZE ${alarm.snoozeMinutes} MIN",
                            color = Color(0xFFFFB300),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFF1744), Color(0xFFB71C1C))))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "DISMISS",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PinDots(filled: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (i < filled) Color(0xFFFF1744) else Color.Transparent)
                    .then(
                        if (i >= filled) Modifier.clip(CircleShape)
                            .background(Color(0xFF23232B))
                        else Modifier
                    )
            )
        }
    }
}

@Composable
private fun PinPad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { key ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (key.isBlank()) Color.Transparent else Color(0xFF17171E))
                            .clickable(enabled = key.isNotBlank()) {
                                when (key) {
                                    "⌫" -> onBackspace()
                                    else -> onDigit(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key.isNotBlank()) {
                            Text(
                                key,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
