package com.example

import com.example.data.local.RaceEntity
import com.example.data.remote.MyChronoRawResultRow
import com.example.data.remote.OpenMeteoClient
import com.example.domain.CourseDifficultyCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CourseDifficultyTest {

    private fun row(runner: Int, km: Double, seconds: Int, label: String = "10 km") = MyChronoRawResultRow(
        runnerId = runner.toString(), bibNumber = "", fullName = "R$runner", club = "", category = "S", sex = "M",
        distanceLabel = label, distanceKm = km, timeSeconds = seconds, timeFormatted = "", overallRank = runner,
        totalFinishers = 20, categoryRank = 1, categoryTotal = 1, points = 1000
    )

    /**
     * 12 coureurs réguliers d'allures différentes, 6 parcours de difficulté connue.
     * L'indice retrouvé doit être la difficulté du parcours, rapportée au parcours médian.
     */
    @Test
    fun factors_recoverCourseDifficulty_whateverTheRunnersLevel() {
        val difficulty = mapOf("r1" to 1.00, "r2" to 1.10, "r3" to 0.95, "r4" to 1.20, "r5" to 1.00, "r6" to 1.05)
        val basePaces = (1..12).associateWith { 240.0 + it * 15 } // de 4:15 à 7:00 /km
        val rows = difficulty.mapValues { (_, d) ->
            basePaces.map { (runner, pace) -> row(runner, 10.0, (pace * d * 10.0).roundToInt()) }
        }

        val factors = CourseDifficultyCalculator.computeSeasonFactors(rows, difficulty.keys)
            .filter { it.distanceLabel == "10 km" }
            .associate { it.raceId to it.factor }

        // Parcours médian des 6 : entre 1,00 et 1,05 → 1,025
        val median = 1.025
        difficulty.forEach { (raceId, d) ->
            assertEquals("indice de $raceId", d / median, factors.getValue(raceId), 0.01)
        }
    }

    @Test
    fun factors_needEnoughRegulars() {
        // 3 coureurs seulement : pas assez de réguliers pour un indice fiable.
        val rows = (1..6).associate { r -> "r$r" to (1..3).map { row(it, 10.0, 2400 + it * 60) } }
        assertTrue(CourseDifficultyCalculator.computeSeasonFactors(rows, rows.keys).isEmpty())
    }

    @Test
    fun courseKeys_matchTheSameCourseFromOneSeasonToTheNext() {
        fun race(id: String, year: Int, num: Int, place: String, date: String) = RaceEntity(
            raceId = id, seasonYear = year, roundNumber = num, myChronoEventId = id, title = place, village = place,
            dateIso = date, distanceKm = 10.0, elevationGainMeters = null, isCompleted = true
        )
        val races = listOf(
            race("a1", 2025, 1, "Virton", "2025-01-26"),
            race("a29", 2025, 29, "Rabais (Virton)", "2025-07-27"),
            race("a41", 2025, 41, "Virton", "2025-10-19"),
            race("a13", 2025, 13, "Freylange", "2025-04-20"),
            race("b1", 2026, 1, "Virton", "2026-01-25"),
            race("b29", 2026, 29, "Rabais (Virton)", "2026-07-26"),
            race("b41", 2026, 41, "Virton", "2026-10-18"),
            race("b11", 2026, 11, "Freylange", "2026-04-05")
        )
        val keys = CourseDifficultyCalculator.courseKeys(races)
        fun prev(id: String) = CourseDifficultyCalculator.previousEdition(races.first { it.raceId == id }, races, keys)?.raceId

        assertEquals("a1", prev("b1"))
        assertEquals("a41", prev("b41")) // 2e course de Virton de la saison
        assertEquals("a29", prev("b29"))
        assertEquals("a13", prev("b11")) // numéro de manche différent, même parcours
        assertNull(prev("a1"))
    }

    @Test
    fun weather_readsStartHourAndRainOfThePreviousDays() {
        val times = (15..18).flatMap { d -> (0..23).map { h -> "\"2026-10-%02dT%02d:00\"".format(d, h) } }
        val temps = (0 until 96).map { if (it == 3 * 24 + 10) "9.4" else "8.0" }
        val precip = (0 until 96).map { i -> if (i < 72) "0.5" else if (i == 82) "1.2" else "0.0" }
        val wind = (0 until 96).map { "12.0" }
        val codes = (0 until 96).map { if (it == 82) "61" else "3" }
        val json = """{"latitude":49.56,"longitude":5.54,"timezone":"Europe/Brussels",
            "hourly":{"time":[${times.joinToString(",")}],"temperature_2m":[${temps.joinToString(",")}],
            "precipitation":[${precip.joinToString(",")}],"wind_speed_10m":[${wind.joinToString(",")}],
            "weather_code":[${codes.joinToString(",")}]}}"""

        val w = OpenMeteoClient.parseWeather(json, "alg_2026_r41", "2026-10-18", 10, OpenMeteoClient.SOURCE_ARCHIVE, 0L)!!
        assertEquals(9.4, w.temperatureC!!, 0.01)
        assertEquals(31.0, w.rainPrevious72hMm!!, 0.01) // du 15 à 10 h au 18 à 10 h : 62 h × 0,5 mm
        assertEquals(1.2, w.precipitationDuringMm!!, 0.01) // 10 h + 11 h
        assertEquals(10, w.startHourLocal)
        assertEquals("Pluie", OpenMeteoClient.weatherLabel(w.weatherCode))

        assertEquals(10, OpenMeteoClient.parseStartHour("10h00 – 4,8 et 10 km"))
        assertEquals(9, OpenMeteoClient.parseStartHour("Départ 9h30"))
        assertNull(OpenMeteoClient.parseStartHour("Route et chemins"))

        // Sans heure de départ : seules les valeurs de la journée sont gardées.
        val day = OpenMeteoClient.parseWeather(json, "x", "2026-10-18", null, OpenMeteoClient.SOURCE_ARCHIVE, 0L)!!
        assertNull(day.temperatureC)
        assertEquals(8.0, day.dayMinC!!, 0.01)
        assertEquals(9.4, day.dayMaxC!!, 0.01)
    }
}
