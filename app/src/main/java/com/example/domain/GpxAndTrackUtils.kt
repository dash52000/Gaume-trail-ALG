package com.example.domain

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class TrackPoint(
    val lat: Double,
    val lon: Double,
    /** Altitude lue dans le fichier ; NaN si le point n'en a pas. */
    val elevationMeters: Double,
    val heartRateBpm: Int? = null
)

data class ParsedGpxSummary(
    val title: String,
    val points: List<TrackPoint>,
    /** D+ mesuré sur les altitudes du fichier ; null si le fichier n'en contient pas. */
    val elevationGainMeters: Int?,
    val distanceKm: Double,
    val avgHeartRate: Int?,
    val maxHeartRate: Int?,
    val serializedCoordinates: String
) {
    val trackPoints: List<TrackPoint> get() = points
    val avgHeartRateBpm: Int? get() = avgHeartRate
    val maxHeartRateBpm: Int? get() = maxHeartRate
}

object GpxAndTrackUtils {

    /** Seuil d'hystérésis pour le D+ : ignore les variations d'altitude inférieures (bruit GPS/baro). */
    private const val ELEVATION_NOISE_THRESHOLD_M = 1.8

    fun parseGpxXmlContent(xmlContent: String, fallbackName: String = "Trace GPX"): ParsedGpxSummary {
        return parseGpxStream(xmlContent.byteInputStream(Charsets.UTF_8), fallbackName)
    }

    /**
     * Analyse un fichier GPX réel : trace GPS, D+, distance et fréquence cardiaque
     * (extensions Garmin TrackPointExtension). Un fichier sans trace exploitable est refusé :
     * aucune trace n'est générée à sa place.
     */
    fun parseGpxStream(inputStream: InputStream, fallbackName: String = "Trace GPX"): ParsedGpxSummary {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var trackTitle = fallbackName
        val rawPoints = mutableListOf<TrackPoint>()
        var currentLat: Double? = null
        var currentLon: Double? = null
        var currentEle: Double = Double.NaN
        var currentHr: Int? = null
        var currentTag: String? = null
        var insideTrkpt = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tagName = parser.name.lowercase()
                    currentTag = tagName
                    if (tagName == "trkpt" || tagName == "rtept") {
                        insideTrkpt = true
                        currentLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                        currentLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                        currentEle = Double.NaN
                        currentHr = null
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim().orEmpty()
                    if (text.isNotEmpty()) {
                        when {
                            !insideTrkpt && currentTag == "name" && trackTitle == fallbackName -> {
                                trackTitle = text
                            }
                            insideTrkpt && currentTag == "ele" -> {
                                currentEle = text.toDoubleOrNull() ?: Double.NaN
                            }
                            insideTrkpt && (currentTag == "hr" || currentTag?.endsWith(":hr") == true) -> {
                                currentHr = text.toIntOrNull()
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tagName = parser.name.lowercase()
                    if ((tagName == "trkpt" || tagName == "rtept") && insideTrkpt) {
                        val lat = currentLat
                        val lon = currentLon
                        if (lat != null && lon != null) {
                            rawPoints.add(
                                TrackPoint(
                                    lat = lat,
                                    lon = lon,
                                    elevationMeters = currentEle,
                                    heartRateBpm = currentHr
                                )
                            )
                        }
                        insideTrkpt = false
                    }
                    currentTag = null
                }
            }
            eventType = parser.next()
        }

        require(rawPoints.size >= 2) { "Ce fichier GPX ne contient pas de trace exploitable." }

        val withElevation = rawPoints.filter { !it.elevationMeters.isNaN() }
        val dPlus: Int? = if (withElevation.size >= 2) {
            var gain = 0.0
            var anchorEle = withElevation.first().elevationMeters
            for (pt in withElevation.drop(1)) {
                val diff = pt.elevationMeters - anchorEle
                if (diff >= ELEVATION_NOISE_THRESHOLD_M) {
                    gain += diff
                    anchorEle = pt.elevationMeters
                } else if (diff <= -ELEVATION_NOISE_THRESHOLD_M) {
                    anchorEle = pt.elevationMeters
                }
            }
            gain.roundToInt()
        } else {
            null
        }

        var totalMeters = 0.0
        for (i in 1 until rawPoints.size) {
            totalMeters += haversineMeters(
                rawPoints[i - 1].lat,
                rawPoints[i - 1].lon,
                rawPoints[i].lat,
                rawPoints[i].lon
            )
        }

        val heartRates = rawPoints.mapNotNull { it.heartRateBpm }.filter { it in 30..230 }
        val avgHr = if (heartRates.isNotEmpty()) heartRates.average().roundToInt() else null
        val maxHr = heartRates.maxOrNull()

        val displayPoints = downsampleTrack(rawPoints, maxPoints = 72)
        return ParsedGpxSummary(
            title = trackTitle,
            points = displayPoints,
            elevationGainMeters = dPlus,
            distanceKm = ((totalMeters / 1000.0) * 100.0).roundToInt() / 100.0,
            avgHeartRate = avgHr,
            maxHeartRate = maxHr,
            serializedCoordinates = serializeTrackPoints(displayPoints)
        )
    }

    fun serializeTrackPoints(points: List<TrackPoint>): String {
        return points.joinToString(";") { pt ->
            val latStr = ((pt.lat * 100000.0).roundToInt() / 100000.0).toString()
            val lonStr = ((pt.lon * 100000.0).roundToInt() / 100000.0).toString()
            val eleStr = if (pt.elevationMeters.isNaN()) "" else pt.elevationMeters.roundToInt().toString()
            val hrStr = pt.heartRateBpm?.toString() ?: ""
            "$latStr,$lonStr,$eleStr,$hrStr"
        }
    }

    fun deserializeTrackPoints(serialized: String): List<TrackPoint> {
        if (serialized.isBlank()) return emptyList()
        return serialized.split(";").mapNotNull { token ->
            val parts = token.split(",")
            if (parts.size >= 3) {
                val lat = parts[0].toDoubleOrNull()
                val lon = parts[1].toDoubleOrNull()
                val ele = parts[2].toDoubleOrNull() ?: Double.NaN
                val hr = parts.getOrNull(3)?.toIntOrNull()
                if (lat != null && lon != null) {
                    TrackPoint(lat = lat, lon = lon, elevationMeters = ele, heartRateBpm = hr)
                } else null
            } else null
        }
    }

    private fun downsampleTrack(points: List<TrackPoint>, maxPoints: Int): List<TrackPoint> {
        if (points.size <= maxPoints) return points
        val step = (points.size - 1).toDouble() / (maxPoints - 1)
        return (0 until maxPoints).map { idx ->
            val sourceIdx = (idx * step).roundToInt().coerceIn(0, points.lastIndex)
            points[sourceIdx]
        }
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
