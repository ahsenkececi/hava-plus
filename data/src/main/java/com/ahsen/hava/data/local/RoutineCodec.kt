package com.ahsen.hava.data.local

import com.ahsen.hava.domain.model.Place
import com.ahsen.hava.domain.model.Routine
import com.ahsen.hava.domain.model.TravelMode
import com.google.gson.Gson
import com.google.gson.JsonParseException
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeParseException

/**
 * Rutin <-> JSON. Android'e bağımlı değil: DataStore olmadan test edilebilir.
 *
 * Enum'lar ve saat metin olarak yazılır ("Bike", "MONDAY", "08:15"): okunur ve sınıf
 * yeniden adlandırılsa bile kayıt bozulmaz. Okunamayan kayıt çökme yerine null döner.
 */
internal class RoutineCodec(private val gson: Gson) {

    fun encode(routine: Routine): String = gson.toJson(StoredRoutine.from(routine))

    fun decode(json: String?): Routine? {
        if (json.isNullOrBlank()) return null
        return try {
            gson.fromJson(json, StoredRoutine::class.java)?.toRoutine()
        } catch (e: JsonParseException) {
            null
        } catch (e: IllegalArgumentException) {
            null // bilinmeyen enum adı
        } catch (e: DateTimeParseException) {
            null
        }
    }
}

private data class StoredPlace(val name: String?, val detail: String?, val latitude: Double?, val longitude: Double?) {
    fun toPlace(): Place? {
        return Place(
            name = name ?: return null,
            detail = detail.orEmpty(),
            latitude = latitude ?: return null,
            longitude = longitude ?: return null,
        )
    }

    companion object {
        fun from(place: Place) = StoredPlace(place.name, place.detail, place.latitude, place.longitude)
    }
}

private data class StoredRoutine(
    val from: StoredPlace?,
    val to: StoredPlace?,
    val mode: String?,
    val days: List<String>?,
    val departure: String?,
    val enabled: Boolean?,
) {
    fun toRoutine(): Routine? {
        return Routine(
            from = from?.toPlace() ?: return null,
            to = to?.toPlace() ?: return null,
            mode = TravelMode.valueOf(mode ?: return null),
            days = days.orEmpty().map(DayOfWeek::valueOf).toSet(),
            departure = LocalTime.parse(departure ?: return null),
            enabled = enabled ?: true,
        )
    }

    companion object {
        fun from(routine: Routine) = StoredRoutine(
            from = StoredPlace.from(routine.from),
            to = StoredPlace.from(routine.to),
            mode = routine.mode.name,
            days = routine.days.sorted().map { it.name },
            departure = routine.departure.toString(),
            enabled = routine.enabled,
        )
    }
}
