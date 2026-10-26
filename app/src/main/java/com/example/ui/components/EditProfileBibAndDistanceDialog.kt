package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.RunnerProfileEntity
import com.example.domain.TrackPoint
import com.example.ui.theme.ClayTerracotta
import com.example.ui.theme.CountingTop15BadgeGreen
import com.example.ui.theme.GoldPodium
import kotlin.math.max
import kotlin.math.min

/**
 * Boîte de dialogue pour configurer le numéro de dossard et la distance (5 km / 10 km) d'un profil.
 */
@Composable
fun EditProfileBibAndDistanceDialog(
    profile: RunnerProfileEntity,
    seasonYear: Int? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        displayName: String,
        myChronoId: String,
        bib10km: String,
        bib5km: String,
        preferredDistanceKm: Int,
        clubName: String,
        categoryLabel: String
    ) -> Unit
) {
    var displayName by remember(profile) { mutableStateOf(profile.displayName) }
    var preferredDistanceKm by remember(profile) { mutableIntStateOf(profile.preferredDistanceKm) }
    var bib10km by remember(profile) { mutableStateOf(profile.defaultBib10km) }
    var bib5km by remember(profile) { mutableStateOf(profile.defaultBib5km) }
    var myChronoId by remember(profile) { mutableStateOf(profile.myChronoRunnerId) }
    var clubName by remember(profile) { mutableStateOf(profile.clubName) }
    var categoryLabel by remember(profile) { mutableStateOf(profile.categoryLabel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Dossards ${seasonYear ?: ""} (${profile.displayName})".replace("  ", " "),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Dossards de la saison ${seasonYear ?: ""} : ils changent chaque année. Les résultats du 10 km et du 5 km sont comptés séparément.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Distance affichée par défaut :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = preferredDistanceKm == 10,
                        onClick = { preferredDistanceKm = 10 },
                        label = { Text("Challenge 10 km", maxLines = 1) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_dist_10km_chip")
                    )
                    FilterChip(
                        selected = preferredDistanceKm == 5,
                        onClick = { preferredDistanceKm = 5 },
                        label = { Text("Challenge 5 km", maxLines = 1) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_dist_5km_chip")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = bib10km,
                        onValueChange = { bib10km = it },
                        label = { Text("Dossard 10 km", maxLines = 1) },
                        placeholder = { Text("Ex: 697") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_bib_10km_input")
                    )
                    OutlinedTextField(
                        value = bib5km,
                        onValueChange = { bib5km = it },
                        label = { Text("Dossard 5 km", maxLines = 1) },
                        placeholder = { Text("Ex: 1150") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_bib_5km_input")
                    )
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nom / Prénom sur MyChrono") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = myChronoId,
                        onValueChange = { myChronoId = it },
                        label = { Text("ID MyChrono", maxLines = 1) },
                        singleLine = true,
                        modifier = Modifier.weight(0.45f)
                    )
                    OutlinedTextField(
                        value = categoryLabel,
                        onValueChange = { categoryLabel = it },
                        label = { Text("Catégorie", maxLines = 1) },
                        singleLine = true,
                        modifier = Modifier.weight(0.55f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        displayName,
                        myChronoId,
                        bib10km,
                        bib5km,
                        preferredDistanceKm,
                        clubName,
                        categoryLabel
                    )
                },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("dialog_save_bib_button")
            ) {
                Text("Enregistrer & Sync", maxLines = 1)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", maxLines = 1)
            }
        }
    )
}
