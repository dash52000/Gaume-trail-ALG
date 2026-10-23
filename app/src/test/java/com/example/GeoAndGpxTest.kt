package com.example

import com.example.data.local.GaumeInitialSeedData
import com.example.data.local.RaceEntity
import com.example.domain.GpxAndTrackUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeoAndGpxTest {

    private fun race(place: String, lat: Double? = null, lon: Double? = null) = RaceEntity(
        raceId = "alg_2026_r01", seasonYear = 2026, roundNumber = 1, myChronoEventId = "1",
        title = place, village = place, dateIso = "2026-01-25", distanceKm = 10.0,
        elevationGainMeters = null, isCompleted = true, startLatitude = lat, startLongitude = lon
    )

    @Test
    fun geo_prefersMyChronoStartPoint_andMapsRealPlacesToTerritories() {
        val etalle = GaumeInitialSeedData.resolveGaumeGeoForRace(race("Etalle", 49.6686, 5.6046))
        assertEquals(49.6686, etalle.latitude!!, 1e-6)
        assertEquals("etalle_tintigny", etalle.territoryId)

        assertEquals("arlon_attert", GaumeInitialSeedData.resolveGaumeGeoForRace(race("Beckerich")).territoryId)
        assertEquals("virton_cuesta", GaumeInitialSeedData.resolveGaumeGeoForRace(race("Rabais (Virton)")).territoryId)
        assertEquals("meix_rouvroy", GaumeInitialSeedData.resolveGaumeGeoForRace(race("Dampicourt")).territoryId)
        assertEquals("semois_orval", GaumeInitialSeedData.resolveGaumeGeoForRace(race("Prouvy")).territoryId)
    }

    @Test
    fun geo_unknownPlaceWithoutCoordinates_staysUnknown() {
        val unknown = GaumeInitialSeedData.resolveGaumeGeoForRace(race("Village Imaginaire"))
        assertNull(unknown.latitude)
        assertFalse(unknown.hasKnownLocation)
        assertEquals(GaumeInitialSeedData.NO_TERRITORY_ID, unknown.territoryId)

        // Lieu absent des listes mais coordonnées publiées : territoire le plus proche.
        val nearVirton = GaumeInitialSeedData.resolveGaumeGeoForRace(race("Nouveau lieu", 49.567, 5.53))
        assertEquals("virton_cuesta", nearVirton.territoryId)
    }

    @Test
    fun allTerritoriesHaveRealRaces2026() {
        val places2026 = listOf(
            "Virton", "Ethe", "Florenville", "Fouches", "Sommethonne", "Nobressart", "Buzenol", "Ruette", "Mellier",
            "Chiny", "Freylange", "Suxy", "Muno", "Etalle", "Beckerich", "Rachecourt", "Lacuisine", "Pierrard",
            "Chantemelle", "Habay-la-Neuve", "Marbehan", "Meix-devant-Virton", "Bleid", "Signeulx", "Gérouville",
            "Lamorteau", "Houdemont", "Rabais (Virton)", "Fratin", "Prouvy", "Musson", "Mussy-la-Ville",
            "Ethe-Belmont", "Bellefontaine", "Halanzy", "Saint-Léger", "Rulles", "Dampicourt"
        )
        val territories = places2026.map { GaumeInitialSeedData.resolveGaumeGeoForRace(race(it)).territoryId }
        assertTrue(territories.none { it == GaumeInitialSeedData.NO_TERRITORY_ID })
        assertEquals(GaumeInitialSeedData.gaumeTerritories.map { it.territoryId }.toSet(), territories.toSet())
    }

    @Test
    fun gpx_withElevation_measuresDPlus() {
        val gpx = """
            <gpx><trk><name>Rulles</name><trkseg>
              <trkpt lat="49.7160" lon="5.5610"><ele>320</ele></trkpt>
              <trkpt lat="49.7170" lon="5.5620"><ele>330</ele></trkpt>
              <trkpt lat="49.7180" lon="5.5630"><ele>325</ele></trkpt>
              <trkpt lat="49.7190" lon="5.5640"><ele>345</ele></trkpt>
            </trkseg></trk></gpx>
        """.trimIndent()
        val parsed = GpxAndTrackUtils.parseGpxXmlContent(gpx)
        assertEquals("Rulles", parsed.title)
        assertEquals(30, parsed.elevationGainMeters)
        assertTrue(parsed.distanceKm > 0.3)
    }

    @Test
    fun gpx_withoutElevation_hasUnknownDPlus() {
        val gpx = """
            <gpx><trk><trkseg>
              <trkpt lat="49.7160" lon="5.5610"></trkpt>
              <trkpt lat="49.7170" lon="5.5620"></trkpt>
            </trkseg></trk></gpx>
        """.trimIndent()
        assertNull(GpxAndTrackUtils.parseGpxXmlContent(gpx).elevationGainMeters)
    }

    @Test(expected = IllegalArgumentException::class)
    fun gpx_withoutTrack_isRejectedInsteadOfInvented() {
        GpxAndTrackUtils.parseGpxXmlContent("<gpx><metadata><name>vide</name></metadata></gpx>")
    }
}
