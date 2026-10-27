package com.example.data.local

import java.text.Normalizer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GaumeTerritoryDef(
    val territoryId: String,
    val title: String,
    val subtitle: String,
    val badgeTitle: String,
    val centerLat: Double,
    val centerLon: Double,
    val radiusMeters: Int,
    val colorHex: Long,
    /** Villages (et sections de communes) rattachés à ce territoire du jeu. */
    val villages: List<String>
)

/**
 * Position d'une manche. Les coordonnées sont celles du départ publiées par MyChrono,
 * à défaut celles du village ; null si le lieu est inconnu (jamais de coordonnées inventées).
 */
data class GaumeVillageGeo(
    val canonicalVillage: String,
    val latitude: Double?,
    val longitude: Double?,
    val territoryId: String,
    val territoryName: String
) {
    val hasKnownLocation: Boolean get() = latitude != null && longitude != null
}

/**
 * Données de départ : les 3 profils du Trio et la géographie des villages de Gaume.
 * Le calendrier n'est PAS inventé ici : il vient uniquement de MyChrono.
 */
object GaumeInitialSeedData {

    const val NO_TERRITORY_ID = ""
    const val NO_TERRITORY_NAME = "Hors fiefs"

    /**
     * Profils initiaux. Les identifiants MyChrono sont ceux relevés dans les résultats officiels
     * (Damien Collet 1171, Laetitia 1202, Elodie 1189) ; les dossards sont annuels et propres à chaque distance.
     */
    fun buildInitialProfiles(): List<RunnerProfileEntity> = listOf(
        RunnerProfileEntity(
            profileId = "me",
            displayName = "Damien Collet",
            myChronoRunnerId = "1171",
            defaultBib10km = "697",
            defaultBib5km = "",
            preferredDistanceKm = 10,
            categoryLabel = "Seniors H",
            clubName = "Gaume Trail",
            avatarColorHex = 0xFF1B5E3A
        ),
        RunnerProfileEntity(
            profileId = "laetitia",
            displayName = "Laetitia",
            myChronoRunnerId = "1202",
            defaultBib10km = "",
            defaultBib5km = "1075",
            preferredDistanceKm = 5,
            categoryLabel = "Dames",
            clubName = "Gaume Trail",
            avatarColorHex = 0xFFC85A32
        ),
        RunnerProfileEntity(
            profileId = "elodie",
            displayName = "Elodie",
            myChronoRunnerId = "1189",
            defaultBib10km = "",
            defaultBib5km = "1076",
            preferredDistanceKm = 5,
            categoryLabel = "Dames",
            clubName = "Gaume Trail",
            avatarColorHex = 0xFF006D77
        )
    )

    /**
     * Les 7 territoires du jeu, découpés sur les lieux réels des manches 2026.
     * Centre et rayon calculés à partir des coordonnées de départ publiées par MyChrono.
     */
    val gaumeTerritories: List<GaumeTerritoryDef> = listOf(
        GaumeTerritoryDef(
            territoryId = "semois_orval",
            title = "Semois & Orval",
            subtitle = "Florenville, Muno, Lacuisine, Chiny, Suxy, Prouvy",
            badgeTitle = "Seigneur d'Orval",
            centerLat = 49.7152,
            centerLon = 5.3236,
            radiusMeters = 12300,
            colorHex = 0xFFE07A2E,
            villages = listOf(
                "Florenville", "Muno", "Lacuisine", "Chiny", "Suxy", "Prouvy", "Jamoigne", "Izel", "Pin",
                "Moyen", "Les Bulles", "Termes", "Chassepierre", "Sainte-Cécile", "Fontenoille",
                "Villers-devant-Orval", "Orval", "Herbeumont", "Bouillon"
            )
        ),
        GaumeTerritoryDef(
            territoryId = "rulles_anlier",
            title = "Rulles & Anlier",
            subtitle = "Rulles, Marbehan, Houdemont, Habay-la-Neuve, Mellier",
            badgeTitle = "Maître du Fief de Rulles",
            centerLat = 49.7314,
            centerLon = 5.5803,
            radiusMeters = 7900,
            colorHex = 0xFFD49B1B,
            villages = listOf(
                "Rulles", "Marbehan", "Houdemont", "Habay-la-Neuve", "Habay-la-Vieille", "Habay", "Hachy",
                "Anlier", "Mellier", "Thibessart", "Léglise", "Neufchâteau"
            )
        ),
        GaumeTerritoryDef(
            territoryId = "arlon_attert",
            title = "Pays d'Arlon & Attert",
            subtitle = "Fouches, Freylange, Nobressart, Beckerich (LU)",
            badgeTitle = "Sentinelle d'Attert",
            centerLat = 49.7139,
            centerLon = 5.7722,
            radiusMeters = 10500,
            colorHex = 0xFF2B59C3,
            villages = listOf(
                "Fouches", "Freylange", "Nobressart", "Beckerich", "Heinsch", "Stockem", "Arlon", "Attert",
                "Thiaumont", "Heinstert", "Nothomb", "Tontelange", "Metzert", "Lottert", "Waltzing", "Bonnert",
                "Frassem", "Clairefontaine", "Autelbas", "Weyler", "Martelange"
            )
        ),
        GaumeTerritoryDef(
            territoryId = "etalle_tintigny",
            title = "Étalle & Tintigny",
            subtitle = "Étalle, Fratin, Buzenol, Chantemelle, Bellefontaine",
            badgeTitle = "Gardien de Montauban",
            centerLat = 49.6534,
            centerLon = 5.5794,
            radiusMeters = 7900,
            colorHex = 0xFF1E7E48,
            villages = listOf(
                "Étalle", "Fratin", "Buzenol", "Chantemelle", "Bellefontaine", "Sainte-Marie-sur-Semois",
                "Sainte-Marie", "Villers-sur-Semois", "Mortinsart", "Vance", "Sampont", "Tintigny", "Rossignol",
                "Breuvanne", "Lahage", "Saint-Vincent", "Ansart", "Poncelle"
            )
        ),
        GaumeTerritoryDef(
            territoryId = "virton_cuesta",
            title = "Virton & la Cuesta",
            subtitle = "Virton, Ethe, Belmont, Bleid, Ruette, Pierrard, Rabais",
            badgeTitle = "Roi de la Cuesta",
            centerLat = 49.5716,
            centerLon = 5.5642,
            radiusMeters = 6000,
            colorHex = 0xFFC85A32,
            villages = listOf(
                "Virton", "Ethe", "Ethe-Belmont", "Belmont", "Bleid", "Ruette", "Pierrard", "Rabais",
                "Saint-Mard", "Latour", "Chenois", "Gomery", "Grandcourt"
            )
        ),
        GaumeTerritoryDef(
            territoryId = "meix_rouvroy",
            title = "Meix & Rouvroy",
            subtitle = "Meix-devant-Virton, Gérouville, Sommethonne, Dampicourt, Lamorteau",
            badgeTitle = "Vigie du Sud gaumais",
            centerLat = 49.5818,
            centerLon = 5.4666,
            radiusMeters = 7500,
            colorHex = 0xFF7B4EA3,
            villages = listOf(
                "Meix-devant-Virton", "Gérouville", "Sommethonne", "Robelmont", "Villers-la-Loue", "Dampicourt",
                "Lamorteau", "Harnoncourt", "Torgny", "Rouvroy", "Couvreux", "Montquintin"
            )
        ),
        GaumeTerritoryDef(
            territoryId = "saint_leger_musson",
            title = "Saint-Léger, Musson & Aubange",
            subtitle = "Saint-Léger, Musson, Mussy-la-Ville, Signeulx, Halanzy, Rachecourt",
            badgeTitle = "Sentinelle des Trois Frontières",
            centerLat = 49.5732,
            centerLon = 5.6859,
            radiusMeters = 6100,
            colorHex = 0xFF006D77,
            villages = listOf(
                "Saint-Léger", "Châtillon", "Meix-le-Tige", "Musson", "Mussy-la-Ville", "Signeulx", "Baranzy",
                "Willancourt", "Halanzy", "Rachecourt", "Aubange", "Athus", "Aix-sur-Cloye", "Battincourt",
                "Messancy", "Habergy", "Bébange", "Hondelange", "Wolkrange", "Sélange"
            )
        )
    )

    private val territoryById = gaumeTerritories.associateBy { it.territoryId }

    private val territoryIdByVillageKey: Map<String, String> = buildMap {
        gaumeTerritories.forEach { t -> t.villages.forEach { put(normalizeKey(it), t.territoryId) } }
    }

    private data class VillagePoint(val name: String, val lat: Double, val lon: Double)

    /**
     * Coordonnées des villages (centre du village), utilisées seulement quand MyChrono
     * ne publie pas le point de départ. Vérifiées contre les départs réels 2026 (écart < 3 km).
     */
    private val villagePoints: List<VillagePoint> = listOf(
        VillagePoint("Florenville", 49.6992, 5.3103),
        VillagePoint("Pin", 49.6897, 5.3469),
        VillagePoint("Izel", 49.6936, 5.3742),
        VillagePoint("Jamoigne", 49.6947, 5.4094),
        VillagePoint("Moyen", 49.6889, 5.3908),
        VillagePoint("Chassepierre", 49.7072, 5.2650),
        VillagePoint("Sainte-Cécile", 49.7294, 5.2417),
        VillagePoint("Fontenoille", 49.7119, 5.2175),
        VillagePoint("Muno", 49.7114, 5.1744),
        VillagePoint("Villers-devant-Orval", 49.6311, 5.3314),
        VillagePoint("Orval", 49.6394, 5.3489),
        VillagePoint("Bouillon", 49.7944, 5.0672),
        VillagePoint("Herbeumont", 49.7764, 5.2358),
        VillagePoint("Prouvy", 49.6834, 5.4349),
        VillagePoint("Chiny", 49.7383, 5.3433),
        VillagePoint("Lacuisine", 49.7128, 5.3183),
        VillagePoint("Suxy", 49.7667, 5.4017),
        VillagePoint("Les Bulles", 49.6958, 5.4328),
        VillagePoint("Termes", 49.7081, 5.4508),
        VillagePoint("Tintigny", 49.6833, 5.5136),
        VillagePoint("Breuvanne", 49.6797, 5.4792),
        VillagePoint("Bellefontaine", 49.6642, 5.4953),
        VillagePoint("Lahage", 49.6431, 5.4792),
        VillagePoint("Rossignol", 49.7175, 5.4864),
        VillagePoint("Saint-Vincent", 49.6675, 5.4414),
        VillagePoint("Ansart", 49.6928, 5.5244),
        VillagePoint("Poncelle", 49.6606, 5.5186),
        VillagePoint("Rulles", 49.7186, 5.5608),
        VillagePoint("Marbehan", 49.7264, 5.5353),
        VillagePoint("Houdemont", 49.7097, 5.5950),
        VillagePoint("Habay-la-Vieille", 49.7231, 5.6186),
        VillagePoint("Habay-la-Neuve", 49.7283, 5.6458),
        VillagePoint("Habay", 49.7283, 5.6458),
        VillagePoint("Hachy", 49.7031, 5.6836),
        VillagePoint("Anlier", 49.7722, 5.6206),
        VillagePoint("Mellier", 49.7675, 5.5183),
        VillagePoint("Thibessart", 49.7592, 5.5619),
        VillagePoint("Léglise", 49.7994, 5.5378),
        VillagePoint("Neufchâteau", 49.8406, 5.4353),
        VillagePoint("Étalle", 49.6742, 5.5633),
        VillagePoint("Sainte-Marie-sur-Semois", 49.6708, 5.5333),
        VillagePoint("Sainte-Marie", 49.6708, 5.5333),
        VillagePoint("Fratin", 49.6628, 5.5561),
        VillagePoint("Villers-sur-Semois", 49.6967, 5.5628),
        VillagePoint("Mortinsart", 49.6939, 5.5819),
        VillagePoint("Buzenol", 49.6361, 5.5944),
        VillagePoint("Chantemelle", 49.6567, 5.6531),
        VillagePoint("Vance", 49.6725, 5.6656),
        VillagePoint("Sampont", 49.6833, 5.6914),
        VillagePoint("Meix-devant-Virton", 49.6056, 5.4806),
        VillagePoint("Gérouville", 49.6222, 5.4244),
        VillagePoint("Robelmont", 49.5964, 5.5122),
        VillagePoint("Villers-la-Loue", 49.5789, 5.4822),
        VillagePoint("Sommethonne", 49.5892, 5.4519),
        VillagePoint("Virton", 49.5675, 5.5333),
        VillagePoint("Pierrard", 49.5735, 5.5490),
        VillagePoint("Rabais", 49.5981, 5.5385),
        VillagePoint("Saint-Mard", 49.5553, 5.5242),
        VillagePoint("Ethe", 49.5833, 5.5825),
        VillagePoint("Ethe-Belmont", 49.5834, 5.5693),
        VillagePoint("Belmont", 49.5758, 5.5678),
        VillagePoint("Bleid", 49.5719, 5.6197),
        VillagePoint("Gomery", 49.5603, 5.6056),
        VillagePoint("Latour", 49.5586, 5.5736),
        VillagePoint("Chenois", 49.5664, 5.5644),
        VillagePoint("Ruette", 49.5414, 5.5922),
        VillagePoint("Grandcourt", 49.5328, 5.5736),
        VillagePoint("Dampicourt", 49.5464, 5.4983),
        VillagePoint("Harnoncourt", 49.5414, 5.4792),
        VillagePoint("Lamorteau", 49.5269, 5.4731),
        VillagePoint("Torgny", 49.5072, 5.4753),
        VillagePoint("Rouvroy", 49.5392, 5.4903),
        VillagePoint("Couvreux", 49.5322, 5.4956),
        VillagePoint("Montquintin", 49.5453, 5.5125),
        VillagePoint("Saint-Léger", 49.6122, 5.6561),
        VillagePoint("Châtillon", 49.6225, 5.6914),
        VillagePoint("Meix-le-Tige", 49.6069, 5.7169),
        VillagePoint("Musson", 49.5583, 5.7053),
        VillagePoint("Mussy-la-Ville", 49.5689, 5.6808),
        VillagePoint("Signeulx", 49.5561, 5.6583),
        VillagePoint("Baranzy", 49.5514, 5.7208),
        VillagePoint("Willancourt", 49.5819, 5.6769),
        VillagePoint("Halanzy", 49.5603, 5.7433),
        VillagePoint("Rachecourt", 49.5908, 5.7367),
        VillagePoint("Aubange", 49.5683, 5.8056),
        VillagePoint("Athus", 49.5653, 5.8369),
        VillagePoint("Aix-sur-Cloye", 49.5758, 5.7769),
        VillagePoint("Battincourt", 49.5786, 5.7611),
        VillagePoint("Messancy", 49.5922, 5.8183),
        VillagePoint("Habergy", 49.6128, 5.7606),
        VillagePoint("Bébange", 49.6186, 5.7744),
        VillagePoint("Hondelange", 49.6331, 5.8292),
        VillagePoint("Wolkrange", 49.6317, 5.7969),
        VillagePoint("Sélange", 49.6083, 5.8492),
        VillagePoint("Arlon", 49.6833, 5.8167),
        VillagePoint("Waltzing", 49.6819, 5.8475),
        VillagePoint("Bonnert", 49.6989, 5.8339),
        VillagePoint("Frassem", 49.6981, 5.8506),
        VillagePoint("Clairefontaine", 49.6656, 5.8819),
        VillagePoint("Autelbas", 49.6497, 5.8644),
        VillagePoint("Weyler", 49.6569, 5.8319),
        VillagePoint("Stockem", 49.6739, 5.7867),
        VillagePoint("Heinsch", 49.6956, 5.7519),
        VillagePoint("Freylange", 49.6967, 5.7744),
        VillagePoint("Fouches", 49.6894, 5.7147),
        VillagePoint("Heinstert", 49.7347, 5.7267),
        VillagePoint("Attert", 49.7511, 5.7864),
        VillagePoint("Nothomb", 49.7322, 5.7836),
        VillagePoint("Tontelange", 49.7264, 5.8108),
        VillagePoint("Metzert", 49.7192, 5.7964),
        VillagePoint("Nobressart", 49.7267, 5.7203),
        VillagePoint("Thiaumont", 49.7100, 5.7408),
        VillagePoint("Lottert", 49.7042, 5.7547),
        VillagePoint("Martelange", 49.8317, 5.7369),
        VillagePoint("Beckerich", 49.7240, 5.8970)
    )

    fun normalizeKey(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase().trim(), Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace("'", " ")
            .replace("’", " ")
            .replace("-", " ")
            .replace(Regex("[()]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /** Village connu dont le nom correspond au lieu MyChrono (exact, puis mot entier, le plus long d'abord). */
    private fun findKnownVillage(place: String): VillagePoint? {
        val key = normalizeKey(place)
        if (key.isBlank()) return null
        villagePoints.firstOrNull { normalizeKey(it.name) == key }?.let { return it }
        return villagePoints
            .sortedByDescending { it.name.length }
            .firstOrNull { vp -> Regex("\\b${Regex.escape(normalizeKey(vp.name))}\\b").containsMatchIn(key) }
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Territoire le plus proche d'un point réel (pour un lieu absent des listes de villages). */
    fun nearestTerritoryId(lat: Double, lon: Double): String =
        gaumeTerritories.minByOrNull { distanceMeters(lat, lon, it.centerLat, it.centerLon) }?.territoryId
            ?: NO_TERRITORY_ID

    /**
     * Position et territoire d'une manche :
     * coordonnées du départ MyChrono, sinon centre du village connu, sinon inconnues.
     */
    private val geoCache = java.util.concurrent.ConcurrentHashMap<Triple<String, Double?, Double?>, GaumeVillageGeo>()

    fun resolveGaumeGeoForRace(race: RaceEntity): GaumeVillageGeo =
        geoCache.getOrPut(Triple(race.village, race.startLatitude, race.startLongitude)) { computeGeo(race) }

    private fun computeGeo(race: RaceEntity): GaumeVillageGeo {
        val known = findKnownVillage(race.village)
        val lat = race.startLatitude ?: known?.lat
        val lon = race.startLongitude ?: known?.lon

        val territoryId = known?.let { territoryIdByVillageKey[normalizeKey(it.name)] }
            ?: territoryIdByVillageKey[normalizeKey(race.village)]
            ?: if (lat != null && lon != null) nearestTerritoryId(lat, lon) else NO_TERRITORY_ID

        return GaumeVillageGeo(
            canonicalVillage = race.village.ifBlank { known?.name ?: "Lieu inconnu" },
            latitude = lat,
            longitude = lon,
            territoryId = territoryId,
            territoryName = territoryById[territoryId]?.title ?: NO_TERRITORY_NAME
        )
    }
}
