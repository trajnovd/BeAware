package com.beaware.app.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.beaware.app.R
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

    // Dummy data: Level 1 critical alert locations
    // In a real app, this would come from a backend or local database
    private val dummyDangerZones = listOf(
        DangerZone(
            location = GeoPoint(41.9981, 21.4254), // Skopje, North Macedonia (central)
            title = "Critical Alert",
            description = "Screaming detected - 2 days ago"
        ),
        DangerZone(
            location = GeoPoint(42.0042, 21.4094), // Near City Park
            title = "Critical Alert",
            description = "Glass breaking + voices - 5 days ago"
        ),
        DangerZone(
            location = GeoPoint(41.9945, 21.4312), // Near old bazaar
            title = "Critical Alert",
            description = "Aggressive shouting - 1 week ago"
        ),
        DangerZone(
            location = GeoPoint(42.0034, 21.4452), // East area
            title = "Critical Alert",
            description = "Physical struggle sounds - 3 days ago"
        ),
        DangerZone(
            location = GeoPoint(41.9912, 21.4178), // South area
            title = "Critical Alert",
            description = "Shouting for help - 4 days ago"
        ),
        // Additional zones to show clustering
        DangerZone(
            location = GeoPoint(42.0012, 21.4234),
            title = "Critical Alert",
            description = "Running + shouting - 6 days ago"
        ),
        DangerZone(
            location = GeoPoint(41.9978, 21.4289),
            title = "Critical Alert",
            description = "Screaming detected - 1 week ago"
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
            // Add marker
            val marker = Marker(binding.mapView).apply {
                position = zone.location
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = zone.title
                snippet = zone.description
                
                // Custom red marker icon
                icon = createDangerMarkerDrawable()
            }
            binding.mapView.overlays.add(marker)
            
            // Add red circle overlay around danger zone
            val dangerCircle = Polygon().apply {
                points = Polygon.pointsAsCircle(zone.location, 100.0) // 100 meter radius
                fillPaint.color = Color.argb(60, 255, 0, 0) // Semi-transparent red
                outlinePaint.color = Color.argb(150, 255, 0, 0)
                outlinePaint.strokeWidth = 3f
            }
            binding.mapView.overlays.add(dangerCircle)
        }
        
        binding.mapView.invalidate()
    }

    private fun createDangerMarkerDrawable(): android.graphics.drawable.Drawable {
        val size = (24 * resources.displayMetrics.density).toInt()
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ContextCompat.getColor(this@MapActivity, R.color.alert_red))
            setStroke(
                (2 * resources.displayMetrics.density).toInt(),
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
     * Data class representing a danger zone location
     */
    data class DangerZone(
        val location: GeoPoint,
        val title: String,
        val description: String
    )
}

