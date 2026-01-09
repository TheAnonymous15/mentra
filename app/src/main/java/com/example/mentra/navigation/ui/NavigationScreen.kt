package com.example.mentra.navigation.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Next-Level Navigation Screen
 * Features:
 * - OpenStreetMap integration
 * - Real-time GPS tracking
 * - Distance/bearing calculations
 * - Location sharing
 * - Multiple map types
 * - Stunning glassmorphic UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    viewModel: NavigationViewModel = hiltViewModel()
) {
    val currentLocation by viewModel.currentLocation.collectAsState()
    val mapType by viewModel.mapType.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    val selectedDestination by viewModel.selectedDestination.collectAsState()
    val routeInfo by viewModel.routeInfo.collectAsState()

    var showMapTypePicker by remember { mutableStateOf(false) }
    var showPOISearch by remember { mutableStateOf(false) }
    var showLocationShare by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E27),
                        Color(0xFF1A1F3A)
                    )
                )
            )
    ) {
        // OSM Map View
        OSMMapView(
            currentLocation = currentLocation,
            mapType = mapType,
            destination = selectedDestination
        )

        // Top Glass Control Panel
        TopNavigationPanel(
            mapType = mapType,
            onMapTypeClick = { showMapTypePicker = true },
            onSearchClick = { showPOISearch = true },
            onShareClick = { showLocationShare = true }
        )

        // Bottom Stats Panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentLocation != null) {
                LocationStatsCard(
                    location = currentLocation!!,
                    isTracking = isTracking,
                    onToggleTracking = { viewModel.toggleTracking() }
                )
            }
        }

        // Floating Action Buttons
        NavigationFABs(
            onCenterLocation = { viewModel.centerOnCurrentLocation() },
            onRecordRoute = { viewModel.startRouteRecording() },
            onCompass = { viewModel.toggleCompass() }
        )

        // Map Type Picker Sheet
        if (showMapTypePicker) {
            MapTypePickerSheet(
                currentType = mapType,
                onTypeSelected = { type ->
                    viewModel.setMapType(type)
                    showMapTypePicker = false
                },
                onDismiss = { showMapTypePicker = false }
            )
        }

        // POI Search Sheet
        if (showPOISearch) {
            POISearchSheet(
                currentLocation = currentLocation,
                onPOISelected = { poi ->
                    viewModel.navigateTo(poi)
                    showPOISearch = false
                },
                onDismiss = { showPOISearch = false }
            )
        }

        // Location Share Sheet
        if (showLocationShare) {
            LocationShareSheet(
                currentLocation = currentLocation,
                onDismiss = { showLocationShare = false }
            )
        }
    }
}

@Composable
fun OSMMapView(
    currentLocation: com.example.mentra.navigation.NavigationLocation?,
    mapType: MapType,
    destination: com.example.mentra.navigation.NavigationLocation?
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(0.0, 0.0))
            }
        },
        update = { mapView ->
            when (mapType) {
                MapType.SATELLITE -> mapView.setTileSource(TileSourceFactory.USGS_SAT)
                MapType.TERRAIN -> mapView.setTileSource(TileSourceFactory.OpenTopo)
                MapType.HYBRID -> mapView.setTileSource(TileSourceFactory.USGS_SAT)
                MapType.STREET -> mapView.setTileSource(TileSourceFactory.MAPNIK)
            }

            currentLocation?.let { location ->
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                mapView.controller.setCenter(geoPoint)
                mapView.overlays.clear()

                val marker = Marker(mapView).apply {
                    position = geoPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "You are here"
                }
                mapView.overlays.add(marker)

                destination?.let { dest ->
                    val destMarker = Marker(mapView).apply {
                        position = GeoPoint(dest.latitude, dest.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Destination"
                    }
                    mapView.overlays.add(destMarker)
                }

                mapView.invalidate()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun TopNavigationPanel(
    mapType: MapType,
    onMapTypeClick: () -> Unit,
    onSearchClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = Color(0xFF1A1F3A).copy(alpha = 0.9f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassButton(
                icon = Icons.Default.Layers,
                text = mapType.name,
                onClick = onMapTypeClick
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color(0xFF4EC9B0).copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    Icon(Icons.Default.Search, "Search", tint = Color(0xFF4EC9B0))
                }

                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color(0xFF569CD6).copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    Icon(Icons.Default.Share, "Share", tint = Color(0xFF569CD6))
                }
            }
        }
    }
}

@Composable
fun LocationStatsCard(
    location: com.example.mentra.navigation.NavigationLocation,
    isTracking: Boolean,
    onToggleTracking: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1A1F3A).copy(alpha = 0.95f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "GPS Location",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = String.format("%.6f, %.6f", location.latitude, location.longitude),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.GpsFixed,
                        "GPS",
                        tint = Color(0xFF4EC9B0),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "±${location.accuracy.toInt()}m",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF4EC9B0),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.Speed,
                    value = String.format("%.1f", location.speed * 3.6),
                    label = "km/h",
                    color = Color(0xFF569CD6)
                )
                StatItem(
                    icon = Icons.Default.Terrain,
                    value = "${location.altitude.toInt()}",
                    label = "m alt",
                    color = Color(0xFFCE9178)
                )
                StatItem(
                    icon = Icons.Default.Explore,
                    value = "${location.bearing.toInt()}°",
                    label = "bearing",
                    color = Color(0xFFC586C0)
                )
            }

            Button(
                onClick = onToggleTracking,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTracking) Color(0xFFCE9178) else Color(0xFF4EC9B0)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    if (isTracking) Icons.Default.Stop else Icons.Default.MyLocation,
                    contentDescription = if (isTracking) "Stop" else "Start"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTracking) "Stop Tracking" else "Start Tracking")
            }
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, label, tint = color, modifier = Modifier.size(24.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun GlassButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color(0xFF4EC9B0).copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, text, tint = Color(0xFF4EC9B0), modifier = Modifier.size(20.dp))
            Text(text = text, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
    }
}

@Composable
fun NavigationFABs(
    onCenterLocation: () -> Unit,
    onRecordRoute: () -> Unit,
    onCompass: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        FloatingActionButton(
            onClick = onCompass,
            containerColor = Color(0xFF569CD6),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(Icons.Default.Explore, "Compass", tint = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        FloatingActionButton(
            onClick = onRecordRoute,
            containerColor = Color(0xFFCE9178),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(Icons.Default.FiberManualRecord, "Record", tint = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        FloatingActionButton(
            onClick = onCenterLocation,
            containerColor = Color(0xFF4EC9B0),
            modifier = Modifier.size(64.dp)
        ) {
            Icon(Icons.Default.MyLocation, "Center", tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapTypePickerSheet(
    currentType: MapType,
    onTypeSelected: (MapType) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1F3A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "MAP TYPES",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF4EC9B0),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            MapTypeOption(MapType.SATELLITE, Icons.Default.Satellite, "Satellite imagery", currentType == MapType.SATELLITE) { onTypeSelected(MapType.SATELLITE) }
            MapTypeOption(MapType.TERRAIN, Icons.Default.Terrain, "Topographic map", currentType == MapType.TERRAIN) { onTypeSelected(MapType.TERRAIN) }
            MapTypeOption(MapType.HYBRID, Icons.Default.Layers, "Satellite + labels", currentType == MapType.HYBRID) { onTypeSelected(MapType.HYBRID) }
            MapTypeOption(MapType.STREET, Icons.Default.Map, "Street map", currentType == MapType.STREET) { onTypeSelected(MapType.STREET) }
        }
    }
}

@Composable
fun MapTypeOption(
    type: MapType,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF4EC9B0).copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, type.name, tint = if (isSelected) Color(0xFF4EC9B0) else Color.White, modifier = Modifier.size(32.dp))
                Column {
                    Text(text = type.name, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(text = description, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, "Selected", tint = Color(0xFF4EC9B0), modifier = Modifier.size(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POISearchSheet(
    currentLocation: com.example.mentra.navigation.NavigationLocation?,
    onPOISelected: (com.example.mentra.navigation.PointOfInterest) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1F3A)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "SEARCH PLACES",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF4EC9B0),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search restaurants, hotels...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, "Search", tint = Color(0xFF4EC9B0)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4EC9B0),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Text(text = "Categories", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                POICategoryChip("🍽️ Food") { searchQuery = "restaurants" }
                POICategoryChip("⛽ Gas") { searchQuery = "gas station" }
                POICategoryChip("🏨 Hotels") { searchQuery = "hotels" }
                POICategoryChip("🏥 Hospital") { searchQuery = "hospital" }
            }
        }
    }
}

@Composable
fun POICategoryChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color(0xFF569CD6).copy(alpha = 0.2f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(text = text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color.White, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationShareSheet(
    currentLocation: com.example.mentra.navigation.NavigationLocation?,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1F3A)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "SHARE LOCATION",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF4EC9B0),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            currentLocation?.let { location ->
                Surface(
                    color = Color(0xFF2A2F4A).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CoordinateRow("Latitude", String.format("%.6f°", location.latitude))
                        CoordinateRow("Longitude", String.format("%.6f°", location.longitude))
                        CoordinateRow("Altitude", "${location.altitude.toInt()}m")
                        CoordinateRow("Accuracy", "±${location.accuracy.toInt()}m")
                    }
                }

                ShareButton(Icons.Default.Message, "Share via SMS", Color(0xFF4EC9B0))
                ShareButton(Icons.Default.Email, "Share via Email", Color(0xFF569CD6))
                ShareButton(Icons.Default.ContentCopy, "Copy Coordinates", Color(0xFFCE9178))
            }
        }
    }
}

@Composable
fun CoordinateRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun ShareButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Surface(
        onClick = { /* Handle share */ },
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, text, tint = color, modifier = Modifier.size(24.dp))
            Text(text = text, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}

enum class MapType {
    SATELLITE, TERRAIN, HYBRID, STREET
}

