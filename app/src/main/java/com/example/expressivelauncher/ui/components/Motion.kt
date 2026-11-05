@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.expressivelauncher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/** Промежуточная фигура между двумя формами Material; [progress] от 0 до 1. */
class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress.coerceIn(0f, 1f)).asComposePath()
        path.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(path)
    }
}

/**
 * «Волна» значков при возвращении на стол. Один общий таймер на все значки вместо анимации в каждой ячейке:
 * значение читается только в graphicsLayer, поэтому не вызывает рекомпозиций.
 */
@Stable
class WakeWave {
    /** 0..1 — ход всей волны; 1 — волна закончилась. */
    var time by mutableFloatStateOf(1f)

    fun arm() { time = 0f }

    /** Прогресс элемента с порядковым номером [order]; ближе к концу немного перелетает за единицу. */
    fun value(order: Int): Float {
        if (time >= 1f) return 1f
        val start = order.coerceAtMost(MAX_ORDER) * WAVE_STEP_MS
        val p = ((time * TOTAL_MS - start) / ITEM_MS).coerceIn(0f, 1f)
        val q = p - 1f
        return 1f + 2.4f * q * q * q + 1.4f * q * q
    }

    companion object {
        const val TOTAL_MS = MAX_ORDER * WAVE_STEP_MS + ITEM_MS
    }
}

/**
 * Движение значка на рабочем столе: вдавливание при нажатии, волна при возвращении домой
 * и «приземление» после перетаскивания. Значения читаются в graphicsLayer, поэтому не вызывают рекомпозиций.
 * [hidden] — лямбда, а не готовое значение: начало перетаскивания не должно перекомпоновывать все ячейки.
 */
@Composable
fun Modifier.homeItemMotion(pressed: Boolean, hidden: () -> Boolean, wave: WakeWave, order: Int, concealed: () -> Boolean = { false }): Modifier {
    val press by animateFloatAsState(if (pressed) 0.86f else 1f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "press")
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val land = remember { Animatable(1f) }
    // Приземление ловим подпиской на скрытость: чтение состояния в эффекте не тянет за собой композицию
    LaunchedEffect(Unit) {
        var wasHidden = hidden()
        snapshotFlow { hidden() }.collectLatest { h ->
            if (h) {
                // Значок ушёл под палец: масштаб возвращаем сразу, иначе отменённая на полпути
                // анимация приземления оставила бы соседа раздутым
                if (land.value != 1f) land.snapTo(1f)
                wasHidden = true
                return@collectLatest
            }
            if (wasHidden) {
                land.snapTo(1.18f)
                land.animateTo(1f, spatial)
            }
            wasHidden = false
        }
    }
    return graphicsLayer {
        val enter = wave.value(order)
        val scale = press * land.value * (0.84f + 0.16f * enter)
        scaleX = scale
        scaleY = scale
        translationY = (1f - enter) * 26.dp.toPx()
        alpha = if (hidden() || concealed()) 0f else 1f
    }
}

/**
 * Положение ячейки внутри контейнера. Значение меняется анимацией и читается на этапе размещения, без рекомпозиции:
 * так соседи расступаются перед перетаскиваемым значком. [landingFrom] — точка, из которой элемент долетает до места после отпускания.
 */
@Composable
fun Modifier.animatedCell(target: Offset, landingFrom: (() -> Offset)?, landingKey: Any?): Modifier {
    val pos = remember { Animatable(target, Offset.VectorConverter) }
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Offset>()
    val handled = remember { arrayOfNulls<Any>(1) }
    LaunchedEffect(target, landingKey) {
        if (landingKey != null && handled[0] !== landingKey && landingFrom != null) {
            handled[0] = landingKey
            pos.snapTo(landingFrom())
        }
        if (pos.value != target) pos.animateTo(target, spec)
    }
    return offset { IntOffset(pos.value.x.roundToInt(), pos.value.y.roundToInt()) }
}

/** Появление с задержкой по порядку. Только для обычных колонок: в ленивых списках повторялось бы при прокрутке. */
@Composable
fun Modifier.staggerIn(order: Int, stepMs: Long = 45L): Modifier {
    val progress = remember { Animatable(0f) }
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    LaunchedEffect(Unit) {
        delay(order.coerceAtMost(MAX_ORDER) * stepMs)
        progress.animateTo(1f, spec)
    }
    return graphicsLayer {
        val v = progress.value
        val scale = 0.92f + 0.08f * v
        scaleX = scale
        scaleY = scale
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * 24.dp.toPx()
    }
}

private const val MAX_ORDER = 12
private const val WAVE_STEP_MS = 22
private const val ITEM_MS = 420
