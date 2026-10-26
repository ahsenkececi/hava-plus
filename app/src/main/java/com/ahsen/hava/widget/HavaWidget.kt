package com.ahsen.hava.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ahsen.hava.AppActions
import com.ahsen.hava.MainActivity
import com.ahsen.hava.R
import com.ahsen.hava.di.AppContainer
import com.ahsen.hava.domain.error.DataError
import com.ahsen.hava.ui.theme.GlassFill
import com.ahsen.hava.ui.theme.HavaYellow
import com.ahsen.hava.ui.theme.Sky
import com.ahsen.hava.ui.theme.TextPrimary
import com.ahsen.hava.ui.theme.TextSecondary
import kotlin.math.roundToInt

/**
 * Ana ekran widget'ı (4×2): solda hava, sağda saatlik eğri ve Yolculuk+ satırı.
 *
 * Jetpack Glance ile yazıldı: Compose'a benzer ama sonuç RemoteViews'a çevrilir. Bu yüzden
 * Canvas yerine Bitmap ([skyBitmap], [hourlyCurveBitmap]) ve stringResource yerine Context kullanılır.
 * İş kuralları domain'de; burası sadece gösterim.
 */
class HavaWidget : GlanceAppWidget() {

    /** Widget'ın gerçek boyutu: bitmap'ler tam o ölçüde çizilir, bulanıklaşmaz. */
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadState()
        provideContent { WidgetContent(state) }
    }

    private suspend fun loadState(): WidgetState = try {
        val weather = AppContainer.getWidgetWeather()
        // Rutin satırı alınamazsa widget yine de havayı göstersin.
        val upcoming = try {
            AppContainer.checkUpcomingRoutine()
        } catch (e: DataError) {
            Log.w(TAG, "Rutin değerlendirilemedi", e)
            null
        }
        widgetState(weather, upcoming)
    } catch (e: DataError) {
        Log.w(TAG, "Widget havası alınamadı", e)
        WidgetState.Unavailable
    }

    private companion object {
        const val TAG = "HavaWidget"
    }
}

private val PADDING = 14.dp
private val TRIP_BOX_HEIGHT = 58.dp

/** Bitmap'ler en fazla 2x çözünürlükte: widget'a gönderilebilecek bellek sınırlı. */
private const val MAX_BITMAP_SCALE = 2f

@Composable
private fun WidgetContent(state: WidgetState) {
    val context = LocalContext.current
    val size = LocalSize.current
    val scale = context.resources.displayMetrics.density.coerceAtMost(MAX_BITMAP_SCALE)
    val sky = (state as? WidgetState.Ready)?.sky ?: Sky.ClearNight

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(ImageProvider(skyBitmap(sky, size.width.px(scale), size.height.px(scale), scale)))
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        when (state) {
            is WidgetState.Ready -> ReadyContent(state, scale)
            WidgetState.Unavailable -> UnavailableContent()
        }
    }
}

@Composable
private fun ReadyContent(state: WidgetState.Ready, scale: Float) {
    val context = LocalContext.current
    val size = LocalSize.current
    val leftWidth = size.width * 0.38f
    val rightWidth = size.width - leftWidth - PADDING * 2
    val curveHeight = (size.height - PADDING * 2 - TRIP_BOX_HEIGHT - 6.dp).coerceAtLeast(36.dp)

    Row(modifier = GlanceModifier.fillMaxSize().padding(PADDING)) {
        Column(modifier = GlanceModifier.width(leftWidth).fillMaxHeight()) {
            Text(state.cityName, style = text(14, FontWeight.Medium), maxLines = 1)
            Text(
                context.getString(R.string.temperature_value, state.temperatureC),
                style = text(42, FontWeight.Normal),
                maxLines = 1,
            )
            Text(state.condition.resolve(context), style = text(13), maxLines = 1)
            state.highLow?.let { Text(it.resolve(context), style = text(12, color = TextSecondary), maxLines = 1) }
        }
        Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
            Image(
                provider = ImageProvider(
                    hourlyCurveBitmap(
                        hours = state.hours,
                        nowLabel = context.getString(R.string.now),
                        widthPx = rightWidth.px(scale),
                        heightPx = curveHeight.px(scale),
                        density = scale,
                    ),
                ),
                contentDescription = context.getString(R.string.widget_curve_cd),
                modifier = GlanceModifier.fillMaxWidth().height(curveHeight),
                contentScale = ContentScale.FillBounds,
            )
            Spacer(GlanceModifier.height(6.dp))
            TripBox(state.tripLine)
        }
    }
}

/** "Yolculuk+" satırı. Dokununca Yolculuk+ (rutin satırıysa rutin rotasıyla) açılır. */
@Composable
private fun TripBox(line: WidgetTripLine) {
    val context = LocalContext.current
    val intent = Intent(context, MainActivity::class.java)
        .setAction(if (line.opensRoutine) AppActions.OPEN_ROUTINE else AppActions.OPEN_TRIP)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(TRIP_BOX_HEIGHT)
            .cornerRadius(12.dp)
            .background(ColorProvider(GlassFill))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clickable(actionStartActivity(intent)),
    ) {
        Text(context.getString(R.string.widget_trip_label), style = text(12, FontWeight.Bold, HavaYellow), maxLines = 1)
        Text(line.text.resolve(context), style = text(12), maxLines = 2)
    }
}

@Composable
private fun UnavailableContent() {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(context.getString(R.string.widget_title), style = text(16, FontWeight.Bold, HavaYellow))
        Text(context.getString(R.string.widget_unavailable), style = text(13, color = TextSecondary))
    }
}

private fun text(sizeSp: Int, weight: FontWeight = FontWeight.Normal, color: Color = TextPrimary) =
    TextStyle(color = ColorProvider(color), fontSize = sizeSp.sp, fontWeight = weight)

private fun Dp.px(scale: Float): Int = (value * scale).roundToInt()
