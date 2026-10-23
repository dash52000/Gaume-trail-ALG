package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlgDao {
    @Query("SELECT * FROM runner_profiles ORDER BY CASE profileId WHEN 'me' THEN 1 WHEN 'laetitia' THEN 2 WHEN 'elodie' THEN 3 ELSE 4 END")
    fun observeRunnerProfiles(): Flow<List<RunnerProfileEntity>>

    @Query("SELECT * FROM runner_profiles")
    suspend fun getRunnerProfilesSnapshot(): List<RunnerProfileEntity>

    @Query("SELECT * FROM runner_profiles WHERE profileId = :profileId LIMIT 1")
    suspend fun getRunnerProfile(profileId: String): RunnerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRunnerProfiles(profiles: List<RunnerProfileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRunnerProfile(profile: RunnerProfileEntity)

    @Query("SELECT * FROM races WHERE seasonYear = :seasonYear AND seasonYear >= 2026 ORDER BY roundNumber ASC")
    fun observeRacesBySeason(seasonYear: Int): Flow<List<RaceEntity>>

    @Query("SELECT * FROM races WHERE seasonYear >= 2026 ORDER BY seasonYear DESC, roundNumber ASC")
    fun observeAllRaces(): Flow<List<RaceEntity>>

    @Query("SELECT * FROM races WHERE seasonYear = :seasonYear AND seasonYear >= 2026 ORDER BY roundNumber ASC")
    suspend fun getRacesBySeasonSnapshot(seasonYear: Int): List<RaceEntity>

    @Query("SELECT * FROM races WHERE raceId = :raceId LIMIT 1")
    suspend fun getRaceById(raceId: String): RaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRaces(races: List<RaceEntity>)

    @Query("DELETE FROM races WHERE seasonYear < 2026")
    suspend fun deletePre2026Races()

    @Query("DELETE FROM races WHERE seasonYear = :seasonYear")
    suspend fun deleteRacesBySeason(seasonYear: Int)

    @Query("SELECT * FROM race_results WHERE seasonYear = :seasonYear AND seasonYear >= 2026")
    fun observeResultsBySeason(seasonYear: Int): Flow<List<RaceResultEntity>>

    @Query("SELECT * FROM race_results WHERE seasonYear >= 2026")
    fun observeAllResults(): Flow<List<RaceResultEntity>>

    @Query("SELECT * FROM race_results")
    suspend fun getAllResultsSnapshot(): List<RaceResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRaceResults(results: List<RaceResultEntity>)

    @Query("DELETE FROM race_results WHERE seasonYear < 2026")
    suspend fun deletePre2026Results()

    @Query("DELETE FROM race_results WHERE seasonYear = :seasonYear")
    suspend fun deleteResultsBySeason(seasonYear: Int)

    @Query("DELETE FROM race_results WHERE profileId = :profileId")
    suspend fun deleteResultsForProfile(profileId: String)

    @Query("DELETE FROM race_results WHERE profileId = :profileId AND distanceLabel = :distanceLabel")
    suspend fun deleteResultsForProfileAndDistance(profileId: String, distanceLabel: String)

    @Query("DELETE FROM race_results WHERE raceId = :raceId AND profileId = :profileId")
    suspend fun deleteResultForRaceAndProfile(raceId: String, profileId: String)

    @Query("DELETE FROM race_results WHERE raceId = :raceId AND profileId = :profileId AND distanceLabel = :distanceLabel")
    suspend fun deleteResultForRaceProfileAndDistance(raceId: String, profileId: String, distanceLabel: String)

    @Query("DELETE FROM race_results")
    suspend fun deleteAllRaceResults()

    @Query("SELECT * FROM race_bib_overrides")
    fun observeAllBibOverrides(): Flow<List<RaceBibOverrideEntity>>

    @Query("SELECT * FROM race_bib_overrides WHERE profileId = :profileId")
    suspend fun getBibOverridesForProfile(profileId: String): List<RaceBibOverrideEntity>

    @Query("SELECT * FROM race_bib_overrides WHERE raceId = :raceId AND profileId = :profileId LIMIT 1")
    suspend fun getBibOverride(raceId: String, profileId: String): RaceBibOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBibOverride(overrideEntity: RaceBibOverrideEntity)

    @Query("DELETE FROM race_bib_overrides WHERE raceId = :raceId AND profileId = :profileId")
    suspend fun deleteBibOverride(raceId: String, profileId: String)

    @Query("SELECT * FROM activity_metrics")
    fun observeAllActivityMetrics(): Flow<List<ActivityMetricsEntity>>

    @Query("SELECT * FROM activity_metrics WHERE raceId = :raceId AND profileId = :profileId LIMIT 1")
    suspend fun getActivityMetrics(raceId: String, profileId: String): ActivityMetricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivityMetrics(metrics: List<ActivityMetricsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSingleActivityMetric(metric: ActivityMetricsEntity)

    @Query("SELECT * FROM race_terrain_weather")
    fun observeAllTerrainWeather(): Flow<List<RaceTerrainWeatherEntity>>

    @Query("SELECT * FROM race_terrain_weather WHERE raceId = :raceId AND profileId = :profileId LIMIT 1")
    suspend fun getTerrainWeather(raceId: String, profileId: String): RaceTerrainWeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTerrainWeather(entity: RaceTerrainWeatherEntity)

    @Query("SELECT * FROM mychrono_directory ORDER BY bestPoints DESC")
    fun observeDirectory(): Flow<List<MyChronoDirectoryEntry>>

    @Query("SELECT * FROM mychrono_directory WHERE fullName LIKE '%' || :query || '%' OR club LIKE '%' || :query || '%' OR myChronoRunnerId LIKE '%' || :query || '%' OR defaultBib10km LIKE '%' || :query || '%' OR defaultBib5km LIKE '%' || :query || '%' ORDER BY bestPoints DESC")
    suspend fun searchDirectory(query: String): List<MyChronoDirectoryEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDirectoryEntries(entries: List<MyChronoDirectoryEntry>)
}
