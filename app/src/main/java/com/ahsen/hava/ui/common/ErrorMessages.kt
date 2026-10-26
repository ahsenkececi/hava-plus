package com.ahsen.hava.ui.common

import androidx.annotation.StringRes
import com.ahsen.hava.R
import com.ahsen.hava.domain.error.DataError

/**
 * Hatanın kullanıcıya gösterilecek cümlesi. Bütün ekranlar aynı dili konuşsun diye tek yerde.
 * [service] verilirse sunucu hatası o servisin adıyla söylenir: "Rota servisi şu an cevap vermiyor."
 */
fun DataError.userMessage(@StringRes service: Int = R.string.service_generic): UiText = when (this) {
    is DataError.Network -> UiText(R.string.error_network)
    is DataError.Server -> UiText(R.string.error_server, UiText(service))
    is DataError.InvalidResponse -> UiText(R.string.error_unexpected)
}
