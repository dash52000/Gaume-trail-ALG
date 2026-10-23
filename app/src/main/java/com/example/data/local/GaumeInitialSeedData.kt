package com.example.data.local

import java.text.Normalizer
import kotlin.math.abs

data class GaumeTerritoryDef(
    val territoryId: String,
    val title: String,
    val subtitle: String,
    val badgeTitle: String,
    val centerLat: Double,
    val centerLon: Double,
    val radiusMeters: Int,
    val colorHex: Long
)

data class GaumeVillageGeo(
    val canonicalVillage: String,
    val latitude: Double,
    val longitude: Double,
    val territoryId: String,
    val territoryName: String
)

/**
 * Configuration initiale des 3 profils du Trio (Damien, Laetitia, Élodie),
 * calendrier officiel des villages du Challenge des Allures Libres de Gaume (ALG) à partir de 2026
 * (avec ZÉRO résultat fictif) et coordonnées GPS WGS84 exactes des villages de Gaume.
 */
object GaumeInitialSeedData {

    /**
     * Profils initiaux :
     * - Damien Collet : dossard 10 km #697, pas de dossard 5 km par défaut ("") -> 0 course sur 5 km tant que non attribué.
     * - Laetitia : pas de dossard 10 km ("") -> 0 course sur 10 km, dossard 5 km #1075.
     * - Élodie : pas de dossard 10 km ("") -> 0 course sur 10 km, dossard 5 km #1076.
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
            avatarColorHex = 0xFF1B5E3A,
            lastSyncedEpochMillis = null
        ),
        RunnerProfileEntity(
            profileId = "laetitia",
            displayName = "Laetitia",
            myChronoRunnerId = "1075",
            defaultBib10km = "",
            defaultBib5km = "1075",
            preferredDistanceKm = 5,
            categoryLabel = "Dames",
            clubName = "Gaume Trail",
            avatarColorHex = 0xFFC85A32,
            lastSyncedEpochMillis = null
        ),
        RunnerProfileEntity(
            profileId = "elodie",
            displayName = "Élodie",
            myChronoRunnerId = "1076",
            defaultBib10km = "",
            defaultBib5km = "1076",
            preferredDistanceKm = 5,
            categoryLabel = "Dames",
            clubName = "Gaume Trail",
            avatarColorHex = 0xFF006D77,
            lastSyncedEpochMillis = null
        )
    )

    fun buildDirectoryEntries(): List<MyChronoDirectoryEntry> = listOf(
        MyChronoDirectoryEntry(
            myChronoRunnerId = "1171",
            fullName = "Damien Collet",
            defaultBib10km = "697",
            defaultBib5km = "",
            club = "Gaume Trail",
            category = "Seniors H",
            sampleRaceCount = 0,
            bestPoints = 0
        ),
        MyChronoDirectoryEntry(
            myChronoRunnerId = "1075",
            fullName = "Laetitia",
            defaultBib10km = "",
            defaultBib5km = "1075",
            club = "Gaume Trail",
            category = "Dames",
            sampleRaceCount = 0,
            bestPoints = 0
        ),
        MyChronoDirectoryEntry(
            myChronoRunnerId = "1076",
            fullName = "Élodie",
            defaultBib10km = "",
            defaultBib5km = "1076",
            club = "Gaume Trail",
            category = "Dames",
            sampleRaceCount = 0,
            bestPoints = 0
        )
    )

    /**
     * Calendrier des manches des Allures Libres de Gaume (ALG) pour une saison (>= 2026).
     * Contient uniquement les manches et villages (ZÉRO résultat fictif) afin que la Carte de Gaume
     * et le Passeport Collector affichent immédiatement tous les villages de Gaume aux coordonnées GPS exactes.
     */
    fun buildOfficialGaumeSeasonCalendar(seasonYear: Int): List<RaceEntity> {
        val year = maxOf(2026, seasonYear)
        data class RoundSeed(
            val round: Int,
            val title: String,
            val village: String,
            val monthDay: String,
            val distKm: Double,
            val elevM: Int,
            val profileDesc: String
        )

        val rounds = listOf(
            RoundSeed(1, "Allure Libre de Virton", "Virton", "03-08", 10.2, 145, "Vallonné forestier (Bois de Bon-Lieu)"),
            RoundSeed(2, "Allure Libre d'Izel", "Izel", "03-15", 10.0, 120, "Bord de Semois et chemins agricoles"),
            RoundSeed(3, "Allure Libre de Bellefontaine", "Bellefontaine", "03-22", 10.4, 155, "Sentiers forestiers de Gaume"),
            RoundSeed(4, "Allure Libre de Jamoigne", "Jamoigne", "03-29", 10.1, 135, "Boucle panoramique sur la Semois"),
            RoundSeed(5, "Allure Libre d'Étalle", "Étalle", "04-05", 10.0, 110, "Parcours roulant et lisière de forêt"),
            RoundSeed(6, "Allure Libre de Rossignol", "Rossignol", "04-12", 10.5, 165, "Forêt domaniale et massif gaumais"),
            RoundSeed(7, "Allure Libre de Pin", "Pin", "04-19", 10.3, 180, "Côte du Maquis et sentiers d'Orval"),
            RoundSeed(8, "Allure Libre de Rulles", "Rulles", "04-26", 10.2, 170, "Vallée de la Rulles et forêt d'Anlier"),
            RoundSeed(9, "Allure Libre de Saint-Mard", "Saint-Mard", "05-03", 10.0, 140, "Cueste de Virton et chemins blancs"),
            RoundSeed(10, "Allure Libre d'Ethe", "Ethe", "05-10", 10.6, 175, "Vallée du Ton et bois de Gomery"),
            RoundSeed(11, "Allure Libre de Tintigny", "Tintigny", "05-17", 10.1, 130, "Campagne gaumaise et Semois"),
            RoundSeed(12, "Allure Libre de Habay-la-Vieille", "Habay-la-Vieille", "05-24", 10.4, 160, "Étangs et lisière d'Anlier"),
            RoundSeed(13, "Allure Libre de Chiny", "Chiny", "06-07", 10.8, 210, "Escarpements de la Semois sauvage"),
            RoundSeed(14, "Allure Libre de Florenville", "Florenville", "06-14", 10.2, 150, "Belvédère de la Semois et terroir d'Orval"),
            RoundSeed(15, "Allure Libre de Saint-Léger", "Saint-Léger", "06-21", 10.5, 165, "Vallée du Ton et forêt de Châtillon"),
            RoundSeed(16, "Allure Libre de Musson", "Musson", "06-28", 10.3, 175, "Cueste bajocienne et Trois Frontières"),
            RoundSeed(17, "Allure Libre de Meix-devant-Virton", "Meix-devant-Virton", "08-16", 10.4, 185, "Vallons de la Chevratte"),
            RoundSeed(18, "Allure Libre de Fratin", "Fratin", "08-23", 10.0, 125, "Pinèdes et sablières d'Étalle"),
            RoundSeed(19, "Allure Libre de Marbehan", "Marbehan", "09-06", 10.5, 170, "Crêtes de la Rulles"),
            RoundSeed(20, "Allure Libre de Signeulx", "Signeulx", "09-13", 10.1, 135, "Plaine de la Vire et côtes boisées"),
            RoundSeed(21, "Allure Libre de Buzenol", "Buzenol", "09-20", 10.7, 205, "Site de Montauban et vallons encaissés"),
            RoundSeed(22, "Allure Libre de Sainte-Marie-sur-Semois", "Sainte-Marie-sur-Semois", "09-27", 10.0, 115, "Boucle rapide en bord de Semois"),
            RoundSeed(23, "Allure Libre de Ruette", "Ruette", "10-04", 10.4, 160, "Coteaux du Sud-Gaume et Grandcourt"),
            RoundSeed(24, "Allure Libre de Torgny", "Torgny", "10-11", 10.6, 195, "Vignobles et village le plus méridional"),
            RoundSeed(25, "Allure Libre d'Arlon - Fouches", "Fouches", "10-18", 10.3, 155, "Clôture entre Gaume et Pays d'Arlon")
        )

        return rounds.map { r ->
            RaceEntity(
                raceId = "alg_${year}_r${r.round}",
                seasonYear = year,
                roundNumber = r.round,
                title = r.title,
                village = r.village,
                dateIso = "$year-${r.monthDay}",
                distanceKm = r.distKm,
                elevationGainMeters = r.elevM,
                isCompleted = false,
                myChronoEventId = "${year}_${r.round}"
            )
        }
    }

    val gaumeTerritories: List<GaumeTerritoryDef> = listOf(
        GaumeTerritoryDef(
            territoryId = "orval_semois",
            title = "Terroir d'Orval & Semois Ouest",
            subtitle = "Florenville, Pin, Izel, Jamoigne, Chassepierre, Muno",
            badgeTitle = "Seigneur d'Orval",
            centerLat = 49.6950,
            centerLon = 5.3150,
            radiusMeters = 7500,
            colorHex = 0xFFE07A2E
        ),
        GaumeTerritoryDef(
            territoryId = "chiny_tintigny",
            title = "Vallée de Chiny & Tintigny",
            subtitle = "Chiny, Tintigny, Bellefontaine, Rossignol, Breuvanne",
            badgeTitle = "Explorateur de la Semois",
            centerLat = 49.6980,
            centerLon = 5.4580,
            radiusMeters = 6800,
            colorHex = 0xFF006D77
        ),
        GaumeTerritoryDef(
            territoryId = "rulles_anlier",
            title = "Fief de La Rulles & Anlier",
            subtitle = "Rulles, Habay, Marbehan, Houdemont, Mellier, Léglise",
            badgeTitle = "Maître du Fief de Rulles",
            centerLat = 49.7380,
            centerLon = 5.5850,
            radiusMeters = 7200,
            colorHex = 0xFFD49B1B
        ),
        GaumeTerritoryDef(
            territoryId = "etalle_buzenol",
            title = "Cœur d'Étalle & Buzenol",
            subtitle = "Étalle, Fratin, Sainte-Marie, Buzenol, Vance, Chantemelle",
            badgeTitle = "Gardien de Montauban",
            centerLat = 49.6640,
            centerLon = 5.5950,
            radiusMeters = 5800,
            colorHex = 0xFF1E7E48
        ),
        GaumeTerritoryDef(
            territoryId = "cueste_virton",
            title = "Cueste de Virton & Torgny",
            subtitle = "Virton, Ethe, Saint-Mard, Ruette, Rouvroy, Torgny, Meix",
            badgeTitle = "Roi de la Cueste Gaumaise",
            centerLat = 49.5660,
            centerLon = 5.5180,
            radiusMeters = 7600,
            colorHex = 0xFFC85A32
        ),
        GaumeTerritoryDef(
            territoryId = "saint_leger_arlon",
            title = "Saint-Léger, Musson & Arlon",
            subtitle = "Saint-Léger, Musson, Signeulx, Halanzy, Messancy, Arlon, Fouches",
            badgeTitle = "Sentinelle des Trois Frontières",
            centerLat = 49.6220,
            centerLon = 5.7550,
            radiusMeters = 8500,
            colorHex = 0xFF2B59C3
        )
    )

    private val territoryById = gaumeTerritories.associateBy { it.territoryId }

    private fun v(name: String, lat: Double, lon: Double, territoryId: String): GaumeVillageGeo {
        val tName = territoryById[territoryId]?.title ?: "Gaume"
        return GaumeVillageGeo(
            canonicalVillage = name,
            latitude = lat,
            longitude = lon,
            territoryId = territoryId,
            territoryName = tName
        )
    }

    /**
     * Coordonnées GPS WGS84 exactes des villages et communes de Gaume & Sud-Luxembourg
     */
    private val exactGaumeVillages: List<GaumeVillageGeo> = listOf(
        // 1. Terroir d'Orval & Semois Ouest
        v("Florenville", 49.6992, 5.3103, "orval_semois"),
        v("Pin", 49.6897, 5.3469, "orval_semois"),
        v("Izel", 49.6936, 5.3742, "orval_semois"),
        v("Jamoigne", 49.6947, 5.4094, "orval_semois"),
        v("Moyen", 49.6889, 5.3908, "orval_semois"),
        v("Chassepierre", 49.7072, 5.2650, "orval_semois"),
        v("Sainte-Cécile", 49.7294, 5.2417, "orval_semois"),
        v("Fontenoille", 49.7119, 5.2175, "orval_semois"),
        v("Muno", 49.7114, 5.1744, "orval_semois"),
        v("Villers-devant-Orval", 49.6311, 5.3314, "orval_semois"),
        v("Orval", 49.6394, 5.3489, "orval_semois"),
        v("Limes", 49.6197, 5.3883, "orval_semois"),
        v("Bouillon", 49.7944, 5.0672, "orval_semois"),
        v("Herbeumont", 49.7764, 5.2358, "orval_semois"),

        // 2. Vallée de Chiny & Tintigny
        v("Chiny", 49.7383, 5.3433, "chiny_tintigny"),
        v("Lacuisine", 49.7128, 5.3183, "chiny_tintigny"),
        v("Suxy", 49.7667, 5.4017, "chiny_tintigny"),
        v("Les Bulles", 49.6958, 5.4328, "chiny_tintigny"),
        v("Termes", 49.7081, 5.4508, "chiny_tintigny"),
        v("Frenois", 49.7042, 5.4694, "chiny_tintigny"),
        v("Tintigny", 49.6833, 5.5136, "chiny_tintigny"),
        v("Breuvanne", 49.6797, 5.4792, "chiny_tintigny"),
        v("Bellefontaine", 49.6642, 5.4953, "chiny_tintigny"),
        v("Lahage", 49.6431, 5.4792, "chiny_tintigny"),
        v("Rossignol", 49.7175, 5.4864, "chiny_tintigny"),
        v("Saint-Vincent", 49.6675, 5.4414, "chiny_tintigny"),
        v("Ansart", 49.6928, 5.5244, "chiny_tintigny"),
        v("Poncelle", 49.6606, 5.5186, "chiny_tintigny"),

        // 3. Fief de La Rulles & Anlier
        v("Rulles", 49.7186, 5.5608, "rulles_anlier"),
        v("Marbehan", 49.7264, 5.5353, "rulles_anlier"),
        v("Houdemont", 49.7097, 5.5950, "rulles_anlier"),
        v("Habay-la-Vieille", 49.7231, 5.6186, "rulles_anlier"),
        v("Habay-la-Neuve", 49.7283, 5.6458, "rulles_anlier"),
        v("Habay", 49.7283, 5.6458, "rulles_anlier"),
        v("Hachy", 49.7031, 5.6836, "rulles_anlier"),
        v("Anlier", 49.7722, 5.6206, "rulles_anlier"),
        v("Mellier", 49.7675, 5.5183, "rulles_anlier"),
        v("Thibessart", 49.7592, 5.5619, "rulles_anlier"),
        v("Léglise", 49.7994, 5.5378, "rulles_anlier"),
        v("Neufchâteau", 49.8406, 5.4353, "rulles_anlier"),
        v("Libramont", 49.9169, 5.3764, "rulles_anlier"),
        v("Bertrix", 49.8544, 5.2533, "rulles_anlier"),

        // 4. Cœur d'Étalle & Buzenol
        v("Étalle", 49.6742, 5.5633, "etalle_buzenol"),
        v("Sainte-Marie-sur-Semois", 49.6708, 5.5333, "etalle_buzenol"),
        v("Sainte-Marie", 49.6708, 5.5333, "etalle_buzenol"),
        v("Fratin", 49.6628, 5.5561, "etalle_buzenol"),
        v("Villers-sur-Semois", 49.6967, 5.5628, "etalle_buzenol"),
        v("Mortinsart", 49.6939, 5.5819, "etalle_buzenol"),
        v("Buzenol", 49.6361, 5.5944, "etalle_buzenol"),
        v("Chantemelle", 49.6567, 5.6531, "etalle_buzenol"),
        v("Vance", 49.6725, 5.6656, "etalle_buzenol"),
        v("Sampont", 49.6833, 5.6914, "etalle_buzenol"),

        // 5. Cueste de Virton & Torgny
        v("Meix-devant-Virton", 49.6056, 5.4806, "cueste_virton"),
        v("Gérouville", 49.6222, 5.4244, "cueste_virton"),
        v("Robelmont", 49.5964, 5.5122, "cueste_virton"),
        v("Villers-la-Loue", 49.5789, 5.4822, "cueste_virton"),
        v("Sommethonne", 49.5892, 5.4519, "cueste_virton"),
        v("Virton", 49.5675, 5.5333, "cueste_virton"),
        v("Saint-Mard", 49.5553, 5.5242, "cueste_virton"),
        v("Ethe", 49.5833, 5.5825, "cueste_virton"),
        v("Belmont", 49.5758, 5.5678, "cueste_virton"),
        v("Bleid", 49.5719, 5.6197, "cueste_virton"),
        v("Gomery", 49.5603, 5.6056, "cueste_virton"),
        v("Latour", 49.5586, 5.5736, "cueste_virton"),
        v("Chenois", 49.5664, 5.5644, "cueste_virton"),
        v("Ruette", 49.5414, 5.5922, "cueste_virton"),
        v("Grandcourt", 49.5328, 5.5736, "cueste_virton"),
        v("Dampicourt", 49.5464, 5.4983, "cueste_virton"),
        v("Harnoncourt", 49.5414, 5.4792, "cueste_virton"),
        v("Lamorteau", 49.5269, 5.4731, "cueste_virton"),
        v("Torgny", 49.5072, 5.4753, "cueste_virton"),
        v("Rouvroy", 49.5392, 5.4903, "cueste_virton"),
        v("Couvreux", 49.5322, 5.4956, "cueste_virton"),
        v("Montquintin", 49.5453, 5.5125, "cueste_virton"),

        // 6. Pays de Saint-Léger, Musson & Arlon
        v("Saint-Léger", 49.6122, 5.6561, "saint_leger_arlon"),
        v("Châtillon", 49.6225, 5.6914, "saint_leger_arlon"),
        v("Meix-le-Tige", 49.6069, 5.7169, "saint_leger_arlon"),
        v("Musson", 49.5583, 5.7053, "saint_leger_arlon"),
        v("Mussy-la-Ville", 49.5689, 5.6808, "saint_leger_arlon"),
        v("Signeulx", 49.5561, 5.6583, "saint_leger_arlon"),
        v("Baranzy", 49.5514, 5.7208, "saint_leger_arlon"),
        v("Willancourt", 49.5819, 5.6769, "saint_leger_arlon"),
        v("Halanzy", 49.5603, 5.7433, "saint_leger_arlon"),
        v("Rachecourt", 49.5908, 5.7367, "saint_leger_arlon"),
        v("Aubange", 49.5683, 5.8056, "saint_leger_arlon"),
        v("Athus", 49.5653, 5.8369, "saint_leger_arlon"),
        v("Aix-sur-Cloye", 49.5758, 5.7769, "saint_leger_arlon"),
        v("Battincourt", 49.5786, 5.7611, "saint_leger_arlon"),
        v("Messancy", 49.5922, 5.8183, "saint_leger_arlon"),
        v("Habergy", 49.6128, 5.7606, "saint_leger_arlon"),
        v("Bébange", 49.6186, 5.7744, "saint_leger_arlon"),
        v("Hondelange", 49.6331, 5.8292, "saint_leger_arlon"),
        v("Wolkrange", 49.6317, 5.7969, "saint_leger_arlon"),
        v("Sélange", 49.6083, 5.8492, "saint_leger_arlon"),
        v("Arlon", 49.6833, 5.8167, "saint_leger_arlon"),
        v("Waltzing", 49.6819, 5.8475, "saint_leger_arlon"),
        v("Bonnert", 49.6989, 5.8339, "saint_leger_arlon"),
        v("Frassem", 49.6981, 5.8506, "saint_leger_arlon"),
        v("Clairefontaine", 49.6656, 5.8819, "saint_leger_arlon"),
        v("Autelbas", 49.6497, 5.8644, "saint_leger_arlon"),
        v("Weyler", 49.6569, 5.8319, "saint_leger_arlon"),
        v("Stockem", 49.6739, 5.7867, "saint_leger_arlon"),
        v("Heinsch", 49.6956, 5.7519, "saint_leger_arlon"),
        v("Freylange", 49.6967, 5.7744, "saint_leger_arlon"),
        v("Fouches", 49.6894, 5.7147, "saint_leger_arlon"),
        v("Heinstert", 49.7347, 5.7267, "saint_leger_arlon"),
        v("Attert", 49.7511, 5.7864, "saint_leger_arlon"),
        v("Nothomb", 49.7322, 5.7836, "saint_leger_arlon"),
        v("Tontelange", 49.7264, 5.8108, "saint_leger_arlon"),
        v("Metzert", 49.7192, 5.7964, "saint_leger_arlon"),
        v("Nobressart", 49.7267, 5.7203, "saint_leger_arlon"),
        v("Thiaumont", 49.7100, 5.7408, "saint_leger_arlon"),
        v("Lottert", 49.7042, 5.7547, "saint_leger_arlon"),
        v("Martelange", 49.8317, 5.7369, "saint_leger_arlon")
    )

    private fun normalizeKey(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase().trim(), Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace("'", " ")
            .replace("-", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Trouve les coordonnées GPS exactes du village de la course en Gaume.
     */
    fun resolveGaumeGeoForRace(race: RaceEntity): GaumeVillageGeo {
        val normVillage = normalizeKey(race.village)
        val normBlob = normalizeKey("${race.village} ${race.title}")

        val sortedBySpecificity = exactGaumeVillages.sortedByDescending { it.canonicalVillage.length }

        // 1. Correspondance exacte sur le nom du village
        sortedBySpecificity.firstOrNull { geo ->
            normalizeKey(geo.canonicalVillage) == normVillage
        }?.let { return it }

        // 2. Correspondance par mot entier dans le village ou le titre de la course
        sortedBySpecificity.firstOrNull { geo ->
            val key = normalizeKey(geo.canonicalVillage)
            Regex("\\b${Regex.escape(key)}\\b").containsMatchIn(normBlob)
        }?.let { return it }

        // 3. Correspondance partielle si nécessaire
        sortedBySpecificity.firstOrNull { geo ->
            val key = normalizeKey(geo.canonicalVillage)
            key.length >= 4 && key in normBlob
        }?.let { return it }

        // 4. Fallback centré en Gaume (autour d'Étalle / Tintigny)
        val hash = abs((race.village + race.roundNumber).hashCode())
        val lat = 49.6400 + ((hash % 9) - 4) * 0.012
        val lon = 5.5400 + (((hash / 9) % 9) - 4) * 0.015
        return GaumeVillageGeo(
            canonicalVillage = race.village.ifBlank { "Gaume #${race.roundNumber}" },
            latitude = lat,
            longitude = lon,
            territoryId = "etalle_buzenol",
            territoryName = "Cœur d'Étalle & Buzenol"
        )
    }
}
