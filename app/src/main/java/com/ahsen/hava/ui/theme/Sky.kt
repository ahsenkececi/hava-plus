package com.ahsen.hava.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Her gökyüzü durumu için yukarıdan aşağıya 3 renk.
// Hepsi koyu ve doygunluğu düşük: içerik öne çıksın, renk sadece havayı hissettirsin.
enum class Sky(val colors: List<Color>) {
    ClearDay(listOf(Color(0xFF0E2A5A), Color(0xFF17427F), Color(0xFF235A9E))),
    ClearNight(listOf(HavaNavyDeep, HavaNavy, Color(0xFF152B5E))),
    Cloudy(listOf(Color(0xFF41557A), Color(0xFF6479A0), Color(0xFF8C9DBD))),
    // Gece kapalı/sisli: gündüzün açık grisi gece yanlış görünüyordu; koyu, soğuk bir arduvaz.
    CloudyNight(listOf(Color(0xFF141C2C), Color(0xFF212C42), Color(0xFF313E58))),
    Rainy(listOf(Color(0xFF1B283C), Color(0xFF2A3C56), Color(0xFF3B5170))),
    Storm(listOf(Color(0xFF15152C), Color(0xFF262444), Color(0xFF38315C))),
    Snow(listOf(Color(0xFF2B3D5A), Color(0xFF41587A), Color(0xFF5B749A))),
}

@Composable
fun SkyBackground(
    sky: Sky,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    // Gökyüzü değişince renkler 800 ms içinde yumuşakça geçiş yapar
    val top by animateColorAsState(sky.colors[0], tween(800), label = "top")
    val mid by animateColorAsState(sky.colors[1], tween(800), label = "mid")
    val bottom by animateColorAsState(sky.colors[2], tween(800), label = "bottom")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(top, mid, bottom))),
        content = content,
    )
}