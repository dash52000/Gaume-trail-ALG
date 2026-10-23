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
        weatherCondition: String,
        temperatureC: Int?,
        terrainNotes: String,
        isPlannedForSimulator: Boolean,
        plannedDistanceKm: Int,
        simulatedTargetPoints: Int?
    ) -> Unit,
    onSelectRunnerProfile: (String) -> Unit,
    onImportGpxFile: (Uri) -> Unit
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
        mutableStateOf(tw?.terrainCondition ?: "Sec")
    }
    var weatherCondition by remember(activeProfileId, tw) {
        mutableStateOf(tw?.weatherCondition ?: "Nuageux")
    }
    var temperatureInput by remember(activeProfileId, tw) {
        mutableStateOf(tw?.temperatureC?.toString().orEmpty())
    }
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
                            text = "Manche #${race.roundNumber} • ${race.title}",
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
                                text = "$dist km",
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

            // 3. BANDEAU FIEF DE GAUME & MÉTÉO RÉSUMÉE
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (currentResult != null) {
                    CountingTop15BadgeGreen.copy(alpha = 0.12f)
                } else {
                    GoldPodium.copy(alpha = 0.12f)
                },
                border = BorderStroke(
                    1.dp,
                    if (currentResult != null) CountingTop15BadgeGreen.copy(alpha = 0.45f)
                    else GoldPodium.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (currentResult != null) CountingTop15BadgeGreen else GoldPodium,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (currentResult != null) {
                                    "Village Conquis : ${raceItem.gaumeGeo.canonicalVillage} (+150 XP)"
                                } else {
                                    "Village à Conquérir : ${raceItem.gaumeGeo.canonicalVillage}"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Fief : ${raceItem.gaumeGeo.territoryName} • " + if (tw != null) {
                                    "Terrain : $terrainCondition • $weatherCondition" + (tw.temperatureC?.let { " ($it°C)" } ?: "")
                                } else {
                                    "Terrain et météo non renseignés"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

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
                                    text = "Météo, Terrain & Simulateur Top 15",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$terrainCondition • $weatherCondition (${temperatureInput}°C)" +
                                        if (isPlannedSimulator) " • Prévue Top 15" else "",
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
                                listOf("Sec", "Souple", "Boueux", "Très gras").forEach { cond ->
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
                                                text = cond,
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

                            Text(
                                text = "Conditions météo :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Soleil", "Nuageux", "Pluie", "Vent / Frais").forEach { wCond ->
                                    val isSelected = weatherCondition == wCond
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .clickable { weatherCondition = wCond }
                                            .testTag("weather_chip_$wCond"),
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (isSelected) {
                                            SemoisRiverTeal
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = wCond,
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = temperatureInput,
                                    onValueChange = { temperatureInput = it },
                                    label = { Text("Temp. (°C)", maxLines = 1) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.35f)
                                )
                                OutlinedTextField(
                                    value = terrainNotes,
                                    onValueChange = { terrainNotes = it },
                                    label = { Text("Note parcours / boue", maxLines = 1) },
                                    placeholder = { Text("Ex: Sous-bois glissant") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.65f)
                                )
                            }

                            Surface(
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
                                        weatherCondition,
                                        temperatureInput.toIntOrNull(),
                                        terrainNotes,
                                        isPlannedSimulator,
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
                                    text = "Enregistrer Météo, Terrain & Simulateur",
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

@Composable
private fun OfficialMyChronoResultCard(
    runnerName: String,
    distanceKm: Int,
    result: RaceResultEntity?,
    pronosticEntry: GaumeRacePronosticEntry? = null,
    isCountingInTop15: Boolean,
    rankAmongMyRaces: Int?,
    onShareCardClick: () -> Unit = {},
    onExpandBibSearch: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (result != null) {
                            "Résultat Officiel MyChrono • $distanceKm km"
                        } else if (pronosticEntry != null) {
                            "Pronostic Avant-Course • $distanceKm km"
                        } else {
                            "Résultat & Dossard • $distanceKm km"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (result != null) {
                            "$runnerName • ${result.distanceLabel} • Dossard #${result.bibNumber}"
                        } else if (pronosticEntry != null) {
                            "$runnerName • Basé sur tes 3 dernières courses (${pronosticEntry.recentAveragePaceFormatted})"
                        } else {
                            "$runnerName • Aucun résultat $distanceKm km enregistré"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Top15StatusBadge(
                    isCountingInTop15 = isCountingInTop15,
                    rankAmongRunnerRaces = rankAmongMyRaces,
                    distanceLabel = "$distanceKm km"
                )
            }

            if (pronosticEntry != null) {
                RacePronosticVsActualMiniBanner(entry = pronosticEntry)
            }

            if (result == null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExpandBibSearch() }
                ) {
                    Text(
                        text = "Aucun résultat MyChrono trouvé sur le $distanceKm km pour cette manche. Utilisez l'encart « Dossard & Recherche MyChrono » ci-dessous pour chercher votre dossard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailHighlightBox(
                        label = "Temps Officiel",
                        value = result.officialTimeFormatted,
                        sub = result.distanceLabel,
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    DetailHighlightBox(
                        label = "Allure & Vitesse",
                        value = result.pacePerKmFormatted,
                        sub = "${result.speedKmh} km/h",
                        accent = SemoisRiverTeal,
                        modifier = Modifier.weight(1f)
                    )
                    DetailHighlightBox(
                        label = "Points (${result.distanceLabel})",
                        value = "${result.challengePoints} pts",
                        sub = if (isCountingInTop15) "Retenu Top 15" else "Hors Top 15",
                        accent = if (isCountingInTop15) CountingTop15BadgeGreen else ClayTerracotta,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Scratch",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${result.overallRank}e / ${result.totalFinishers}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Catégorie",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${result.categoryRank}e / ${result.categoryTotal}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onShareCardClick() }
                            .testTag("detail_open_share_card_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Carte Bilan",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailHighlightBox(
    label: String,
    value: String,
    sub: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TrioRaceComparisonSection(
    raceItem: RaceItemWithContext,
    profiles: List<RunnerProfileEntity>,
    trioSummaries: Map<String, RunnerChallengeSummary>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Comparaison Trio sur cette manche",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            profiles.forEach { profile ->
                val results = raceItem.resultsByProfileAndDistance[profile.profileId].orEmpty()
                if (results.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color(profile.avatarColorHex))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = profile.displayName.substringBefore(" "),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "Non courue",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                } else {
                    results.forEach { r ->
                        val distKm = if (r.distanceLabel == "5 km") 5 else 10
                        val isCounting = trioSummaries[profile.profileId]
                            ?.challengeForDistance(distKm)
                            ?.countingRaceIds
                            ?.contains(raceItem.race.raceId) == true

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(0.42f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(Color(profile.avatarColorHex))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${profile.displayName.substringBefore(" ")} (${r.distanceLabel})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                modifier = Modifier.weight(0.58f),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${r.bibNumber} • ${r.officialTimeFormatted} • ${r.challengePoints} pts",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isCounting) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = "Retenu Top 15",
                                        tint = CountingTop15BadgeGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchAndGpsSection(
    raceItem: RaceItemWithContext,
    onPickGpxFile: () -> Unit
) {
    val metrics = raceItem.myMetrics
    val trackPoints = remember(metrics?.serializedTrackPoints) {
        metrics?.serializedTrackPoints?.let { GpxAndTrackUtils.deserializeTrackPoints(it) }.orEmpty()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Trace GPS & Profil Altimétrique (.GPX)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )

            if (metrics != null && trackPoints.isNotEmpty()) {
                GaumeGpsAndElevationCanvas(
                    trackPoints = trackPoints,
                    recordedDistanceKm = metrics.recordedDistanceKm,
                    elevationGainMeters = metrics.elevationGainMeters,
                    sourceLabel = metrics.sourceTitle
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WatchMetricChip(
                        icon = Icons.Default.Favorite,
                        label = "FC Moy / Max",
                        value = "${metrics.avgHeartRateBpm ?: "--"} / ${metrics.maxHeartRateBpm ?: "--"} bpm",
                        tint = ClayTerracotta,
                        modifier = Modifier.weight(1f)
                    )
                    WatchMetricChip(
                        icon = Icons.Default.Speed,
                        label = "Cadence Moy.",
                        value = metrics.cadenceSpm?.let { "$it spm" } ?: "Non mesurée",
                        tint = SemoisRiverTeal,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Importez un fichier .GPX de votre montre pour afficher le tracé 2D et le profil altimétrique D+ de cette manche.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedButton(
                onClick = onPickGpxFile,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("import_gpx_race_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Importer un fichier .GPX",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun WatchMetricChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}
