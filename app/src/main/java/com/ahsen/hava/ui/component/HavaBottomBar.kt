package com.ahsen.hava.ui.component

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahsen.hava.R
import com.ahsen.hava.domain.model.WeatherCondition
import com.ahsen.hava.ui.theme.GlassBorder
import com.ahsen.hava.ui.theme.HavaNavyDeep
import com.ahsen.hava.ui.theme.HavaYellow
import com.ahsen.hava.ui.theme.TextSecondary

/** Alt menüdeki sekmeler. */
enum class HavaTab(@param:StringRes val label: Int) { Weather(R.string.tab_weather), Trip(R.string.tab_trip) }

/**
 * Yüzen, cam görünümlü alt menü. Düz bir çubuk yerine ortada duran bir kapsül:
 * gökyüzü arka planı ekranın en altına kadar kesintisiz devam etsin.
 */
@Composable
fun HavaBottomBar(selected: HavaTab, onSelect: (HavaTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(HavaNavyDeep.copy(alpha = 0.88f))
            .border(1.dp, GlassBorder, RoundedCornerShape(28.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HavaTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val background by animateColorAsState(if (isSelected) HavaYellow.copy(alpha = 0.16f) else Color.Transparent, label = "tabBg")
            val tint = if (isSelected) HavaYellow else TextSecondary
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(background)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 22.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (tab) {
                    HavaTab.Trip -> RouteGlyph(color = tint)
                    HavaTab.Weather -> WeatherIcon(condition = WeatherCondition.PARTLY_CLOUDY, isDay = true, size = 24.dp)
                }
                Text(
                    text = stringResource(tab.label),
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = tint,
                )
            }
        }
    }
}

/** Yolculuk ikonu: iki nokta ve aralarında kıvrılan kesikli bir yol. */
@Composable
private fun RouteGlyph(color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val s = size.minDimension
        val path = Path().apply {
            moveTo(s * 0.22f, s * 0.80f)
            cubicTo(s * 0.22f, s * 0.40f, s * 0.78f, s * 0.60f, s * 0.78f, s * 0.22f)
        }
        drawPath(
            path,
            color,
            style = Stroke(
                width = s * 0.08f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(s * 0.12f, s * 0.1f)),
            ),
        )
        drawCircle(color, radius = s * 0.11f, center = Offset(s * 0.22f, s * 0.80f))
        drawCircle(color, radius = s * 0.11f, center = Offset(s * 0.78f, s * 0.22f))
    }
}
