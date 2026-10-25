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

    @Query("SELECT * FROM races WHERE seasonYear = :seasonYear AND seasonYear >= 2025 ORDER BY roundNumber ASC")
    fun observeRacesBySeason(seasonYear: Int): Flow<List<RaceEntity>>

    @Query("SELECT * FROM races WHERE seasonYear >= 2025 ORDER BY seasonYear DESC, roundNumber ASC")
    fun observeAllRaces(): Flow<List<RaceEntity>>

    @Query("SELECT * FROM races WHERE seasonYear = :seasonYear AND seasonYear >= 2025 ORDER BY roundNumber ASC")
    suspend fun getRacesBySeasonSnapshot(seasonYear: Int): List<RaceEntity>

    @Query("SELECT * FROM races WHERE raceId = :raceId LIMIT 1")
    suspend fun getRaceById(raceId: String): RaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRaces(races: List<RaceEntity>)

    @Query("DELETE FROM races WHERE seasonYear < 2025")
    suspend fun deletePre2025Races()

    @Query("SELECT * FROM races WHERE seasonYear >= 2025")
    suspend fun getAllRacesSnapshot(): List<RaceEntity>

    @Query("DELETE FROM races WHERE seasonYear = :seasonYear")
    suspend fun deleteRacesBySeason(seasonYear: Int)

    @Query("SELECT * FROM race_results WHERE seasonYear = :seasonYear AND seasonYear >= 2025")
    fun observeResultsBySeason(seasonYear: Int): Flow<List<RaceResultEntity>>

    @Query("SELECT * FROM race_results WHERE seasonYear >= 2025")
    fun observeAllResults(): Flow<List<RaceResultEntity>>

    @Query("SELECT * FROM race_results")
    suspend fun getAllResultsSnapshot(): List<RaceResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRaceResults(results: List<RaceResultEntity>)

    @Query("DELETE FROM race_results WHERE seasonYear < 2025")
    suspend fun deletePre2025Results()

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

    @Query("SELECT * FROM challenge_standings WHERE seasonYear >= 2025")
    fun observeAllStandings(): Flow<List<ChallengeStandingEntity>>

    @Query("DELETE FROM challenge_standings WHERE seasonYear = :seasonYear")
    suspend fun deleteStandingsBySeason(seasonYear: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStandings(standings: List<ChallengeStandingEntity>)

    @Query("SELECT * FROM race_bib_overrides")
    suspend fun getAllBibOverridesSnapshot(): List<RaceBibOverrideEntity>

    // --- Dossards par saison ---
    @Query("SELECT * FROM season_bibs")
    fun observeSeasonBibs(): Flow<List<SeasonBibEntity>>

    @Query("SELECT * FROM season_bibs")
    suspend fun getSeasonBibsSnapshot(): List<SeasonBibEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSeasonBib(entity: SeasonBibEntity)

    // --- Résultats bruts MyChrono (cache) ---
    @Query("SELECT * FROM mychrono_rows WHERE seasonYear = :seasonYear")
    suspend fun getRowsForSeason(seasonYear: Int): List<MyChronoRowEntity>

    @Query("SELECT * FROM mychrono_rows WHERE raceId = :raceId")
    suspend fun getRowsForRace(raceId: String): List<MyChronoRowEntity>

    @Query("DELETE FROM mychrono_rows WHERE raceId = :raceId")
    suspend fun deleteRowsForRace(raceId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRows(rows: List<MyChronoRowEntity>)

    @Query("SELECT DISTINCT seasonYear FROM mychrono_rows")
    suspend fun getSeasonsWithRows(): List<Int>

    @Query("SELECT * FROM race_fetch_state")
    suspend fun getFetchStates(): List<RaceFetchStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFetchState(state: RaceFetchStateEntity)

    // --- Indices de difficulté des parcours ---
    @Query("SELECT * FROM course_factors")
    fun observeCourseFactors(): Flow<List<CourseFactorEntity>>

    @Query("DELETE FROM course_factors WHERE seasonYear = :seasonYear")
    suspend fun deleteCourseFactorsBySeason(seasonYear: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCourseFactors(factors: List<CourseFactorEntity>)

    // --- Météo réelle ---
    @Query("SELECT * FROM race_weather")
    fun observeRaceWeather(): Flow<List<RaceWeatherEntity>>

    @Query("SELECT * FROM race_weather")
    suspend fun getRaceWeatherSnapshot(): List<RaceWeatherEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRaceWeather(entity: RaceWeatherEntity)
}
