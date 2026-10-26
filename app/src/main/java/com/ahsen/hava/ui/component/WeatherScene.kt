package com.ahsen.hava.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ahsen.hava.domain.model.WeatherCondition
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/** Arka planda çizilecek sahne türü. */
enum class SceneKind { Sun, Moon, PartlySunny, PartlyMoon, Clouds, Fog, Rain, Snow, Storm }

fun sceneKindFor(condition: WeatherCondition, isDay: Boolean): SceneKind = when (condition) {
    WeatherCondition.CLEAR -> if (isDay) SceneKind.Sun else SceneKind.Moon
    WeatherCondition.MAINLY_CLEAR,
    WeatherCondition.PARTLY_CLOUDY,
    -> if (isDay) SceneKind.PartlySunny else SceneKind.PartlyMoon
    WeatherCondition.OVERCAST, WeatherCondition.UNKNOWN -> SceneKind.Clouds
    WeatherCondition.FOG -> SceneKind.Fog
    WeatherCondition.DRIZZLE, WeatherCondition.RAIN, WeatherCondition.RAIN_SHOWERS -> SceneKind.Rain
    WeatherCondition.SNOW, WeatherCondition.SNOW_SHOWERS -> SceneKind.Snow
    WeatherCondition.THUNDERSTORM -> SceneKind.Storm
}

/**
 * Canlı gökyüzü: güneş, bulut, yağmur, kar, şimşek ve yıldızlar Canvas ile çizilir.
 *
 * Tek bir zaman değeri `t` 40 saniyede 0'dan 1'e gider ve tekrar eder. Her parçacığın
 * hızı tam sayıdır; böylece t = 1'de herkes başladığı yere döner ve döngü fark edilmez.
 * `t` sadece çizim (draw) aşamasında okunur: her karede yeniden çizim olur ama
 * yeniden composition olmaz, bu da animasyonu ucuzlatır.
 */
@Composable
fun WeatherScene(condition: WeatherCondition, isDay: Boolean, modifier: Modifier = Modifier) {
    val kind = sceneKindFor(condition, isDay)
    val t by rememberInfiniteTransition(label = "scene").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = LOOP_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sceneTime",
    )
    // Hava değişince sahneler yumuşakça birbirine karışır.
    Crossfade(targetState = kind, animationSpec = tween(900), label = "sceneFade", modifier = modifier) { k ->
        val particles = remember(k) { Particles.create(k) }
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawScene(k, t, particles)
        }
    }
}

private const val LOOP_MS = 40_000

/** Rastgele ama sabit (seed'li) parçacıklar: her açılışta aynı gökyüzü. */
private class Particles(
    val drops: List<Drop>,
    val flakes: List<Flake>,
    val stars: List<Star>,
) {
    companion object {
        fun create(kind: SceneKind): Particles {
            val r = Random(42)
            val dropCount = when (kind) {
                SceneKind.Rain -> 110
                SceneKind.Storm -> 150
                else -> 0
            }
            val flakeCount = if (kind == SceneKind.Snow) 90 else 0
            val starCount = if (kind == SceneKind.Moon || kind == SceneKind.PartlyMoon) 70 else 0
            return Particles(
                drops = List(dropCount) {
                    Drop(r.nextFloat(), r.nextFloat(), 40 + r.nextInt(30), 0.025f + r.nextFloat() * 0.03f, 0.10f + r.nextFloat() * 0.22f)
                },
                flakes = List(flakeCount) {
                    Flake(r.nextFloat(), r.nextFloat(), 4 + r.nextInt(5), 1.5f + r.nextFloat() * 2.5f, r.nextFloat(), 0.4f + r.nextFloat() * 0.5f)
                },
                stars = List(starCount) {
                    Star(r.nextFloat(), r.nextFloat() * 0.65f, 0.6f + r.nextFloat() * 1.4f, r.nextFloat(), 6 + r.nextInt(10))
                },
            )
        }
    }
}

private class Drop(val x: Float, val y: Float, val speed: Int, val length: Float, val alpha: Float)
private class Flake(val x: Float, val y: Float, val speed: Int, val radiusDp: Float, val phase: Float, val alpha: Float)
private class Star(val x: Float, val y: Float, val radiusDp: Float, val phase: Float, val twinkle: Int)

/** Bir bulut: yarı saydam, kenarları yumuşak "puf"lardan oluşur. */
private class CloudSpec(val x: Float, val y: Float, val width: Float, val speed: Int, val alpha: Float)

private val lightClouds = listOf(
    CloudSpec(0.10f, 0.08f, 0.60f, 1, 0.13f),
    CloudSpec(0.55f, 0.20f, 0.50f, 2, 0.10f),
    CloudSpec(0.30f, 0.34f, 0.65f, 1, 0.08f),
    CloudSpec(0.80f, 0.04f, 0.45f, 2, 0.12f),
)
private val overcastClouds = listOf(
    // Arka katman: büyük, silik, yavaş
    CloudSpec(0.05f, 0.04f, 0.85f, 1, 0.16f),
    CloudSpec(0.50f, 0.10f, 0.80f, 1, 0.14f),
    CloudSpec(0.85f, 0.22f, 0.75f, 1, 0.12f),
    // Orta katman
    CloudSpec(0.25f, 0.20f, 0.60f, 2, 0.20f),
    CloudSpec(0.70f, 0.34f, 0.55f, 2, 0.17f),
    CloudSpec(0.40f, 0.46f, 0.65f, 1, 0.12f),
    // Ön katman: küçük, belirgin, daha hızlı
    CloudSpec(0.15f, 0.30f, 0.40f, 3, 0.22f),
    CloudSpec(0.60f, 0.02f, 0.42f, 3, 0.24f),
    CloudSpec(0.90f, 0.40f, 0.38f, 3, 0.18f),
)
private val fewClouds = listOf(
    CloudSpec(0.20f, 0.14f, 0.50f, 1, 0.09f),
    CloudSpec(0.70f, 0.28f, 0.42f, 2, 0.07f),
)

private fun DrawScope.drawScene(kind: SceneKind, t: Float, p: Particles) {
    when (kind) {
        SceneKind.Sun -> drawSun(t)
        SceneKind.Moon -> {
            drawStars(t, p.stars)
            drawMoon()
        }
        SceneKind.PartlySunny -> {
            drawSun(t)
            drawClouds(fewClouds, t, Color.White)
        }
        SceneKind.PartlyMoon -> {
            drawStars(t, p.stars)
            drawMoon()
            drawClouds(fewClouds, t, Color(0xFFCFD8EA))
        }
        SceneKind.Clouds -> drawClouds(overcastClouds, t, Color.White)
        SceneKind.Fog -> drawFog(t)
        SceneKind.Rain -> {
            drawClouds(lightClouds, t, Color(0xFFC9D3E3))
            drawRain(t, p.drops)
        }
        SceneKind.Snow -> {
            drawClouds(fewClouds, t, Color.White)
            drawSnow(t, p.flakes)
        }
        SceneKind.Storm -> {
            drawClouds(lightClouds, t, Color(0xFF9AA3BF))
            drawRain(t, p.drops)
            drawLightning(t)
        }
    }
}

private fun wave(t: Float, cycles: Int, phase: Float = 0f): Float =
    sin(2 * PI * (t * cycles + phase)).toFloat()

/** Güneş: çizgi film ışınları yok; sadece sıcak, yumuşak bir ışık ve sakin bir nefes alma. */
private fun DrawScope.drawSun(t: Float) {
    val center = Offset(size.width * 0.86f, size.height * 0.06f)
    val r = size.minDimension * 0.09f
    val breathe = 1f + 0.04f * wave(t, 6)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x40FFD27A), Color(0x14FFC900), Color.Transparent),
            center = center,
            radius = r * 6f * breathe,
        ),
        radius = r * 6f * breathe,
        center = center,
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xCCFFF4D6), Color(0x66FFD98A), Color.Transparent),
            center = center,
            radius = r * 1.6f,
        ),
        radius = r * 1.6f,
        center = center,
    )
}

private fun DrawScope.drawMoon() {
    val center = Offset(size.width * 0.82f, size.height * 0.10f)
    val r = size.minDimension * 0.06f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x40DDE6FF), Color.Transparent),
            center = center,
            radius = r * 4f,
        ),
        radius = r * 4f,
        center = center,
    )
    // Hilal: büyük daireden kaydırılmış bir daire çıkarılır.
    val full = Path().apply { addOval(Rect(center, r)) }
    val cut = Path().apply {
        addOval(Rect(Offset(center.x + r * 0.45f, center.y - r * 0.25f), r * 0.95f))
    }
    val crescent = Path().apply { op(full, cut, PathOperation.Difference) }
    drawPath(crescent, color = Color(0xFFF1F4FF), alpha = 0.75f)
}

private fun DrawScope.drawStars(t: Float, stars: List<Star>) {
    stars.forEach { s ->
        val alpha = 0.25f + 0.6f * (0.5f + 0.5f * wave(t, s.twinkle, s.phase))
        drawCircle(
            color = Color.White,
            radius = s.radiusDp * density,
            center = Offset(s.x * size.width, s.y * size.height),
            alpha = alpha,
        )
    }
}

private fun DrawScope.drawClouds(clouds: List<CloudSpec>, t: Float, tint: Color) {
    clouds.forEach { c ->
        // Sağa doğru süzülür; ekranın dışından çıkıp öbür taraftan girer.
        val frac = (c.x + t * c.speed) % 1f
        val cx = (-0.5f + 2f * frac) * size.width
        drawSoftCloud(Offset(cx, c.y * size.height), c.width * size.width, tint, c.alpha)
    }
}

/** Puf'lar: içi dolu, kenarı saydamlaşan daireler. Üst üste binince yumuşak bir bulut olur. */
private fun DrawScope.drawSoftCloud(center: Offset, width: Float, tint: Color, alpha: Float) {
    val puffs = listOf(
        Triple(-0.30f, 0.06f, 0.26f),
        Triple(-0.08f, -0.08f, 0.32f),
        Triple(0.18f, -0.02f, 0.30f),
        Triple(0.36f, 0.08f, 0.22f),
        Triple(0.02f, 0.12f, 0.30f),
    )
    puffs.forEach { (dx, dy, rr) ->
        val pc = Offset(center.x + dx * width, center.y + dy * width)
        val radius = rr * width
        drawCircle(
            brush = Brush.radialGradient(
                0f to tint.copy(alpha = alpha),
                0.65f to tint.copy(alpha = alpha * 0.7f),
                1f to Color.Transparent,
                center = pc,
                radius = radius,
            ),
            radius = radius,
            center = pc,
        )
    }
}

private fun DrawScope.drawFog(t: Float) {
    repeat(5) { i ->
        val y = size.height * (0.15f + i * 0.17f)
        val bandHeight = size.height * 0.12f
        val alpha = 0.10f + 0.05f * wave(t, 2 + i, i * 0.2f)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = alpha), Color.Transparent),
                startY = y,
                endY = y + bandHeight,
            ),
            topLeft = Offset(0f, y),
            size = Size(size.width, bandHeight),
        )
    }
}

private fun DrawScope.drawRain(t: Float, drops: List<Drop>) {
    val stroke = 1.1f * density
    drops.forEach { d ->
        val y = ((d.y + t * d.speed) % 1f) * size.height * 1.1f - size.height * 0.05f
        val x = d.x * size.width
        val len = d.length * size.height
        drawLine(
            color = Color(0xFFDDE8FF),
            start = Offset(x, y),
            end = Offset(x - len * 0.22f, y + len),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
            alpha = d.alpha,
        )
    }
}

private fun DrawScope.drawSnow(t: Float, flakes: List<Flake>) {
    flakes.forEach { f ->
        val y = ((f.y + t * f.speed) % 1f) * size.height * 1.05f - size.height * 0.02f
        val sway = wave(t, f.speed * 2, f.phase) * 14f * density
        drawCircle(
            color = Color.White,
            radius = f.radiusDp * density,
            center = Offset(f.x * size.width + sway, y),
            alpha = f.alpha,
        )
    }
}

/** Döngü başına 5 şimşek: kısa bir parlama + zikzak bir yıldırım. */
private fun DrawScope.drawLightning(t: Float) {
    val flashes = 5
    val index = floor(t * flashes)
    val local = t * flashes - index
    if (local > 0.035f) return
    val strength = 1f - local / 0.035f
    drawRect(color = Color.White, alpha = 0.16f * strength)
    val startX = size.width * (0.25f + ((index * 37f) % 50f) / 100f)
    val bolt = Path().apply {
        moveTo(startX, size.height * 0.12f)
        lineTo(startX - size.width * 0.05f, size.height * 0.26f)
        lineTo(startX + size.width * 0.01f, size.height * 0.27f)
        lineTo(startX - size.width * 0.06f, size.height * 0.42f)
    }
    drawPath(
        path = bolt,
        color = Color(0xFFFFF6CC),
        alpha = 0.9f * strength,
        style = Stroke(width = 3f * density, cap = StrokeCap.Round),
    )
}
