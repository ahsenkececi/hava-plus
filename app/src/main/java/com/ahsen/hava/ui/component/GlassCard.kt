package com.ahsen.hava.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ahsen.hava.ui.theme.GlassBorder
import com.ahsen.hava.ui.theme.GlassFill

/**
 * Uygulamadaki bütün kartların ortak "cam" görünümü.
 *
 * @param borderColor Seçili kartta sarı kenarlık için. Renk değişimi animasyonludur.
 * @param onClick null ise kart tıklanamaz.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = GlassBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val animatedBorder by animateColorAsState(borderColor, label = "glassBorder")
    Column(
        modifier = modifier
            .clip(shape)
            .background(GlassFill)
            .border(1.5.dp, animatedBorder, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content,
    )
}
