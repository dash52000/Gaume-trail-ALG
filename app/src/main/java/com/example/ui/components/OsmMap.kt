package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Typeface
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Overlay
import java.io.File
import kotlin.math.hypot

/** Un point de la carte : une manche, ou un village. */
data class MapMarker(
    val key: String,
    val latitude: Double,
    val longitude: Double,
    val label: String,
    /** Texte dans la pastille (numéro de manche, nombre de passages) ; null = pastille sans texte. */
    val badge: String? = null,
    val color: Color,
    /** Pastille creuse : lieu pas encore couru. */
    val hollow: Boolean = false,
    /** 1 = taille normale. */
    val sizeFactor: Float = 1f
)

/** Pastilles dessinées par-dessus le fond OpenStreetMap, et touchées au doigt. */
private class MarkersOverlay(private val density: Float) : Overlay() {
    var markers: List<MapMarker> = emptyList()
    var selectedKey: String? = null
    var onSelect: (String) -> Unit = {}

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = android.graphics.Color.rgb(20, 38, 26)
    }
    private val labelBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(230, 255, 255, 255)
    }
    private val point = Point()

    private fun radius(m: MapMarker): Float = (9f * m.sizeFactor + if (m.key == selectedKey) 3f else 0f) * density

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val projection = mapView.projection
        val showAllLabels = mapView.zoomLevelDouble >= 11.5
        markers.sortedBy { it.key == selectedKey }.forEach { m ->
            projection.toPixels(GeoPoint(m.latitude, m.longitude), point)
            val x = point.x.toFloat()
            val y = point.y.toFloat()
            val r = radius(m)
            val selected = m.key == selectedKey
            val argb = m.color.toArgb()

            if (selected) {
                fill.color = android.graphics.Color.argb(110, 255, 215, 0)
                canvas.drawCircle(x, y, r * 1.7f, fill)
            }
            fill.color = if (m.hollow) android.graphics.Color.WHITE else argb
            canvas.drawCircle(x, y, r, fill)
            stroke.color = when {
                selected -> android.graphics.Color.rgb(255, 215, 0)
                m.hollow -> argb
                else -> android.graphics.Color.WHITE
            }
            stroke.strokeWidth = (if (selected || m.hollow) 2.4f else 1.6f) * density
            canvas.drawCircle(x, y, r, stroke)

            m.badge?.let { text ->
                badgePaint.textSize = (if (text.length > 2) 8f else 9.5f) * density
                badgePaint.color = if (m.hollow) argb else android.graphics.Color.WHITE
                canvas.drawText(text, x, y - (badgePaint.descent() + badgePaint.ascent()) / 2f, badgePaint)
            }

            if (selected || showAllLabels) {
                labelPaint.textSize = 10f * density
                labelPaint.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                val half = labelPaint.measureText(m.label) / 2f + 5f * density
                val top = y - r - 19f * density
                canvas.drawRoundRect(x - half, top, x + half, top + 15f * density, 5f * density, 5f * density, labelBackground)
                canvas.drawText(m.label, x, top + 11f * density, labelPaint)
            }
        }
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        val projection = mapView.projection
        var best: MapMarker? = null
        var bestDistance = Float.MAX_VALUE
        markers.forEach { m ->
            projection.toPixels(GeoPoint(m.latitude, m.longitude), point)
            val d = hypot(point.x - e.x, point.y - e.y)
            if (d < bestDistance) {
                bestDistance = d
                best = m
            }
        }
        val hit = best ?: return false
        if (bestDistance > 30f * density) return false
        onSelect(hit.key)
        return true
    }
}

/** État gardé entre deux recompositions (cadrage déjà fait, dernière sélection). */
private class MapHolder {
    var fittedFor: Any? = Unit
    var lastSelected: String? = null
}

/**
 * Carte OpenStreetMap (vraies routes, rivières et relief) avec des pastilles par-dessus.
 * Le cadrage se fait sur les pastilles à l'ouverture et quand [fitKey] change.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun OsmMap(
    markers: List<MapMarker>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    fitKey: Any?,
    modifier: Modifier = Modifier,
    height: Dp = 290.dp
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val overlay = remember { MarkersOverlay(density) }
    val holder = remember { MapHolder() }
    val mapView = remember {
        Configuration.getInstance().apply {
            setUserAgentValue(BuildConfig.APPLICATION_ID)
            val base = File(context.cacheDir, "osmdroid")
            setOsmdroidBasePath(base)
            setOsmdroidTileCache(File(base, "tiles"))
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setTilesScaledToDpi(true)
            setMinZoomLevel(7.0)
            setMaxZoomLevel(18.0)
            getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            controller.setZoom(10.0)
            controller.setCenter(GeoPoint(49.66, 5.55))
            overlays.add(overlay)
            overlays.add(CopyrightOverlay(context))
            // La carte garde le doigt (sinon la liste qui la contient défile à sa place).
            setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) v.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .testTag("osm_map"),
        update = { map ->
            overlay.markers = markers
            overlay.selectedKey = selectedKey
            overlay.onSelect = onSelect

            if (holder.fittedFor != fitKey && markers.isNotEmpty()) {
                holder.fittedFor = fitKey
                val fit = {
                    if (markers.size == 1) {
                        map.controller.setZoom(13.0)
                        map.controller.setCenter(GeoPoint(markers[0].latitude, markers[0].longitude))
                    } else {
                        val box = BoundingBox.fromGeoPointsSafe(markers.map { GeoPoint(it.latitude, it.longitude) })
                        map.zoomToBoundingBox(box.increaseByScale(1.2f), false, (24 * density).toInt())
                    }
                }
                if (map.width > 0 && map.height > 0) fit() else map.addOnFirstLayoutListener { _, _, _, _, _ -> fit() }
            }

            if (selectedKey != holder.lastSelected) {
                holder.lastSelected = selectedKey
                markers.firstOrNull { it.key == selectedKey }?.let { m ->
                    val target = GeoPoint(m.latitude, m.longitude)
                    if (map.width > 0 && !map.boundingBox.contains(target)) map.controller.animateTo(target)
                }
            }
            map.invalidate()
        }
    )
}
