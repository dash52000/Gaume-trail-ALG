package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.GaumeVillageGeo
import com.example.data.local.RaceEntity

/** Ouvre l'itinéraire vers le départ dans Google Maps (ou le navigateur), depuis la position actuelle. */
fun openDirectionsTo(context: Context, latitude: Double, longitude: Double) {
    runCatching {
        val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

fun openExternalUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

/**
 * Fiche pratique d'une manche, uniquement avec ce que publie MyChrono
 * (adresse, horaires, terrain, équipements, tracés). Rien n'est affiché pour une info absente.
 */
@Composable
fun RacePracticalInfoCard(
    race: RaceEntity,
    geo: GaumeVillageGeo,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val routeLinks = race.routeLinkList()
    val facilities = listOfNotNull(
        race.hasVestiaire?.let { (if (it) "✓" else "✗") + " Vestiaires" },
        race.hasDouche?.let { (if (it) "✓" else "✗") + " Douches" },
        race.hasGarderie?.let { (if (it) "✓" else "✗") + " Garderie" },
        race.hasResto?.let { (if (it) "✓" else "✗") + " Petite restauration" }
    )
    val distances = listOfNotNull(
        race.distanceKm.takeIf { it > 0.0 }?.let { "grande %.1f km".format(it) },
        race.distance5Km?.let { "petite %.1f km".format(it) }
    )
    val hasAnyInfo = race.address != null || race.schedule != null || race.kidsSchedule != null ||
        race.terrainDescription != null || facilities.isNotEmpty() || routeLinks.isNotEmpty()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("race_practical_info_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Infos pratiques",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (!hasAnyInfo) {
                Text(
                    text = "MyChrono ne publie pas encore d'infos pratiques pour cette manche.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            race.address?.let { InfoLine(label = "Lieu", value = it) }
            race.schedule?.let { InfoLine(label = "Départ", value = it) }
            race.kidsSchedule?.let { InfoLine(label = "Jeunes", value = it) }
            if (distances.isNotEmpty()) {
                InfoLine(label = "Distances", value = distances.joinToString(" • "))
            }
            race.terrainDescription?.let { InfoLine(label = "Terrain", value = it) }
            if (facilities.isNotEmpty()) {
                InfoLine(label = "Sur place", value = facilities.joinToString("   "))
            }

            if (routeLinks.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    routeLinks.forEach { (label, url) ->
                        OutlinedButton(
                            onClick = { openExternalUrl(context, url) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Tracé $label", maxLines = 1)
                        }
                    }
                }
            }

            val lat = geo.latitude
            val lon = geo.longitude
            if (lat != null && lon != null) {
                Button(
                    onClick = { openDirectionsTo(context, lat, lon) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("race_practical_directions_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (race.startLatitude != null) "Itinéraire jusqu'au départ" else "Itinéraire jusqu'au village"
                    )
                }
            }

            race.sourceUrl?.let { url ->
                TextButton(
                    onClick = { openExternalUrl(context, url) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Page officielle de la course",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(78.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
