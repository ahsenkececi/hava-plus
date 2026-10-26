package com.ahsen.hava.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ahsen.hava.ui.theme.HavaTheme
import com.ahsen.hava.ui.theme.Sky
import com.ahsen.hava.ui.theme.SkyBackground

/**
 * Yükleniyor durumu: gerçek kartlarla aynı ölçüde boş cam kartlar
 * ve üstlerinden kayan bir ışıltı.
 */
@Composable
fun ShimmerList(modifier: Modifier = Modifier, itemCount: Int = 6) {
    val brush = shimmerBrush()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(itemCount) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(brush, RoundedCornerShape(12.dp)),
                    )
                    Column(
                        modifier = Modifier.padding(start = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(16.dp)
                                .background(brush, RoundedCornerShape(4.dp)),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(12.dp)
                                .background(brush, RoundedCornerShape(4.dp)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun shimmerBrush(): Brush {
    val base = Color(0x1AFFFFFF)
    val highlight = Color(0x59FFFFFF)
    val translate by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = SHIMMER_DISTANCE,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translate - SHIMMER_WIDTH, 0f),
        end = Offset(translate, 0f),
    )
}

private const val SHIMMER_DISTANCE = 1_500f
private const val SHIMMER_WIDTH = 400f

@Preview
@Composable
private fun ShimmerListPreview() {
    HavaTheme { SkyBackground(sky = Sky.ClearNight) { ShimmerList() } }
}
