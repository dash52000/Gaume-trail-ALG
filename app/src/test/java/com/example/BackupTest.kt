package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ActivityMetricsEntity
import com.example.data.local.AlgDatabase
import com.example.data.local.GaumeInitialSeedData
import com.example.data.local.RaceBibOverrideEntity
import com.example.data.local.RaceTerrainWeatherEntity
import com.example.data.local.SeasonBibEntity
import com.example.data.local.SeasonGoalEntity
import com.example.data.repository.AlgRepository
import com.example.data.repository.UserDataBackup
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** La sauvegarde rend exactement les saisies d'origine, sur un téléphone vide. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupTest {

    private fun newDb(): AlgDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(), AlgDatabase::class.java
    ).allowMainThreadQueries().build()

    private fun repository(db: AlgDatabase) =
        AlgRepository(algDao = db.algDao(), today = { LocalDate.of(2026, 10, 26) }, database = db)

    @Test
    fun backup_roundTripsEverythingTheRunnerEntered() = runBlocking {
        val source = newDb()
        val dao = source.algDao()
        dao.upsertRunnerProfiles(GaumeInitialSeedData.buildInitialProfiles())
        dao.upsertSeasonBib(SeasonBibEntity("me", 2026, "697", "1150"))
        dao.upsertSeasonBib(SeasonBibEntity("laetitia", 2026, "", "1075"))
        dao.upsertBibOverride(RaceBibOverrideEntity("alg_2026_r05", "me", "1150", 5))
        dao.upsertTerrainWeather(
            RaceTerrainWeatherEntity("alg_2026_r41", "me", "Boueux", "Gras en forêt", false, 10, null)
        )
        dao.upsertTerrainWeather(
            RaceTerrainWeatherEntity("alg_2026_r42", "laetitia", "", "", true, 5, 1110)
        )
        dao.upsertSeasonGoal(SeasonGoalEntity("me", 2027, "races", 20))
        dao.upsertActivityMetrics(
            listOf(ActivityMetricsEntity("alg_2026_r41", "me", "GPX", "Virton", 10.38, 118, 152, 171, null, "49.5,5.5,300;49.6,5.5,310"))
        )
        val original = repository(source).userDataSnapshot()
        val text = UserDataBackup.toJson(original, 1_790_000_000_000L, "1.2.0")
        source.close()

        val target = newDb()
        val message = repository(target).restoreUserData(UserDataBackup.fromJson(text))
        assertTrue(message, message.contains("3 profil(s)"))

        val restored = repository(target).userDataSnapshot()
        assertEquals(original.profiles.sortedBy { it.profileId }, restored.profiles.sortedBy { it.profileId })
        assertEquals(original.seasonBibs.toSet(), restored.seasonBibs.toSet())
        assertEquals(original.bibOverrides, restored.bibOverrides)
        assertEquals(original.terrainNotes.toSet(), restored.terrainNotes.toSet())
        assertEquals(original.goals, restored.goals)
        assertEquals(original.activities, restored.activities)
        target.close()
    }

    @Test
    fun anotherFile_isRefused() {
        assertThrows(IllegalArgumentException::class.java) { UserDataBackup.fromJson("""{"races":[]}""") }
        assertThrows(IllegalArgumentException::class.java) { UserDataBackup.fromJson("pas du json") }
    }
}
