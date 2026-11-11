@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.example.expressivelauncher.ui.home

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.expressivelauncher.data.ClockAlign
import com.example.expressivelauncher.data.ClockStyle
import com.example.expressivelauncher.ui.LocalClockStyle
import com.example.expressivelauncher.ui.LocalSettings
import com.example.expressivelauncher.ui.settings.BlobBadge
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
private fun rememberTime(refreshKey: Int): Pair<Date, Boolean> {
    val context = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    // 12/24-часовой режим — тоже состояние: система читается один раз в минуту, а не на каждую рекомпозицию
    var is24 by remember { mutableStateOf(DateFormat.is24HourFormat(context)) }
    // При возвращении на стол время обновляем сразу: пока процесс был заморожен, тики могли не приходить
    remember(refreshKey) { now = Date() }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                now = Date()
                is24 = DateFormat.is24HourFormat(context)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK); addAction(Intent.ACTION_TIME_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        now = Date()
        is24 = DateFormat.is24HourFormat(context)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return now to is24
}

@Composable
private fun rememberBattery(): State<Pair<Int, Boolean>> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(0 to false) }
    DisposableEffect(context) {
        fun read(i: Intent?) {
            i ?: return
            val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            if (level >= 0) state.value = (level * 100 / scale.coerceAtLeast(1)) to charging
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) = read(i)
        }
        read(ContextCompat.registerReceiver(context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED))
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}

/**
 * Часы как элемент сетки: время крупным текстом прямо на обоях, под ним одной строкой дата и заряд батареи.
 * Без фигур, подложек и чипов — читаемость обеспечивает цвет текста под обои (светлые/тёмные), как у подписей
 * значков. Шрифт — выбранный в настройках. BIG — крупное время над строкой, COMPACT — время и справа дата.
 */
@Composable
fun ClockCard(style: ClockStyle, refreshKey: Int) {
    val s = LocalSettings.current
    val (now, is24) = rememberTime(refreshKey)
    val locale = Locale.getDefault()
    // Форматтеры пересоздаются только при смене языка или 12/24-часового режима, а не три раза в минуту
    val hoursFormat = remember(locale, is24) { SimpleDateFormat(if (is24) "HH" else "h", locale) }
    val minutesFormat = remember(locale) { SimpleDateFormat("mm", locale) }
    val dateFormat = remember(locale) { SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEMMMd"), locale) }
    val time = remember(now, hoursFormat, minutesFormat) { hoursFormat.format(now) + ":" + minutesFormat.format(now) }
    val date = remember(now, dateFormat) { dateFormat.format(now).replaceFirstChar { it.uppercase() } }
    val (percent, charging) = rememberBattery().value
    val showBattery = s.showBattery && percent > 0
    val horizontal = when (s.clockAlign) {
        ClockAlign.START -> Alignment.Start
        ClockAlign.CENTER -> Alignment.CenterHorizontally
        ClockAlign.END -> Alignment.End
    }
    val glass = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current.glass
    // Текст прямо на обоях: цвет по яркости обоев, на тёмных — мягкая тень
    val light = com.example.expressivelauncher.ui.LocalWallpaperLight.current
    val ink = if (light) Color(0xFF1B1B1F) else Color.White
    val shadow = if (light) null else Shadow(Color.Black.copy(alpha = 0.4f), offset = Offset(0f, 2f), blurRadius = 10f)
    val weight = if (glass) FontWeight.Light else FontWeight.Medium

    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        val info: @Composable () -> Unit = {
            if (s.showDate || showBattery) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val text = MaterialTheme.typography.titleMedium.copy(shadow = shadow)
                if (s.showDate) Text(date, style = text, color = ink.copy(alpha = 0.92f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (s.showDate && showBattery) Box(Modifier.size(4.dp).clip(CircleShape).background(ink.copy(alpha = 0.6f)))
                if (showBattery) {
                    Icon(
                        if (charging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull, null,
                        tint = ink.copy(alpha = 0.92f), modifier = Modifier.size(18.dp),
                    )
                    Text("$percent%", style = text, color = ink.copy(alpha = 0.92f), maxLines = 1)
                }
            }
        }
        if (style == ClockStyle.BIG) {
            val infoHeight = if (s.showDate || showBattery) 28.dp else 0.dp
            val size = with(LocalDensity.current) {
                min(min(maxHeight - infoHeight, maxWidth * 0.5f) * 0.9f, 112.dp).toSp()
            } * s.clockScale
            Column(Modifier.fillMaxWidth(), horizontalAlignment = horizontal, verticalArrangement = Arrangement.Center) {
                RollingText(
                    time,
                    LocalClockStyle.current.copy(fontSize = size, lineHeight = size * 1.05f, fontWeight = weight, letterSpacing = (-size.value * 0.025f).sp, shadow = shadow),
                    ink,
                )
                info()
            }
        } else {
            val size = 40.sp * s.clockScale
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, horizontal.let { if (it == Alignment.Start) Alignment.Start else if (it == Alignment.End) Alignment.End else Alignment.CenterHorizontally }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RollingText(
                    time,
                    LocalClockStyle.current.copy(fontSize = size, lineHeight = size * 1.05f, fontWeight = weight, letterSpacing = (-size.value * 0.025f).sp, shadow = shadow),
                    ink,
                )
                info()
            }
        }
    }
}

/** Новое значение въезжает снизу и вытесняет старое вверх. */
@Composable
private fun RollingText(text: String, style: TextStyle, color: Color) {
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()
    val fade = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    AnimatedContent(
        targetState = text,
        transitionSpec = { (slideInVertically(spec) { it } + fadeIn(fade)) togetherWith (slideOutVertically(spec) { -it } + fadeOut(fade)) },
        label = "clockDigits",
    ) { Text(it, style = style, color = color, maxLines = 1) }
}
