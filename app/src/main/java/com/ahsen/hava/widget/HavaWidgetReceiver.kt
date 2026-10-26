package com.ahsen.hava.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Sistemin widget'la konuştuğu kapı (manifest'te kayıtlı). İlk widget eklenince düzenli
 * güncellemeyi başlatır, son widget kaldırılınca durdurur: widget yoksa ağ trafiği de yok.
 */
class HavaWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = HavaWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefreshWorker.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefreshWorker.cancel(context)
    }
}
