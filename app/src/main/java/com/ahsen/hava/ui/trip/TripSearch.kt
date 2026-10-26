package com.ahsen.hava.ui.trip

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahsen.hava.R
import com.ahsen.hava.domain.model.Place
import com.ahsen.hava.ui.BottomBarSpace
import com.ahsen.hava.ui.LoadState
import com.ahsen.hava.ui.component.GlassCard
import com.ahsen.hava.ui.theme.GlassBorder
import com.ahsen.hava.ui.theme.GlassFill
import com.ahsen.hava.ui.theme.HavaYellow
import com.ahsen.hava.ui.theme.TextPrimary
import com.ahsen.hava.ui.theme.TextSecondary

/*
 * Yolculuk+ üst kısmı: nereden–nereye kartı ve adres arama sonuçları.
 */

/**
 * Nereden–nereye kartı. İki satır arasında kesikli bir çizgi: haritadaki rota gibi.
 * Dokunulan satır yazılabilir hale gelir.
 */
@Composable
internal fun RouteCard(
    originText: String,
    originIsMyLocation: Boolean,
    destinationText: String?,
    activeField: TripField?,
    query: String,
    onFieldClick: (TripField) -> Unit,
    onQueryChange: (String) -> Unit,
    onSwap: () -> Unit,
    canSwap: Boolean,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                FieldRow(
                    dotColor = if (originIsMyLocation) HavaYellow else TextPrimary,
                    label = stringResource(R.string.trip_from),
                    text = originText,
                    placeholder = stringResource(R.string.my_location),
                    isActive = activeField == TripField.Origin,
                    query = query,
                    onClick = { onFieldClick(TripField.Origin) },
                    onQueryChange = onQueryChange,
                )
                // Noktaları birleştiren kesikli çizgi
                Canvas(
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .width(2.dp)
                        .height(18.dp),
                ) {
                    drawLine(
                        color = TextSecondary,
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                    )
                }
                FieldRow(
                    dotColor = Color(0xFFFF8A65),
                    label = stringResource(R.string.trip_to),
                    text = destinationText,
                    placeholder = stringResource(R.string.trip_to_hint),
                    isActive = activeField == TripField.Destination,
                    query = query,
                    onClick = { onFieldClick(TripField.Destination) },
                    onQueryChange = onQueryChange,
                )
            }
            IconButton(onClick = onSwap, enabled = canSwap) {
                // Yer değiştir (gidiş ↔ dönüş). Çekirdek ikon setinde ⇅ olmadığı için Refresh kullanıyoruz.
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.cd_swap),
                    tint = if (canSwap) TextPrimary else TextSecondary.copy(alpha = 0.4f),
                )
            }
        }
    }
}

@Composable
private fun FieldRow(
    dotColor: Color,
    label: String,
    text: String?,
    placeholder: String,
    isActive: Boolean,
    query: String,
    onClick: () -> Unit,
    onQueryChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isActive) { if (isActive) focusRequester.requestFocus() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) GlassFill else Color.Transparent)
            .border(1.dp, if (isActive) HavaYellow.copy(alpha = 0.7f) else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(enabled = !isActive, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            if (isActive) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                    cursorBrush = SolidColor(HavaYellow),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) {
                                Text(text ?: placeholder, color = TextSecondary, fontSize = 17.sp)
                            }
                            inner()
                        }
                    },
                )
            } else {
                Text(
                    text = text ?: placeholder,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (text != null) TextPrimary else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun SearchResults(
    field: TripField,
    results: LoadState<List<Place>>?,
    query: String,
    onPlaceSelected: (Place) -> Unit,
    onUseMyLocation: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = BottomBarSpace),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (field == TripField.Origin) {
            item(key = "my-location") {
                ResultRow(
                    title = stringResource(R.string.my_location),
                    detail = stringResource(R.string.my_location_detail),
                    highlight = true,
                    onClick = onUseMyLocation,
                )
            }
        }
        when (results) {
            null -> item(key = "hint") {
                Text(
                    text = stringResource(if (query.isEmpty()) R.string.place_search_hint else R.string.place_search_min_chars),
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
            LoadState.Loading -> item(key = "loading") {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = HavaYellow, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                }
            }
            LoadState.Empty -> item(key = "empty") {
                Text(stringResource(R.string.place_not_found), modifier = Modifier.padding(8.dp), color = TextSecondary)
            }
            is LoadState.Error -> item(key = "error") {
                Text(results.message.asString(), modifier = Modifier.padding(8.dp), color = TextPrimary)
            }
            is LoadState.Success -> items(results.data, key = { "${it.latitude},${it.longitude},${it.name}" }) { place ->
                ResultRow(title = place.name, detail = place.detail, highlight = false, onClick = { onPlaceSelected(place) })
            }
        }
    }
}

@Composable
private fun ResultRow(title: String, detail: String, highlight: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassFill)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (highlight) Icons.Filled.LocationOn else Icons.Filled.Place,
            contentDescription = null,
            tint = if (highlight) HavaYellow else TextSecondary,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detail.isNotBlank()) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
