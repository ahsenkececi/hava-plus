package com.ahsen.hava.ui.component

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ahsen.hava.ui.theme.GlassBorder
import com.ahsen.hava.ui.theme.HavaYellow

/**
 * Telefonun baktığı yön (pusula), derece. 0 = kuzey. Sensör yoksa null.
 *
 * DisposableEffect: ekran açıkken sensörü dinler, ekrandan çıkınca dinlemeyi bırakır
 * (yoksa pil boşa harcanır).
 */
@Composable
fun rememberCompassHeading(): Float? {
    val context = LocalContext.current
    var heading by remember { mutableStateOf<Float?>(null) }
    DisposableEffect(context) {
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (manager == null || sensor == null) return@DisposableEffect onDispose { }
        val matrix = FloatArray(9)
        val orientation = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                SensorManager.getOrientation(matrix, orientation)
                heading = ((Math.toDegrees(orientation[0].toDouble()).toFloat()) + 360f) % 360f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { manager.unregisterListener(listener) }
    }
    return heading
}

/**
 * Rüzgâr pusulası. Ok rüzgârın ESTİĞİ yönü gösterir ve telefon döndükçe gerçek dünyaya göre
 * sabit kalır: telefonu çevirirsen ok ters yöne döner, tıpkı gerçek bir pusula gibi.
 *
 * @param fromDegrees Rüzgârın geldiği yön (Open-Meteo'nun verdiği değer).
 */
@Composable
fun WindCompass(fromDegrees: Double, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val heading = rememberCompassHeading() ?: 0f
    val target = (fromDegrees + 180.0).toFloat() - heading
    val northTarget = -heading

    // 359° → 1° geçişinde ok bütün daireyi dönmesin: her seferinde en kısa yoldan dön.
    val arrow = remember { Animatable(target) }
    val north = remember { Animatable(northTarget) }
    LaunchedEffect(target) { arrow.animateTo(arrow.value + shortestDelta(arrow.value, target), tween(250)) }
    LaunchedEffect(northTarget) { north.animateTo(north.value + shortestDelta(north.value, northTarget), tween(250)) }

    Canvas(modifier = modifier.size(size)) {
        val r = this.size.minDimension / 2
        drawCircle(GlassBorder, radius = r - 1.dp.toPx(), style = Stroke(width = 1.dp.toPx()))
        // Kuzey işareti: küçük sarı nokta, gerçek kuzeyde durur.
        rotate(north.value) {
            drawCircle(HavaYellow, radius = 2.5.dp.toPx(), center = Offset(center.x, center.y - r + 4.dp.toPx()))
        }
        // Ok
        rotate(arrow.value) {
            val top = Offset(center.x, center.y - r * 0.62f)
            drawLine(Color.White, Offset(center.x, center.y + r * 0.55f), top, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            val head = Path().apply {
                moveTo(top.x, top.y - 2.dp.toPx())
                lineTo(top.x - r * 0.28f, top.y + r * 0.32f)
                lineTo(top.x + r * 0.28f, top.y + r * 0.32f)
                close()
            }
            drawPath(head, Color.White)
        }
    }
}

private fun shortestDelta(from: Float, to: Float): Float = ((to - from) % 360f + 540f) % 360f - 180f
