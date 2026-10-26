package com.ahsen.hava

/**
 * Uygulamayı dışarıdan (bildirim, widget) belirli bir yere açan eylemler.
 * [MainActivity] bunları okuyup ilgili sekmeye geçer.
 */
object AppActions {
    /** Yolculuk+ sekmesini açar. */
    const val OPEN_TRIP = "com.ahsen.hava.action.OPEN_TRIP"

    /** Yolculuk+ sekmesini açar ve rutin rotasını doldurur. */
    const val OPEN_ROUTINE = "com.ahsen.hava.action.OPEN_ROUTINE"
}
