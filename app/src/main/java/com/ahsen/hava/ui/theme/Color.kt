package com.ahsen.hava.ui.theme

import androidx.compose.ui.graphics.Color

// Vurgu renkleri: sarı-lacivert
val HavaYellow = Color(0xFFFFC900)
val HavaNavy = Color(0xFF0B1F4D)
val HavaNavyDeep = Color(0xFF06122E)

// Cam kartlar: koyu zeminde ince, zarif bir cam. Az saydamlık = daha net görüntü.
val GlassFill = Color(0x14FFFFFF)   // %8 beyaz
val GlassBorder = Color(0x26FFFFFF) // %15 beyaz

// Metinler hep gradyan üstünde duracağı için beyaz tonları
val TextPrimary = Color.White
val TextSecondary = Color(0xA6FFFFFF) // %65 beyaz

// Vurgu dışı yardımcı renkler
val CoolBlue = Color(0xFF8FC8FF)  // yağmur, soğuk
val WarmOrange = Color(0xFFFFA463) // sıcak uç

// Ulaşım türlerinin vurgu renkleri: Yolculuk+ kartları bir bakışta ayırt edilsin.
val WalkMint = Color(0xFF6EE7B7)
val BikeCoral = Color(0xFFFF8A65)
val CarLavender = Color(0xFFB39DFF)
val TransitSky = Color(0xFF7FD1FF)

/**
 * Bütün kartların ortak zemini: %85 opak koyu lacivert. Gökyüzü hafifçe sezilir ama
 * metin her arka planda aynı netlikte okunur (tek kart sistemi).
 */
val CardSurface = Color(0xD916233D)