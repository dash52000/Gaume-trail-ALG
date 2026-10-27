package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.MyChronoDirectoryEntry
import com.example.data.local.RunnerProfileEntity
import com.example.data.repository.SyncDiagnostics
import com.example.ui.components.MyChronoDiagnosticCard
import com.example.ui.components.EditProfileBibAndDistanceDialog
import com.example.ui.theme.CountingTop15BadgeGreen
import com.example.ui.theme.GoldPodium
import com.example.ui.theme.SemoisRiverTeal

/**
 * Écran Réglages & Attribution des Dossards par Challenge (10 km & 5 km) :
 * - Attribution directe et séparée du dossard Challenge 10 km et du dossard Challenge 5 km pour chaque membre du Trio
 * - Bouton 1-clic « Aucun (0 course) » pour vider un challenge non couru (ex. 10 km pour Laetitia et Elodie)
 * - Bouton « Nettoyer les données sans dossard & Re-synchroniser MyChrono »
 * - Annuaire MyChrono (2026+)
 */
@Composable
fun SettingsAndAssociationScreen(
    profiles: List<RunnerProfileEntity>,
    selectedSeasonYear: Int,
    directorySearchQuery: String,
    directoryResults: List<MyChronoDirectoryEntry>,
    isSyncingMyChrono: Boolean,
    onUpdateSearchQuery: (String) -> Unit,
    onLinkProfileToEntry: (profileId: String, entry: MyChronoDirectoryEntry) -> Unit,
    onUpdateProfileBibAndDistance: (
        profileId: String,
        displayName: String,
        myChronoId: String,
        bib10km: String,
        bib5km: String,
        preferredDistanceKm: Int,
        clubName: String,
        categoryLabel: String
    ) -> Unit,
    onTriggerMyChronoSync: (Int) -> Unit,
    onPurgeAndResyncCleanResults: () -> Unit = { onTriggerMyChronoSync(selectedSeasonYear) },
    diagnostics: SyncDiagnostics = SyncDiagnostics(),
    foundResultCounts: Map<String, Map<Int, Map<String, Int>>> = emptyMap(),
    onExportUserData: (Uri) -> Unit = {},
    onImportUserData: (Uri) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) onExportUserData(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImportUserData(uri)
    }
    var activeTargetProfileId by remember { mutableStateOf("me") }
    var profileBeingEditedInModal by remember { mutableStateOf<RunnerProfileEntity?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 0. Diagnostic : ce que MyChrono publie sous chaque identifiant, et ce que l'appli retrouve
        item {
            MyChronoDiagnosticCard(
                profiles = profiles,
                diagnostics = diagnostics,
                foundResultCounts = foundResultCounts
            )
        }

        // 1. En-tête : Attribution des Dossards pour les 2 Challenges (10 km & 5 km)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Dossards de la saison $selectedSeasonYear (10 km & 5 km)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Les résultats sont retrouvés par l'identifiant MyChrono, sur le 10 km comme sur le 5 km. Le dossard de la saison ne sert qu'en secours, si l'identifiant manque.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Cartes d'attribution directe pour Damien, Laetitia et Elodie
                    profiles.forEach { profile ->
                        DirectDualChallengeBibAssignmentCard(
                            profile = profile,
                            isSyncing = isSyncingMyChrono,
                            onSaveBibs = { bib10, bib5, prefDist ->
                                onUpdateProfileBibAndDistance(
                                    profile.profileId,
                                    profile.displayName,
                                    profile.myChronoRunnerId,
                                    bib10,
                                    bib5,
                                    prefDist,
                                    profile.clubName,
                                    profile.categoryLabel
                                )
                            },
                            onOpenFullEditModal = { profileBeingEditedInModal = profile }
                        )
                    }
                }
            }
        }

        // 2. Carte Synchronisation MyChrono & Purge des données fictives / sans dossard
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Synchronisation MyChrono",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Toutes les saisons depuis 2025. Les courses nouvelles ou des 3 dernières semaines sont téléchargées, quelques classements plus anciens sont rafraîchis à chaque fois, et ta fiche MyChrono sert de contrôle ; « Tout retélécharger » reprend tous les classements.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onTriggerMyChronoSync(selectedSeasonYear) },
                            enabled = !isSyncingMyChrono,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("settings_sync_mychrono_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSyncingMyChrono) "Sync…" else "Sync MyChrono",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = onPurgeAndResyncCleanResults,
                            enabled = !isSyncingMyChrono,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("settings_purge_results_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tout retélécharger",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Sauvegarde des saisies (changement de téléphone, réinstallation)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
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
                        text = "Sauvegarde de mes données",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Profils, dossards, dossards par course, terrain et notes, courses prévues, objectifs et traces GPX, " +
                            "dans un fichier à garder (Drive, mail…). Les classements MyChrono n'y sont pas : ils se retéléchargent. " +
                            "La restauration remplace les saisies qui portent sur les mêmes courses et garde les autres.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { exportLauncher.launch("gaume-trail-alg-sauvegarde-${java.time.LocalDate.now()}.json") },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("settings_export_button")
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sauvegarder", fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
                            enabled = !isSyncingMyChrono,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("settings_import_button")
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restaurer", fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }

        // 3. Recherche dans l'annuaire MyChrono
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rechercher un coureur dans MyChrono",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        profiles.forEach { p ->
                            val isSelected = p.profileId == activeTargetProfileId
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { activeTargetProfileId = p.profileId },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = p.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = directorySearchQuery,
                        onValueChange = onUpdateSearchQuery,
                        label = { Text("Nom, club ou numéro de dossard") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        supportingText = {
                            if (directorySearchQuery.trim().length < 2) Text("Tape au moins 2 caractères (100 résultats au plus).")
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("directory_search_input")
                    )
                }
            }
        }

        items(directoryResults, key = { it.myChronoRunnerId }) { entry ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.fullName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = buildString {
                                append("10 km : ")
                                append(if (entry.defaultBib10km.isNotBlank()) "#${entry.defaultBib10km}" else "Aucun")
                                append(" • 5 km : ")
                                append(if (entry.defaultBib5km.isNotBlank()) "#${entry.defaultBib5km}" else "Aucun")
                                append(" • ${entry.club}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = { onLinkProfileToEntry(activeTargetProfileId, entry) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Associer",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    profileBeingEditedInModal?.let { targetProfile ->
        EditProfileBibAndDistanceDialog(
            profile = targetProfile,
            seasonYear = selectedSeasonYear,
            onDismiss = { profileBeingEditedInModal = null },
            onConfirm = { name, myChronoId, bib10, bib5, prefDist, club, cat ->
                profileBeingEditedInModal = null
                onUpdateProfileBibAndDistance(
                    targetProfile.profileId,
                    name,
                    myChronoId,
                    bib10,
                    bib5,
                    prefDist,
                    club,
                    cat
                )
            }
        )
    }
}

/**
 * Carte d'attribution directe des dossards pour les 2 Challenges (Challenge 10 km & Challenge 5 km)
 * avec modification directe, bouton rapide « Aucun (0 course) » par challenge et sauvegarde 1-clic.
 */
@Composable
private fun DirectDualChallengeBibAssignmentCard(
    profile: RunnerProfileEntity,
    isSyncing: Boolean,
    onSaveBibs: (bib10km: String, bib5km: String, preferredDistanceKm: Int) -> Unit,
    onOpenFullEditModal: () -> Unit
) {
    var bib10Input by remember(profile.profileId, profile.defaultBib10km) {
        mutableStateOf(profile.defaultBib10km)
    }
    var bib5Input by remember(profile.profileId, profile.defaultBib5km) {
        mutableStateOf(profile.defaultBib5km)
    }
    var preferredDist by remember(profile.profileId, profile.preferredDistanceKm) {
        mutableIntStateOf(profile.preferredDistanceKm)
    }

    val hasUnsavedChanges = bib10Input.trim().removePrefix("#") != profile.defaultBib10km ||
        bib5Input.trim().removePrefix("#") != profile.defaultBib5km ||
        preferredDist != profile.preferredDistanceKm

    val runnerColor = Color(profile.avatarColorHex)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(
            width = if (hasUnsavedChanges) 1.5.dp else 1.dp,
            color = if (hasUnsavedChanges) runnerColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dual_bib_card_${profile.profileId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Ligne 1 : Avatar + Nom du coureur + Bouton Détails profil
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(runnerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${profile.categoryLabel} • ${profile.clubName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenFullEditModal() }
                        .testTag("settings_edit_profile_${profile.profileId}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Infos profil",
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Profil",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Ligne 2 : Les 2 boîtes côte à côte (Challenge 10 km vs Challenge 5 km)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Boîte Challenge 10 km
                ChallengeBibInputBox(
                    title = "Challenge 10 km",
                    bibValue = bib10Input,
                    onBibChange = {
                        bib10Input = it
                        if (it.isNotBlank() && bib5Input.isBlank()) preferredDist = 10
                    },
                    onClearBib = {
                        bib10Input = ""
                        if (bib5Input.isNotBlank()) preferredDist = 5
                    },
                    isPreferred = preferredDist == 10,
                    onSelectPreferred = { preferredDist = 10 },
                    accentColor = MaterialTheme.colorScheme.primary,
                    testTagPrefix = "${profile.profileId}_10km",
                    modifier = Modifier.weight(1f)
                )

                // Boîte Challenge 5 km
                ChallengeBibInputBox(
                    title = "Challenge 5 km",
                    bibValue = bib5Input,
                    onBibChange = {
                        bib5Input = it
                        if (it.isNotBlank() && bib10Input.isBlank()) preferredDist = 5
                    },
                    onClearBib = {
                        bib5Input = ""
                        if (bib10Input.isNotBlank()) preferredDist = 10
                    },
                    isPreferred = preferredDist == 5,
                    onSelectPreferred = { preferredDist = 5 },
                    accentColor = SemoisRiverTeal,
                    testTagPrefix = "${profile.profileId}_5km",
                    modifier = Modifier.weight(1f)
                )
            }

            // Ligne 3 : Résumé de l'état des 2 challenges + Bouton Enregistrer les dossards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildString {
                        append("10k: ")
                        append(if (bib10Input.isNotBlank()) "#${bib10Input.trim()}" else "0 course")
                        append(" • 5k: ")
                        append(if (bib5Input.isNotBlank()) "#${bib5Input.trim()}" else "0 course")
                        append(" • Défaut: ${preferredDist}k")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        onSaveBibs(
                            bib10Input.trim().removePrefix("#"),
                            bib5Input.trim().removePrefix("#"),
                            preferredDist
                        )
                    },
                    enabled = !isSyncing,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("save_dual_bibs_${profile.profileId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (hasUnsavedChanges) "Appliquer" else "Enregistré",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengeBibInputBox(
    title: String,
    bibValue: String,
    onBibChange: (String) -> Unit,
    onClearBib: () -> Unit,
    isPreferred: Boolean,
    onSelectPreferred: () -> Unit,
    accentColor: Color,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    val hasBib = bibValue.trim().isNotBlank()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (hasBib) 1.5.dp else 1.dp,
            color = if (hasBib) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (hasBib) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isPreferred) GoldPodium.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectPreferred() }
                ) {
                    Text(
                        text = if (isPreferred) "Défaut ★" else "Défaut",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPreferred) Color(0xFF8A5D00) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            OutlinedTextField(
                value = bibValue,
                onValueChange = onBibChange,
                placeholder = {
                    Text(
                        text = "Aucun (0 course)",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                prefix = {
                    if (hasBib) {
                        Text("#", fontWeight = FontWeight.ExtraBold, color = accentColor)
                    }
                },
                trailingIcon = {
                    if (hasBib) {
                        IconButton(
                            onClick = onClearBib,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Vider ce dossard (0 course)",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bib_input_$testTagPrefix")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasBib) "Actif (#${bibValue.trim()})" else "Non couru → 0 course",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasBib) CountingTop15BadgeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (hasBib) {
                    Text(
                        text = "Retirer",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onClearBib() }
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
