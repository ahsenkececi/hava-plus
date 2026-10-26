package com.ahsen.hava.ui.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahsen.hava.R
import com.ahsen.hava.domain.logic.DepartureAdvice
import com.ahsen.hava.ui.common.departureMessageText
import com.ahsen.hava.ui.common.departureOffsetText
import com.ahsen.hava.ui.common.label
import com.ahsen.hava.ui.component.GlassCard
import com.ahsen.hava.ui.theme.CoolBlue
import com.ahsen.hava.ui.theme.GlassBorder
import com.ahsen.hava.ui.theme.GlassFill
import com.ahsen.hava.ui.theme.HavaYellow
import com.ahsen.hava.ui.theme.TextPrimary
import com.ahsen.hava.ui.theme.TextSecondary
import java.time.format.DateTimeFormatter

/**
 * "Ne zaman çıkmalıyım?" kartı: öneri cümlesi + 7 çıkış saatlik zaman şeridi.
 * Her kutu o saatte çıkarsan yağmurda kaç dakika kalacağını gösterir. Dokununca bütün
 * seçenekler o çıkış saatine göre yeniden hesaplanır.
 */
@Composable
internal fun DepartureCard(advice: DepartureAdvice, selectedOffset: Int, onSelect: (Int) -> Unit) {
    val clock = remember { DateTimeFormatter.ofPattern("HH:mm") }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.departure_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = HavaYellow,
                fontWeight = FontWeight.Bold,
            )
            TravelModeIcon(mode = advice.mode, size = 18.dp, color = TextSecondary)
            Text(advice.mode.label(), modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
        Spacer(Modifier.height(8.dp))
        Text(departureMessageText(advice.message), style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        val maxWet = advice.slots.maxOf { it.wetMinutes }.coerceAtLeast(1)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            advice.slots.forEach { slot ->
                val selected = slot.offsetMinutes == selectedOffset
                val isBest = slot.offsetMinutes == advice.bestOffsetMinutes
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(slot.offsetMinutes) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (slot.wetMinutes == 0) GlassFill else CoolBlue.copy(alpha = 0.35f + 0.65f * slot.wetMinutes / maxWet),
                            )
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) HavaYellow else GlassBorder,
                                shape = RoundedCornerShape(10.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (slot.wetMinutes == 0) stringResource(R.string.slot_dry) else stringResource(R.string.slot_wet_minutes, slot.wetMinutes),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal,
                            color = TextPrimary,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = departureOffsetText(slot.offsetMinutes),
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) HavaYellow else TextPrimary,
                        maxLines = 1,
                    )
                    Text(
                        text = slot.time.format(clock),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                    )
                    // En iyi çıkış: altında küçük sarı nokta
                    Box(
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isBest) HavaYellow else Color.Transparent),
                    )
                }
            }
        }
    }
}
