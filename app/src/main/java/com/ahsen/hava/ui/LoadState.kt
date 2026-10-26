package com.ahsen.hava.ui

import com.ahsen.hava.ui.common.UiText

/**
 * Ekranın bulunabileceği dört durum. `sealed` olduğu için `when` içinde
 * bir durumu unutursak Kotlin derleme hatası verir.
 */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Success<T>(val data: T) : LoadState<T>
    data object Empty : LoadState<Nothing>
    /** [message]: strings.xml'den çözülecek metin (bkz. [UiText]). */
    data class Error(val message: UiText) : LoadState<Nothing>
}
