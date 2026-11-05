@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.example.expressivelauncher.ui.home

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
    // При возвращении на стол время обновляем сразу: пока процесс был заморожен, тики могли не приходить
    remember(refreshKey) { now = Date() }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { now = Date() }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK); addAction(Intent.ACTION_TIME_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        now = Date()
        onDispose { context.unregisterReceiver(receiver) }
    }
    return now to DateFormat.is24HourFormat(context)
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
 * Часы как элемент сетки. Крупные: часы и минуты в двух фигурах Expressive, которые чуть заходят друг на друга
 * и проворачиваются при смене значения. Размер вычисляется из ячеек, поэтому подходит для любой сетки.
 */
@Composable
fun ClockCard(style: ClockStyle, refreshKey: Int) {
    val s = LocalSettings.current
    val (now, is24) = rememberTime(refreshKey)
    val locale = Locale.getDefault()
    val hours = remember(now, is24) { SimpleDateFormat(if (is24) "HH" else "h", locale).format(now) }
    val minutes = remember(now) { SimpleDateFormat("mm", locale).format(now) }
    val date = remember(now) { SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEMMMd"), locale).format(now) }
        .replaceFirstChar { it.uppercase() }
    val cs = MaterialTheme.colorScheme
    val (percent, charging) = rememberBattery().value
    val showBattery = s.showBattery && percent > 0
    val horizontal = when (s.clockAlign) {
        ClockAlign.START -> Alignment.Start
        ClockAlign.CENTER -> Alignment.CenterHorizontally
        ClockAlign.END -> Alignment.End
    }
    val rowArrangement = when (s.clockAlign) {
        ClockAlign.START -> Arrangement.Start
        ClockAlign.CENTER -> Arrangement.Center
        ClockAlign.END -> Arrangement.End
    }

    val spec = com.example.expressivelauncher.ui.theme.LocalThemeSpec.current
    if (spec.glass || spec.lite) {
        FlatClock(spec.glass, hours, minutes, date, showBattery, percent, charging, horizontal)
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        val chips: @Composable () -> Unit = {
            if (s.showDate) InfoChip(date, cs.primaryContainer, cs.onPrimaryContainer, null)
            if (showBattery) InfoChip(
                "$percent%", cs.surfaceContainerHigh, cs.onSurface,
                if (charging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
            )
        }
        if (style == ClockStyle.BIG) {
            val chipsHeight = if (s.showDate || showBattery) 44.dp else 0.dp
            val blob = min(min(maxHeight - chipsHeight, (maxWidth / 2f) * 1.05f), 176.dp * s.clockScale).coerceAtLeast(56.dp)
            Column(Modifier.fillMaxWidth(), horizontalAlignment = horizontal, verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
                val calendar = remember(now) { Calendar.getInstance().apply { time = now } }
                Row(horizontalArrangement = Arrangement.spacedBy(-blob * 0.1f)) {
                    DigitBlob(hours, MaterialShapes.Cookie12Sided.toShape(), cs.primary, cs.onPrimary, blob, calendar.get(Calendar.HOUR_OF_DAY) * 30f)
                    DigitBlob(minutes, MaterialShapes.Cookie9Sided.toShape(), cs.tertiary, cs.onTertiary, blob, calendar.get(Calendar.MINUTE) * 40f)
                }
                if (chipsHeight > 0.dp) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { chips() }
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, rowArrangement.let { if (it == Arrangement.Start) Alignment.Start else if (it == Arrangement.End) Alignment.End else Alignment.CenterHorizontally }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.clip(CircleShape).background(cs.primaryContainer).padding(horizontal = 20.dp, vertical = 6.dp)) {
                    RollingText("$hours:$minutes", LocalClockStyle.current.copy(fontSize = 32.sp * s.clockScale, lineHeight = 40.sp * s.clockScale), cs.onPrimaryContainer)
                }
                chips()
            }
        }
    }
}

@Composable
private fun DigitBlob(text: String, shape: Shape, bg: Color, fg: Color, size: Dp, turn: Float) {
    val rotation by animateFloatAsState(turn, MaterialTheme.motionScheme.slowSpatialSpec(), label = "clockTurn")
    val fontSize = with(LocalDensity.current) { (size * 0.46f).toSp() }
    BlobBadge(shape, bg, size, rotation = { rotation }) {
        RollingText(text, LocalClockStyle.current.copy(fontSize = fontSize, lineHeight = fontSize), fg)
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

@Composable
private fun InfoChip(text: String, bg: Color, fg: Color, icon: ImageVector?) {
    Row(
        Modifier.clip(CircleShape).background(bg).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Часы тем Liquid Glass и Lite. Liquid Glass: стеклянная карточка с крупным тонким временем и стеклянными
 * чипами даты и заряда. Lite: спокойный плоский Material You — время текстом, сплошные тональные чипы, без фигур.
 */
@Composable
private fun FlatClock(
    glass: Boolean, hours: String, minutes: String, date: String,
    showBattery: Boolean, percent: Int, charging: Boolean, horizontal: Alignment.Horizontal,
) {
    val s = LocalSettings.current
    val cs = MaterialTheme.colorScheme
    val lightWallpaper = com.example.expressivelauncher.ui.LocalWallpaperLight.current
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        val timeSize = with(LocalDensity.current) { min(maxHeight * 0.5f, maxWidth * 0.26f).toSp() } * s.clockScale
        val chipRow: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (s.showDate) FlatChip(glass, date, null)
                if (showBattery) FlatChip(glass, "$percent%", if (charging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull)
            }
        }
        if (glass) {
            com.example.expressivelauncher.ui.components.LiquidSurface(
                androidx.compose.foundation.shape.RoundedCornerShape(32.dp), Modifier.fillMaxSize(), blockTouches = false,
            ) {
                Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp), horizontalAlignment = horizontal, verticalArrangement = Arrangement.Center) {
                    RollingText(
                        "$hours:$minutes",
                        LocalClockStyle.current.copy(fontSize = timeSize, lineHeight = timeSize, fontWeight = androidx.compose.ui.text.font.FontWeight.Light),
                        cs.onSurface,
                    )
                    chipRow()
                }
            }
        } else {
            // Lite: текст прямо на обоях, читаемый за счёт цвета, а не подложки
            val onWallpaper = if (lightWallpaper) Color(0xFF1B1B1F) else Color.White
            Column(Modifier.fillMaxWidth(), horizontalAlignment = horizontal, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
                Text(
                    "$hours:$minutes",
                    style = LocalClockStyle.current.copy(fontSize = timeSize, lineHeight = timeSize, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal),
                    color = onWallpaper, maxLines = 1,
                )
                chipRow()
            }
        }
    }
}

@Composable
private fun FlatChip(glass: Boolean, text: String, icon: ImageVector?) {
    val cs = MaterialTheme.colorScheme
    val content: @Composable () -> Unit = {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) Icon(icon, null, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    if (glass) {
        // Внутри стеклянной карточки: обои второй раз не подкладываются
        com.example.expressivelauncher.ui.components.LiquidSurface(CircleShape, overBlur = true, shadow = false, blockTouches = false) { content() }
    } else {
        androidx.compose.material3.Surface(shape = CircleShape, color = cs.surfaceContainerHigh, contentColor = cs.onSurface) { content() }
    }
}
