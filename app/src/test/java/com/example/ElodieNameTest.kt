package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AlgDatabase
import com.example.data.local.GaumeInitialSeedData
import com.example.data.repository.AlgRepository
import com.example.data.repository.UserDataSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Le prénom s'écrit « Elodie », sans accent, y compris sur un téléphone ou une sauvegarde qui avait « Élodie ». */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ElodieNameTest {

    private fun newDb(): AlgDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(), AlgDatabase::class.java
    ).allowMainThreadQueries().build()

    private fun repository(db: AlgDatabase) =
        AlgRepository(algDao = db.algDao(), today = { LocalDate.of(2026, 10, 27) }, database = db)

    private suspend fun elodieName(db: AlgDatabase) =
        db.algDao().getRunnerProfilesSnapshot().single { it.profileId == "elodie" }.displayName

    private fun withAccent() =
        GaumeInitialSeedData.buildInitialProfiles().map { if (it.profileId == "elodie") it.copy(displayName = "Élodie") else it }

    @Test
    fun newPhone_isSeededWithoutAccent() = runBlocking {
        val db = newDb()
        repository(db).ensureSeeded()
        assertEquals("Elodie", elodieName(db))
        db.close()
    }

    @Test
    fun existingProfileWithAccent_isRenamedAtStartup() = runBlocking {
        val db = newDb()
        db.algDao().upsertRunnerProfiles(withAccent())
        repository(db).ensureSeeded()
        assertEquals("Elodie", elodieName(db))
        assertEquals("Laetitia", db.algDao().getRunnerProfilesSnapshot().single { it.profileId == "laetitia" }.displayName)
        db.close()
    }

    @Test
    fun oldBackupWithAccent_isRenamedOnRestore() = runBlocking {
        val db = newDb()
        val repo = repository(db)
        repo.ensureSeeded()
        repo.restoreUserData(UserDataSnapshot(profiles = withAccent()))
        assertEquals("Elodie", elodieName(db))
        db.close()
    }
}
