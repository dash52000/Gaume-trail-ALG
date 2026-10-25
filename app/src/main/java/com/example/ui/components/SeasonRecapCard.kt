package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.local.RunnerProfileEntity
import com.example.domain.AlgChallengeCalculator
import com.example.domain.FormPoint
import com.example.domain.SeasonInsightsCalculator
import com.example.domain.SeasonRecap
import com.example.ui.theme.CountingTop15BadgeGreen
import java.io.File
import java.io.FileOutputStream

private fun SeasonRecap.challengeLine(): String =
    if (challengeComplete) "Challenge validé : $countingRaces/${AlgChallengeCalculator.MAX_COUNTING_RACES} courses"
    else "$countingRaces/${AlgChallengeCalculator.MAX_COUNTING_RACES} courses comptées"

private fun SeasonRecap.standingLine(): String? = standing?.let { st ->
    "${st.overallRank}e / ${runnersInDistance ?: "?"} au général • ${st.categoryRank}e / ${runnersInCategory ?: "?"} en ${st.category.ifBlank { "catégorie" }}"
}

private fun FormPoint.shortLine(withCorrected: Boolean = false): String =
    "$village (${dateIso.substring(5).replace('-', '/')}) • " +
        if (withCorrected && correctedPaceSecPerKm != null) SeasonInsightsCalculator.formatPace(correctedPaceSecPerKm)
        else "$timeFormatted • ${SeasonInsightsCalculator.formatPace(paceSecPerKm)}"

private fun SeasonRecap.formLine(): String? {
    val start = formStartPaceSecPerKm ?: return null
    val end = formEndPaceSecPerKm ?: return null
    val delta = end - start
    val trend = when {
        delta <= -3 -> "en progrès de ${-delta} s/km"
        delta >= 3 -> "en recul de $delta s/km"
        else -> "stable"
    }
    return "Forme : ${SeasonInsightsCalculator.formatPace(start)} en début de saison → ${SeasonInsightsCalculator.formatPace(end)} en fin ($trend)"
}

/** Bilan de saison du coureur actif, avec un résumé du Trio et un export image. */
@Composable
fun SeasonRecapCard(
    recap: SeasonRecap,
    trioRecaps: Map<String, SeasonRecap>,
    profiles: List<RunnerProfileEntity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("season_recap_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFD49B1B),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bilan ${recap.seasonYear} • ${recap.profileName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Challenge ${recap.distanceLabel}" + if (recap.isSeasonOver) " • saison terminée" else " • saison en cours",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (recap.challengeComplete) CountingTop15BadgeGreen.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = recap.challengeLine(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (recap.challengeComplete) CountingTop15BadgeGreen else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${recap.racesRun} course(s) courue(s) • ${recap.totalPoints} pts sur les 15 meilleures",
                        style = MaterialTheme.typography.bodySmall
                    )
                    recap.standingLine()?.let {
                        Text(text = "Classement provisoire : $it", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            RecapLine("Kilomètres de course", "${"%.1f".format(recap.totalKm)} km en ${SeasonInsightsCalculator.formatDuration(recap.totalTimeSeconds)}")
            recap.bestPointsRace?.let { RecapLine("Meilleur score", "${it.points} pts • ${it.village}") }
            recap.fastestRace?.let { RecapLine("Allure la plus rapide", it.shortLine()) }
            recap.bestCorrectedRace?.let { RecapLine("Meilleure course (parcours corrigé)", it.shortLine(withCorrected = true)) }
            recap.formLine()?.let { RecapLine("Évolution", it.substringAfter("Forme : ")) }
            RecapLine(
                "Villages",
                "${recap.villagesThisSeason} village(s) cette saison" +
                    if (recap.newVillagesThisSeason > 0) ", dont ${recap.newVillagesThisSeason} nouveau(x)" else ""
            )
            if (recap.horsChallengeRaces > 0) {
                RecapLine("Hors challenge", "${recap.horsChallengeRaces} course(s), sans effet sur le challenge")
            }

            val others = profiles.mapNotNull { p -> trioRecaps[p.profileId] }.filter { it.racesRun > 0 }
            if (others.size > 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text(text = "Le Trio", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                others.forEach { r ->
                    Text(
                        text = "${r.profileName.substringBefore(" ")} (${r.distanceLabel}) : ${r.racesRun} courses • " +
                            (if (r.challengeComplete) "challenge validé" else "${r.countingRaces}/15") +
                            " • ${r.villagesThisSeason} villages",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { shareSeasonRecap(context, recap) },
                    enabled = recap.racesRun > 0,
                    modifier = Modifier.weight(1f).testTag("share_season_recap_button")
                ) {
                    Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Partager mon bilan")
                }
                if (others.size > 1) {
                    OutlinedButton(
                        onClick = { shareTrioRecap(context, recap.seasonYear, others) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Bilan du Trio")
                    }
                }
            }
        }
    }
}

@Composable
private fun RecapLine(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun recapText(recap: SeasonRecap): String = buildString {
    appendLine("Allures Libres de Gaume ${recap.seasonYear} — ${recap.profileName} (${recap.distanceLabel})")
    appendLine(recap.challengeLine() + " • ${recap.totalPoints} pts")
    recap.standingLine()?.let { appendLine("Classement provisoire : $it") }
    appendLine("${recap.racesRun} courses • ${"%.1f".format(recap.totalKm)} km • ${recap.villagesThisSeason} villages")
    recap.bestPointsRace?.let { appendLine("Meilleur score : ${it.points} pts à ${it.village}") }
    recap.bestCorrectedRace?.let { appendLine("Meilleure course (parcours corrigé) : ${it.shortLine(withCorrected = true)}") }
    recap.formLine()?.let { appendLine(it) }
    if (recap.horsChallengeRaces > 0) appendLine("+ ${recap.horsChallengeRaces} course(s) hors challenge")
}

private fun shareTrioRecap(context: Context, seasonYear: Int, recaps: List<SeasonRecap>) {
    val text = buildString {
        appendLine("Le Trio aux Allures Libres de Gaume $seasonYear")
        recaps.forEach { r ->
            appendLine()
            append(recapText(r))
        }
    }
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            },
            "Partager le bilan du Trio"
        )
    )
}

/** Image 1080 × 1350 du bilan, partagée avec le texte. */
fun shareSeasonRecap(context: Context, recap: SeasonRecap) {
    val text = recapText(recap)
    runCatching {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(0xFF0E3520.toInt(), 0xFF1B5E3A.toInt(), 0xFF123D26.toInt()),
                null, Shader.TileMode.CLAMP
            )
        })
        canvas.drawRoundRect(RectF(36f, 36f, width - 36f, height - 36f), 42f, 42f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = 0xFFFFD166.toInt()
        })
        fun paint(size: Float, color: Int, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
        val gold = 0xFFFFD166.toInt()
        val white = 0xFFFFFFFF.toInt()
        val mint = 0xFFB7E4C7.toInt()

        canvas.drawText("ALLURES LIBRES DE GAUME • BILAN ${recap.seasonYear}", 84f, 120f, paint(34f, gold, true))
        canvas.drawText(recap.profileName, 84f, 205f, paint(72f, white, true))
        canvas.drawText("Challenge ${recap.distanceLabel}", 84f, 262f, paint(38f, mint, true))

        canvas.drawRoundRect(RectF(76f, 310f, width - 76f, 520f), 32f, 32f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x26FFFFFF })
        canvas.drawText(
            if (recap.challengeComplete) "CHALLENGE VALIDÉ" else "${recap.countingRaces} / 15 COURSES",
            112f, 400f, paint(64f, gold, true)
        )
        canvas.drawText("${recap.racesRun} courses • ${recap.totalPoints} pts", 112f, 470f, paint(40f, white))

        var y = 600f
        fun line(label: String, value: String) {
            canvas.drawText(label.uppercase(), 84f, y, paint(26f, mint, true))
            canvas.drawText(value.take(52), 84f, y + 46f, paint(38f, white, true))
            y += 118f
        }
        recap.standingLine()?.let { line("Classement provisoire", it) }
        line("Kilomètres", "${"%.1f".format(recap.totalKm)} km en ${SeasonInsightsCalculator.formatDuration(recap.totalTimeSeconds)}")
        recap.bestPointsRace?.let { line("Meilleur score", "${it.points} pts • ${it.village}") }
        recap.bestCorrectedRace?.let { line("Meilleure course (parcours corrigé)", it.shortLine(withCorrected = true)) }
        line("Villages", "${recap.villagesThisSeason} cette saison" + if (recap.newVillagesThisSeason > 0) " (${recap.newVillagesThisSeason} nouveaux)" else "")
        recap.formLine()?.let { line("Évolution", it.substringAfter("Forme : ").substringBefore(" (")) }

        val dir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val out = File(dir, "bilan_saison_${recap.seasonYear}_${recap.profileId}.png")
        FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", out)
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, text)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Partager mon bilan de saison"
            )
        )
    }.onFailure {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                "Partager mon bilan de saison"
            )
        )
    }
}
