package com.ahsen.hava.ui.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * ViewModel'den ekrana giden metin: metnin kendisi değil, strings.xml'deki kimliği (+ parametreleri).
 *
 * Neden? ViewModel Android'in Context'ine bağımlı olmasın (test edilebilir kalsın) ve metin cihazın
 * diline göre ekranda çözülsün. Parametre olarak başka bir [UiText] de verilebilir:
 * "%1$s şu an cevap vermiyor." + "Rota servisi" → "Rota servisi şu an cevap vermiyor."
 */
data class UiText(@param:StringRes val id: Int, val args: List<Any> = emptyList()) {

    constructor(@StringRes id: Int, vararg args: Any) : this(id, args.toList())

    @Composable
    fun asString(): String {
        val resolved = args.map { if (it is UiText) it.asString() else it }
        return stringResource(id, *resolved.toTypedArray())
    }

    /** Compose dışında (bildirim, widget) aynı metni çözmek için. */
    fun resolve(context: Context): String {
        val resolved = args.map { if (it is UiText) it.resolve(context) else it }
        return context.getString(id, *resolved.toTypedArray())
    }
}
