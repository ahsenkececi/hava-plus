package com.ahsen.hava.ui.trip

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ahsen.hava.R
import com.ahsen.hava.domain.model.Place
import com.ahsen.hava.domain.model.Routine
import com.ahsen.hava.domain.model.TravelMode
import com.ahsen.hava.ui.component.GlassCard
import com.ahsen.hava.ui.theme.HavaNavy
import com.ahsen.hava.ui.theme.HavaTheme
import com.ahsen.hava.ui.theme.HavaYellow
import com.ahsen.hava.ui.theme.TextPrimary
import com.ahsen.hava.ui.theme.TextSecondary
import java.time.LocalTime

/**
 * Kayıtlı rutin: dokununca yolculuğu açar, kalemle düzenlenir, anahtarla bildirimleri açılır/kapanır.
 */
@Composable
internal fun RoutineCard(
    routine: Routine,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = null,
                tint = if (routine.enabled) HavaYellow else TextSecondary,
                modifier = Modifier.size(22.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    stringResource(R.string.routine_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = HavaYellow,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.routine_route, routine.from.name, routine.to.name),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(routineScheduleText(routine), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.cd_routine_edit), tint = TextSecondary)
            }
            Switch(
                checked = routine.enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(checkedThumbColor = HavaNavy, checkedTrackColor = HavaYellow),
            )
        }
    }
}

/** Rota kutusunun altında: "Bu yolculuğu rutinim yap". */
@Composable
internal fun SaveRoutineCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Notifications, contentDescription = null, tint = HavaYellow, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    stringResource(R.string.routine_save_trip),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Text(stringResource(R.string.routine_save_trip_hint), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Preview
@Composable
private fun RoutineCardPreview() {
    HavaTheme {
        RoutineCard(
            routine = Routine(
                from = Place("Ev", "Bornova", 38.46, 27.21),
                to = Place("Okul", "Çiğli", 38.49, 27.06),
                mode = TravelMode.Bike,
                days = Routine.WEEKDAYS,
                departure = LocalTime.of(8, 15),
            ),
            onOpen = {},
            onEdit = {},
            onEnabledChange = {},
        )
    }
}
