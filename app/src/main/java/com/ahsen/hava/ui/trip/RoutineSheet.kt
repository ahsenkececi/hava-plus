package com.ahsen.hava.ui.trip

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahsen.hava.R
import com.ahsen.hava.domain.model.Routine
import com.ahsen.hava.ui.common.label
import com.ahsen.hava.ui.theme.GlassBorder
import com.ahsen.hava.ui.theme.GlassFill
import com.ahsen.hava.ui.theme.HavaNavy
import com.ahsen.hava.ui.theme.HavaYellow
import com.ahsen.hava.ui.theme.TextPrimary
import com.ahsen.hava.ui.theme.TextSecondary
import com.ahsen.hava.ui.theme.WarmOrange
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Rutin kurma/düzenleme sayfası: ulaşım türü, günler ve çıkış saati.
 * Seçimler sayfanın içinde tutulur; "Kaydet"e basılınca tek seferde ViewModel'e gider.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoutineSheet(
    editor: RoutineEditor,
    onSave: (Routine) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val initial = editor.initial
    var mode by remember(initial) { mutableStateOf(initial.mode) }
    var days by remember(initial) { mutableStateOf(initial.days) }
    val time = rememberTimePickerState(initialHour = initial.departure.hour, initialMinute = initial.departure.minute, is24Hour = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = HavaNavy,
        contentColor = TextPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(stringResource(R.string.routine_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.routine_route, initial.from.name, initial.to.name),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            SectionLabel(R.string.routine_mode_question)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                editor.modes.forEach { option ->
                    FilterChip(
                        selected = option == mode,
                        onClick = { mode = option },
                        label = { Text(option.label()) },
                        leadingIcon = { TravelModeIcon(option, size = 18.dp, color = if (option == mode) HavaNavy else TextPrimary) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HavaYellow,
                            selectedLabelColor = HavaNavy,
                            labelColor = TextPrimary,
                        ),
                    )
                }
            }

            SectionLabel(R.string.routine_days_question)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DayOfWeek.entries.forEach { day ->
                    DayToggle(
                        day = day,
                        selected = day in days,
                        onToggle = { days = if (day in days) days - day else days + day },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            SectionLabel(R.string.routine_time_question)
            TimeInput(state = time)

            Text(
                stringResource(R.string.routine_explainer),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSave(initial.copy(mode = mode, days = days, departure = LocalTime.of(time.hour, time.minute))) },
                enabled = days.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = HavaYellow, contentColor = HavaNavy),
            ) {
                Text(stringResource(R.string.routine_save), fontWeight = FontWeight.Bold)
            }
            if (editor.isExisting) {
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.routine_delete), color = WarmOrange)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(@StringRes id: Int) {
    Text(
        stringResource(id),
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = TextSecondary,
    )
}

/** Haftanın bir günü: yuvarlak, seçiliyse sarı. */
@Composable
private fun DayToggle(day: DayOfWeek, selected: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(if (selected) HavaYellow else GlassFill)
            .border(1.dp, if (selected) HavaYellow else GlassBorder, CircleShape)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() }),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(day.shortLabelRes()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) HavaNavy else TextPrimary,
        )
    }
}
