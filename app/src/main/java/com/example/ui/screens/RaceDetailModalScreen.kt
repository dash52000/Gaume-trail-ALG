package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.RaceBibOverrideEntity
import com.example.data.local.RaceResultEntity
import com.example.data.local.RunnerProfileEntity
import com.example.domain.GpxAndTrackUtils
import com.example.domain.RaceItemWithContext
import com.example.domain.RunnerChallengeSummary
import com.example.ui.components.GaumeGpsAndElevationCanvas
import com.example.ui.components.GaumePerfsCalculator
import com.example.ui.components.GaumeRacePronosticEntry
import com.example.ui.components.RaceConditionsCard
import com.example.ui.components.RacePracticalInfoCard
import com.example.ui.components.RacePronosticVsActualMiniBanner
import com.example.ui.components.openDirectionsTo
import com.example.ui.components.Top15StatusBadge
import com.example.ui.theme.ClayTerracotta
import com.example.ui.theme.CountingTop15BadgeGreen
import com.example.ui.theme.GoldPodium
import com.example.ui.theme.SemoisRiverTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceDetailModalScreen(
    raceItem: RaceItemWithContext,
    activeProfile: RunnerProfileEntity?,
    profiles: List<RunnerProfileEntity>,
    trioSummaries: Map<String, RunnerChallengeSummary>,
    bibOverridesForRace: Map<String, RaceBibOverrideEntity>,
    selectedDistanceKm: Int,
    isSyncingMyChrono: Boolean,
    onClose: () -> Unit,
    onOpenShareCardForRace: (String) -> Unit = {},
    onSaveRaceBibAndDistance: (profileId: String, bibNumber: String, distanceKm: Int) -> Unit,
    onSaveRaceTerrainAndWeather: (
        profileId: String,
        terrainCondition: String,
        terrainNotes: String,
        isPlannedForSimulator: Boolean,
        plannedDistanceKm: Int,
        simulatedTargetPoints: Int?
    ) -> Unit,
    onSelectRunnerProfile: (String) -> Unit,
    onImportGpxFile: (Uri) -> Unit,
    courseRecord: com.example.domain.CourseRecord? = null
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val race = raceItem.race
    val activeProfileId = activeProfile?.profileId ?: "me"
    val existingOverride = bibOverridesForRace[activeProfileId]

    val initialDistKm = existingOverride?.distanceKm
        ?: if (raceItem.resultForDistance(selectedDistanceKm) != null) selectedDistanceKm
        else if (raceItem.myResult10km != null) 10
        else if (raceItem.myResult5km != null) 5
        else selectedDistanceKm

    var distanceKm by remember(activeProfileId, existingOverride, selectedDistanceKm) {
        mutableIntStateOf(initialDistKm)
    }

    val currentResult = raceItem.resultForDistance(distanceKm)
    val isCountingInTop15 = raceItem.isCountingForDistance(distanceKm)
    val rankForDist = raceItem.rankForDistance(distanceKm)

    val initialBib = existingOverride?.bibNumber
        ?: currentResult?.bibNumber
        ?: if (distanceKm == 5) {
            activeProfile?.defaultBib5km.orEmpty()
        } else {
            activeProfile?.defaultBib10km.orEmpty()
        }

    var bibInput by remember(activeProfileId, distanceKm, existingOverride, currentResult) {
        mutableStateOf(initialBib)
    }

    val tw = raceItem.terrainWeather
    var terrainCondition by remember(activeProfileId, tw) {
        mutableStateOf(tw?.terrainCondition.orEmpty())
    }
    val isChallenge = race.isChallenge
    val canPlanInSimulator = isChallenge && !race.isCompleted
    var terrainNotes by remember(activeProfileId, tw) {
        mutableStateOf(tw?.terrainNotes.orEmpty())
    }
    var isPlannedSimulator by remember(activeProfileId, tw) {
        mutableStateOf(tw?.isPlannedForSimulator == true)
    }
    // Objectif par défaut du simulateur : ta moyenne réelle de points sur cette distance (0 sans historique).
    val averagePointsForDistance = trioSummaries[activeProfileId]
        ?.challengeForDistance(distanceKm)?.averagePointsPerRace ?: 0
    var simulatedTargetPointsInput by remember(activeProfileId, tw, averagePointsForDistance) {
        mutableStateOf((tw?.simulatedTargetPoints ?: averagePointsForDistance).toString())
    }

    // Accordéons repliables : fermés par défaut si la course a déjà un résultat officiel
    var showBibAccordion by remember(race.raceId, activeProfileId) {
        mutableStateOf(currentResult == null)
    }
    var showTerrainAccordion by remember(race.raceId) {
        mutableStateOf(false)
    }

    val gpxPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportGpxFile(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = (if (isChallenge) "Manche #${race.roundNumber}" else "Hors challenge") + " • ${race.title}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${raceItem.gaumeGeo.canonicalVillage} (${raceItem.gaumeGeo.territoryName}) • ${race.dateIso}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_race_detail_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val la = raceItem.gaumeGeo.latitude
                            val lo = raceItem.gaumeGeo.longitude
                            if (la != null && lo != null) openDirectionsTo(context, la, lo)
                        },
                        enabled = raceItem.gaumeGeo.hasKnownLocation,
                        modifier = Modifier.testTag("race_detail_gps_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Itinéraire GPS",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. SÉLECTEUR RAPIDE COUREUR + DISTANCE (10 km / 5 km)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                profiles.forEach { profile ->
                    val isSelected = profile.profileId == activeProfileId
                    val color = Color(profile.avatarColorHex)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectRunnerProfile(profile.profileId) }
                            .testTag("detail_runner_chip_${profile.profileId}"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = profile.displayName.substringBefore(" "),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Bascule rapide 10k / 5k
                listOf(10, 5).forEach { dist ->
                    val isSel = distanceKm == dist
                    Surface(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                distanceKm = dist
                                if (dist == 10) {
                                    bibInput = raceItem.myResult10km?.bibNumber ?: activeProfile?.defaultBib10km.orEmpty()
                                } else {
                                    bibInput = raceItem.myResult5km?.bibNumber ?: activeProfile?.defaultBib5km.orEmpty()
                                }
                            }
                            .testTag("race_detail_dist_${dist}km"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = race.distanceNameFor(dist),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. PRIORITÉ #1 EN HAUT : RÉSULTAT OFFICIEL MYCHRONO + PRONOSTIC (PRÉVU VS RÉEL) + BOUTON CARTE BILAN
            val activeRunnerSummary = trioSummaries[activeProfileId]
            val pronosticEntry = remember(activeRunnerSummary, distanceKm, raceItem.race.raceId) {
                activeRunnerSummary?.let { summary ->
                    GaumePerfsCalculator.computeSeasonPronostics(summary, distanceKm)
                        .allPronosticsByRaceId[raceItem.race.raceId]
                }
            }

            OfficialMyChronoResultCard(
                runnerName = activeProfile?.displayName ?: "Moi",
                distanceKm = distanceKm,
                distanceName = race.distanceNameFor(distanceKm),
                isChallenge = isChallenge,
                result = currentResult,
                pronosticEntry = pronosticEntry,
                isCountingInTop15 = isCountingInTop15,
                rankAmongMyRaces = rankForDist,
                onShareCardClick = { onOpenShareCardForRace(race.raceId) },
                onExpandBibSearch = { showBibAccordion = true }
            )

            // 2 bis. FICHE PRATIQUE (adresse, horaires, terrain, équipements, tracés, itinéraire)
            RacePracticalInfoCard(
                race = race,
                geo = raceItem.gaumeGeo
            )

            // 3. CONDITIONS : MÉTÉO RÉELLE, INDICE DU PARCOURS, TERRAIN NOTÉ
            RaceConditionsCard(
                raceItem = raceItem,
                distanceKm = distanceKm,
                terrainCondition = terrainCondition,
                record = courseRecord
            )

            // 4. COMPARAISON DU TRIO SUR CETTE MANCHE
            TrioRaceComparisonSection(
                raceItem = raceItem,
                profiles = profiles,
                trioSummaries = trioSummaries
            )

            // 5. TRACE GPS, PROFIL ALTIMÉTRIQUE & IMPORT .GPX LOCAL
            WatchAndGpsSection(
                raceItem = raceItem,
                onPickGpxFile = { gpxPickerLauncher.launch(arrayOf("*/*")) }
            )

            // 6. ACCORDÉON REPLIABLE 1 : RECHERCHER / MODIFIER DOSSARD MYCHRONO (10 KM / 5 KM)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("race_bib_distance_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showBibAccordion = !showBibAccordion }
                            .padding(14.dp)
                            .testTag("toggle_bib_accordion"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Dossard & Recherche MyChrono (${distanceKm} km)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (bibInput.isNotBlank()) "Dossard #$bibInput • Touchez pour modifier" else "Aucun dossard • Touchez pour chercher",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            text = if (showBibAccordion) "Replier ▲" else "Ouvrir ▼",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(visible = showBibAccordion) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            Text(
                                text = "Les résultats du 10 km et du 5 km sont stockés séparément pour leur Challenge respectif.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = bibInput,
                                    onValueChange = { bibInput = it },
                                    label = { Text("N° de Dossard ($distanceKm km)", maxLines = 1) },
                                    placeholder = { Text("Ex: 697 ou 1150") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("race_detail_bib_input")
                                )

                                Button(
                                    onClick = {
                                        onSaveRaceBibAndDistance(activeProfileId, bibInput, distanceKm)
                                    },
                                    enabled = !isSyncingMyChrono && bibInput.isNotBlank(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("race_detail_fetch_mychrono_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isSyncingMyChrono) "Sync…" else "Chercher",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 7. ACCORDÉON REPLIABLE 2 : MÉTÉO, ÉTAT DU TERRAIN (BOUE/SEC) & PLANIFICATION SIMULATEUR TOP 15
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("race_terrain_weather_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTerrainAccordion = !showTerrainAccordion }
                            .padding(14.dp)
                            .testTag("toggle_terrain_accordion"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (canPlanInSimulator) "Terrain, notes & Simulateur Top 15" else "Terrain & notes",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = terrainCondition.ifBlank { "Terrain non noté" } +
                                        (if (terrainNotes.isNotBlank()) " • $terrainNotes" else "") +
                                        if (isPlannedSimulator && canPlanInSimulator) " • Prévue Top 15" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            text = if (showTerrainAccordion) "Replier ▲" else "Modifier ▼",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(visible = showTerrainAccordion) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            Text(
                                text = "État du terrain :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("", "Sec", "Souple", "Boueux", "Très gras").forEach { cond ->
                                    val isSelected = terrainCondition == cond
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .clickable { terrainCondition = cond }
                                            .testTag("terrain_chip_$cond"),
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = cond.ifBlank { "Non noté" },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = terrainNotes,
                                onValueChange = { terrainNotes = it },
                                label = { Text("Note parcours / boue", maxLines = 1) },
                                placeholder = { Text("Ex: Sous-bois glissant") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (canPlanInSimulator) Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isPlannedSimulator) {
                                    CountingTop15BadgeGreen.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isPlannedSimulator) CountingTop15BadgeGreen else Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { isPlannedSimulator = !isPlannedSimulator }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlannedSimulator) Icons.Default.CheckCircle else Icons.Default.AddTask,
                                            contentDescription = null,
                                            tint = if (isPlannedSimulator) CountingTop15BadgeGreen else SemoisRiverTeal,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isPlannedSimulator) {
                                                "Incluse dans mon Simulateur Top 15 ($distanceKm km)"
                                            } else {
                                                "Ajouter au Simulateur Top 15 ($distanceKm km)"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isPlannedSimulator) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${simulatedTargetPointsInput.toIntOrNull() ?: averagePointsForDistance} pts",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = CountingTop15BadgeGreen
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    onSaveRaceTerrainAndWeather(
                                        activeProfileId,
                                        terrainCondition,
                                        terrainNotes,
                                        isPlannedSimulator && canPlanInSimulator,
                                        distanceKm,
                                        simulatedTargetPointsInput.toIntOrNull() ?: averagePointsForDistance
                                    )
                                    showTerrainAccordion = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("save_terrain_weather_button")
                            ) {
                                Text(
                                    text = "Enregistrer",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
