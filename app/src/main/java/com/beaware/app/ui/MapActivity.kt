package com.beaware.app.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.beaware.app.databinding.ActivityMapBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

/**
 * Map Activity - Shows anonymous Level 1 (critical danger) alert locations.
 * Uses OpenStreetMap (OSMDroid) for offline-capable, API-key-free mapping.
 */
class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding

    // Dummy data: High alert zones with two awareness levels
    // In a real app, this would come from a backend or local database
    private val dummyDangerZones = listOf(
        DangerZone(
            location = GeoPoint(41.9981, 21.4254), // Skopje, North Macedonia (central)
            title = "Critical Alert",
            description = "Emergency siren detected - 2 minutes ago",
            type = ZoneType.CRITICAL
        ),
        DangerZone(
            location = GeoPoint(42.0042, 21.4094), // Near City Park
            title = "Caution Zone",
            description = "Heavy traffic area - be alert",
            type = ZoneType.CAUTION
        ),
        DangerZone(
            location = GeoPoint(41.9945, 21.4312), // Near old bazaar
            title = "Caution Zone",
            description = "Busy pedestrian crossing - 1 week ago",
            type = ZoneType.CAUTION
        ),
        DangerZone(
            location = GeoPoint(42.0034, 21.4452), // East area
            title = "Critical Alert",
            description = "Ambulance siren - 3 days ago",
            type = ZoneType.CRITICAL
        ),
        DangerZone(
            location = GeoPoint(41.9912, 21.4178), // South area
            title = "Caution Zone",
            description = "High traffic intersection - 4 days ago",
            type = ZoneType.CAUTION
        ),
        // Additional zones to show clustering
        DangerZone(
            location = GeoPoint(42.0012, 21.4234),
            title = "Caution Zone",
            description = "Bike crossing - 6 days ago",
            type = ZoneType.CAUTION
        ),
        DangerZone(
            location = GeoPoint(41.9978, 21.4289),
            title = "Critical Alert",
            description = "Fire truck siren - 1 week ago",
            type = ZoneType.CRITICAL
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure OSMDroid
        Configuration.getInstance().userAgentValue = packageName
        
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupMap()
        addDangerZones()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupMap() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            
            // Set initial position (Skopje, North Macedonia)
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(41.9981, 21.4254))
            
            // Enable zoom controls
            setBuiltInZoomControls(true)
        }
    }

    private fun addDangerZones() {
        for (zone in dummyDangerZones) {
            // Get color based on zone type
            val zoneColor = when (zone.type) {
                ZoneType.CRITICAL -> Color.argb(80, 255, 82, 82) // Red for critical zones
                ZoneType.CAUTION -> Color.argb(80, 255, 193, 7) // Yellow for caution zones
            }

            val outlineColor = when (zone.type) {
                ZoneType.CRITICAL -> Color.argb(200, 255, 82, 82)
                ZoneType.CAUTION -> Color.argb(200, 255, 193, 7)
            }

            // Add larger circle overlay around danger zone (200m radius for better visibility)
            val dangerCircle = Polygon().apply {
                points = Polygon.pointsAsCircle(zone.location, 200.0) // 200 meter radius
                fillPaint.color = zoneColor
                outlinePaint.color = outlineColor
                outlinePaint.strokeWidth = 4f
            }
            binding.mapView.overlays.add(dangerCircle)

            // Add center marker with icon
            val marker = Marker(binding.mapView).apply {
                position = zone.location
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = zone.title
                snippet = zone.description
                
                // Custom marker icon based on zone type
                icon = createZoneMarkerDrawable(zone.type)
            }
            binding.mapView.overlays.add(marker)
        }
        
        binding.mapView.invalidate()
    }

    private fun createZoneMarkerDrawable(zoneType: ZoneType): android.graphics.drawable.Drawable {
        val size = (32 * resources.displayMetrics.density).toInt()
        val color = when (zoneType) {
            ZoneType.CRITICAL -> Color.rgb(255, 82, 82) // Red
            ZoneType.CAUTION -> Color.rgb(255, 193, 7) // Yellow
        }

        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(
                (3 * resources.displayMetrics.density).toInt(),
                Color.WHITE
            )
            setSize(size, size)
        }
        return drawable
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    /**
     * Enum representing different types of awareness zones
     */
    enum class ZoneType {
        CRITICAL,  // Headphones OFF always (red)
        CAUTION    // Headphones OK but be cautious (yellow)
    }

    /**
     * Data class representing a danger zone location
     */
    data class DangerZone(
        val location: GeoPoint,
        val title: String,
        val description: String,
        val type: ZoneType
    )
}

