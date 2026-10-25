package com.example.data.remote

import com.example.data.local.RaceWeatherEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Météo réelle d'une course, au point de départ publié par MyChrono, via Open-Meteo (gratuit, sans clé).
 * - Course de plus de 6 jours : réanalyse (archive-api.open-meteo.com).
 * - Course récente : modèle de prévision sur les jours passés (api.open-meteo.com).
 * Aucune valeur n'est devinée : sans heure de départ publiée, seules les valeurs de la journée sont gardées.
 */
class OpenMeteoClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val ARCHIVE_URL = "https://archive-api.open-meteo.com/v1/archive"
        private const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
        const val SOURCE_ARCHIVE = "Open-Meteo (réanalyse)"
        const val SOURCE_FORECAST = "Open-Meteo (modèle)"

        /** Heure de départ lue dans l'horaire publié (« 10h00 – 4,8 et 10 km » → 10), sinon null. */
        fun parseStartHour(schedule: String?): Int? {
            if (schedule.isNullOrBlank()) return null
            val m = Regex("(\\d{1,2})\\s*[hH:]\\s*(\\d{2})?").find(schedule) ?: return null
            return m.groupValues[1].toIntOrNull()?.takeIf { it in 5..22 }
        }

        /** Libellé court d'un code météo OMM. */
        fun weatherLabel(code: Int?): String? = when (code) {
            null -> null
            0 -> "Ciel clair"
            1, 2 -> "Éclaircies"
            3 -> "Couvert"
            45, 48 -> "Brouillard"
            51, 53, 55, 56, 57 -> "Bruine"
            61, 63, 65, 66, 67 -> "Pluie"
            71, 73, 75, 77, 85, 86 -> "Neige"
            80, 81, 82 -> "Averses"
            95, 96, 99 -> "Orage"
            else -> null
        }

        private fun JSONArray.doubleAt(i: Int): Double? =
            if (i < 0 || i >= length() || isNull(i)) null else optDouble(i).takeIf { !it.isNaN() }

        private fun round1(v: Double?): Double? = v?.let { (it * 10.0).roundToInt() / 10.0 }

        /**
         * Lit une réponse horaire Open-Meteo (temperature_2m, precipitation, wind_speed_10m, weather_code)
         * couvrant les 3 jours avant la course et le jour de la course.
         * Renvoie null si la réponse ne contient aucune valeur exploitable pour le jour de la course.
         */
        fun parseWeather(
            jsonText: String,
            raceId: String,
            raceDateIso: String,
            startHour: Int?,
            source: String,
            fetchedAtEpochMillis: Long
        ): RaceWeatherEntity? {
            val root = runCatching { JSONObject(jsonText) }.getOrNull() ?: return null
            val hourly = root.optJSONObject("hourly") ?: return null
            val time = hourly.optJSONArray("time") ?: return null
            val temp = hourly.optJSONArray("temperature_2m") ?: JSONArray()
            val precip = hourly.optJSONArray("precipitation") ?: JSONArray()
            val wind = hourly.optJSONArray("wind_speed_10m") ?: JSONArray()
            val code = hourly.optJSONArray("weather_code") ?: JSONArray()

            val times = (0 until time.length()).map { time.optString(it) }
            val dayIdx = times.indices.filter { times[it].startsWith(raceDateIso) }
            if (dayIdx.isEmpty()) return null
            val dayTemps = dayIdx.mapNotNull { temp.doubleAt(it) }
            if (dayTemps.isEmpty()) return null

            val startIdx = startHour?.let { h -> times.indexOf("%sT%02d:00".format(raceDateIso, h)).takeIf { it >= 0 } }
            val before72 = if (startIdx != null) {
                (maxOf(0, startIdx - 72) until startIdx).mapNotNull { precip.doubleAt(it) }
            } else {
                times.indices.filter { times[it] < raceDateIso }.mapNotNull { precip.doubleAt(it) }
            }

            return RaceWeatherEntity(
                raceId = raceId,
                startHourLocal = startIdx?.let { startHour },
                temperatureC = startIdx?.let { round1(temp.doubleAt(it)) },
                precipitationDuringMm = startIdx?.let { i ->
                    listOfNotNull(precip.doubleAt(i), precip.doubleAt(i + 1)).takeIf { it.isNotEmpty() }?.sum()
                }?.let { round1(it) },
                rainPrevious72hMm = before72.takeIf { it.isNotEmpty() }?.sum()?.let { round1(it) },
                windKmh = startIdx?.let { round1(wind.doubleAt(it)) },
                weatherCode = startIdx?.let { i -> code.doubleAt(i)?.roundToInt() },
                dayMinC = round1(dayTemps.minOrNull()),
                dayMaxC = round1(dayTemps.maxOrNull()),
                source = source,
                fetchedAtEpochMillis = fetchedAtEpochMillis
            )
        }
    }

    private fun get(url: String): String? =
        httpClient.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        }

    private fun url(base: String, lat: Double, lon: Double, date: LocalDate): String =
        "$base?latitude=${"%.4f".format(Locale.ROOT, lat)}&longitude=${"%.4f".format(Locale.ROOT, lon)}" +
            "&start_date=${date.minusDays(3)}&end_date=$date" +
            "&hourly=temperature_2m,precipitation,wind_speed_10m,weather_code&timezone=Europe%2FBrussels"

    suspend fun fetchRaceWeather(
        raceId: String,
        latitude: Double,
        longitude: Double,
        raceDateIso: String,
        startHour: Int?,
        today: LocalDate = LocalDate.now()
    ): Result<RaceWeatherEntity?> = withContext(Dispatchers.IO) {
        runCatching {
            val date = LocalDate.parse(raceDateIso)
            val now = System.currentTimeMillis()
            val attempts = if (date.isBefore(today.minusDays(6))) {
                listOf(ARCHIVE_URL to SOURCE_ARCHIVE, FORECAST_URL to SOURCE_FORECAST)
            } else {
                listOf(FORECAST_URL to SOURCE_FORECAST)
            }
            attempts.firstNotNullOfOrNull { (base, source) ->
                get(url(base, latitude, longitude, date))
                    ?.let { parseWeather(it, raceId, raceDateIso, startHour, source, now) }
            }
        }
    }
}
