package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AlgDatabase
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Vérifie que la mise à jour de la base (version 6 → 7) conserve les saisies du téléphone
 * et ne retire que les valeurs inventées par l'ancienne version.
 * La base version 6 est recréée à partir du schéma exporté (app/schemas/.../6.json).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseMigrationTest {

    private val dbName = "migration-test.db"
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    private fun createVersion6Database() {
        val schema = JSONObject(File("schemas/com.example.data.local.AlgDatabase/6.json").readText())
            .getJSONObject("database")
        val db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(dbName).apply { parentFile?.mkdirs() }, null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val e = entities.getJSONObject(i)
            db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", e.getString("tableName")))
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) db.execSQL(setup.getString(i))

        // Données telles qu'un téléphone avec l'ancienne version peut les contenir.
        db.execSQL(
            "INSERT INTO runner_profiles VALUES ('me','Damien Collet','1171','697','',10,'Seniors H','Gaume Trail',${0xFF1B5E3A},NULL)"
        )
        db.execSQL(
            "INSERT INTO runner_profiles VALUES ('laetitia','Laetitia','1075','','1075',5,'Dames','Gaume Trail',${0xFFC85A32},NULL)"
        )
        db.execSQL(
            "INSERT INTO runner_profiles VALUES ('elodie','Élodie','1076','','1076',5,'Dames','Gaume Trail',${0xFF006D77},NULL)"
        )
        // Manche synchronisée depuis MyChrono (identifiant numérique) avec le D+ par défaut de 150 m.
        db.execSQL(
            "INSERT INTO races VALUES ('alg_2026_r41',2026,41,'41','Virton — Le Jogging du TT Virton','Virton','2026-10-18',10.4,150,1,0)"
        )
        // Manche du faux calendrier d'origine (identifiant "année_numéro").
        db.execSQL(
            "INSERT INTO races VALUES ('alg_2027_r1',2027,1,'2027_1','Allure Libre de Virton','Virton','2027-03-08',10.2,145,0,0)"
        )
        db.execSQL(
            "INSERT INTO race_results VALUES ('alg_2026_r41','me',2026,'697','10 km',3254,'54:14','5:13 /km',11.5,75,209,27,70,1126)"
        )
        db.execSQL("INSERT INTO race_bib_overrides VALUES ('alg_2026_r41','laetitia','1075',5)")
        db.execSQL(
            "INSERT INTO activity_metrics VALUES ('alg_2026_r41','me','GPX','Virton',10.38,118,152,171,174,'49.5,5.5,300,')"
        )
        db.execSQL(
            "INSERT INTO race_terrain_weather VALUES ('alg_2026_r41','me','Boueux','Pluie',9,'Gras en forêt',0,10,NULL)"
        )
        db.execSQL("INSERT INTO mychrono_directory VALUES ('1075','Laetitia','','1075','Gaume Trail','Dames',0,0)")
        db.version = 6
        db.close()
    }

    @Test
    fun migration6To7_keepsUserDataAndRemovesInventedValues() = runBlocking {
        createVersion6Database()

        val room = Room.databaseBuilder(context, AlgDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        val dao = room.algDao()

        val profiles = dao.getRunnerProfilesSnapshot().associateBy { it.profileId }
        assertEquals("1171", profiles.getValue("me").myChronoRunnerId)
        assertEquals("697", profiles.getValue("me").defaultBib10km)
        assertEquals("1202", profiles.getValue("laetitia").myChronoRunnerId)
        assertEquals("1075", profiles.getValue("laetitia").defaultBib5km)
        assertEquals("1189", profiles.getValue("elodie").myChronoRunnerId)

        val race41 = dao.getRaceById("alg_2026_r41")
        assertNotNull(race41)
        assertNull(race41!!.elevationGainMeters)
        assertEquals(10.4, race41.distanceKm, 0.001)
        assertNull(dao.getRaceById("alg_2027_r1"))

        assertEquals(1126, dao.getAllResultsSnapshot().single().challengePoints)
        assertEquals("1075", dao.getBibOverride("alg_2026_r41", "laetitia")?.bibNumber)

        val metrics = dao.getActivityMetrics("alg_2026_r41", "me")!!
        assertEquals(118, metrics.elevationGainMeters)
        assertNull(metrics.cadenceSpm)

        assertEquals("Gras en forêt", dao.getTerrainWeather("alg_2026_r41", "me")?.terrainNotes)
        assertEquals(0, dao.searchDirectory("Laetitia").size)

        dao.upsertStandings(emptyList()) // la nouvelle table existe
        room.close()
    }

    @Test
    fun freshInstall_opensVersion7() = runBlocking {
        val room = Room.databaseBuilder(context, AlgDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        assertEquals(0, room.algDao().getRunnerProfilesSnapshot().size)
        room.close()
    }
}
