package com.example.mentra.navigation.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mentra.navigation.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * ═══════════════════════════════════════════════════════════════════
 * NEXUS NAVIGATION - SUPER FUTURISTIC NAVIGATION MODAL
 * Glassmorphic, alien-like design with stunning visual effects
 * ═══════════════════════════════════════════════════════════════════
 */

// Nexus Navigation Color Palette
private object NavColors {
    val voidBlack = Color(0xFF020206)
    val deepSpace = Color(0xFF080810)
    val background = Color(0xFF080810)
    val surface = Color(0xFF0F1118)
    val glassSurface = Color(0xFF151820).copy(alpha = 0.88f)
    val glassCore = Color(0xFF0A0C12).copy(alpha = 0.92f)
    val cardSurface = Color(0xFF1A1D28)

    val primary = Color(0xFF00F5D4)      // Cyan
    val secondary = Color(0xFF7B61FF)    // Purple
    val accent = Color(0xFFFF2E63)       // Pink
    val warning = Color(0xFFFFD93D)      // Yellow
    val success = Color(0xFF00E676)      // Green
    val info = Color(0xFF569CD6)         // Blue

    val gpsActive = Color(0xFF00F5D4)
    val gpsInactive = Color(0xFF6B7280)
    val routeLine = Color(0xFF7B61FF)

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B8C4)
    val textMuted = Color(0xFF6B7280)
    val textDim = Color(0xFF4A5568)

    val borderGlow = Color(0xFF00F5D4).copy(alpha = 0.5f)
    val purpleGlow = Color(0xFF7B61FF).copy(alpha = 0.4f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusNavigationScreen(
    viewModel: NavigationViewModel = hiltViewModel(),
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val currentLocation by viewModel.currentLocation.collectAsState()
    val mapType by viewModel.mapType.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    val selectedDestination by viewModel.selectedDestination.collectAsState()
    val routeInfo by viewModel.routeInfo.collectAsState()

    var showMapTypePicker by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showStatsPanel by remember { mutableStateOf(true) }
    var showCompass by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "nav_effects")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val orbitalRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbital"
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val gpsIndicatorScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gpsScale"
    )

    // Auto-start tracking
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
        if (!isTracking) {
            viewModel.toggleTracking()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavColors.voidBlack)
    ) {
        // Animated background orbital effects
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = orbitalRotation }
                .drawBehind {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                NavColors.primary.copy(alpha = 0.03f * pulseAlpha),
                                Color.Transparent,
                                NavColors.secondary.copy(alpha = 0.03f * pulseAlpha),
                                Color.Transparent
                            )
                        ),
                        radius = size.maxDimension * 0.6f,
                        style = Stroke(width = 2f)
                    )
                }
        )

        // Map View
        NexusMapView(
            currentLocation = currentLocation,
            mapType = mapType,
            destination = selectedDestination,
            isTracking = isTracking,
            showCompass = showCompass,
            onMapReady = { map -> mapViewRef = map }
        )

        // Top Navigation Bar
        NexusTopBar(
            isTracking = isTracking,
            currentLocation = currentLocation,
            pulseAlpha = pulseAlpha,
            gpsScale = if (isTracking && currentLocation != null) gpsIndicatorScale else 1f,
            onClose = onClose,
            onMapTypeClick = { showMapTypePicker = true },
            onSearchClick = { showSearchSheet = true },
            onShareClick = { showShareSheet = true }
        )

        // Floating Action Panel (right side)
        NexusFloatingControls(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            showCompass = showCompass,
            isRecording = isRecording,
            pulseAlpha = pulseAlpha,
            onCenterLocation = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                currentLocation?.let { loc ->
                    mapViewRef?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
                }
            },
            onToggleCompass = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showCompass = !showCompass
            },
            onToggleRecording = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isRecording = !isRecording
                if (isRecording) {
                    viewModel.startRouteRecording()
                }
            },
            onZoomIn = {
                mapViewRef?.controller?.zoomIn()
            },
            onZoomOut = {
                mapViewRef?.controller?.zoomOut()
            }
        )

        // Bottom Stats Panel
        AnimatedVisibility(
            visible = showStatsPanel,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            NexusStatsPanel(
                currentLocation = currentLocation,
                routeInfo = routeInfo,
                isTracking = isTracking,
                isRecording = isRecording,
                pulseAlpha = pulseAlpha,
                onToggleTracking = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleTracking()
                },
                onMinimize = { showStatsPanel = false }
            )
        }

        // Collapsed stats indicator
        AnimatedVisibility(
            visible = !showStatsPanel,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            NexusExpandButton(
                onClick = { showStatsPanel = true },
                pulseAlpha = pulseAlpha
            )
        }

        // Loading overlay
        if (isTracking && currentLocation == null) {
            NexusLoadingOverlay(pulseAlpha = pulseAlpha)
        }

        // Map Type Picker
        if (showMapTypePicker) {
            NexusMapTypePicker(
                currentType = mapType,
                onTypeSelected = { type ->
                    viewModel.setMapType(type)
                    showMapTypePicker = false
                },
                onDismiss = { showMapTypePicker = false }
            )
        }

        // Search Sheet
        if (showSearchSheet) {
            NexusSearchSheet(
                currentLocation = currentLocation,
                onPOISelected = { poi ->
                    viewModel.navigateTo(poi)
                    showSearchSheet = false
                },
                onDismiss = { showSearchSheet = false }
            )
        }

        // Share Sheet
        if (showShareSheet) {
            NexusShareSheet(
                currentLocation = currentLocation,
                onDismiss = { showShareSheet = false }
            )
        }
    }
}

@Composable
private fun NexusMapView(
    currentLocation: NavigationLocation?,
    mapType: MapType,
    destination: NavigationLocation?,
    isTracking: Boolean,
    showCompass: Boolean,
    onMapReady: (MapView) -> Unit
) {
    val context = LocalContext.current
    val defaultLocation = GeoPoint(-1.2921, 36.8219) // Nairobi, Kenya

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                onMapReady(this)
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(16.0)
                controller.setCenter(defaultLocation)
                setBuiltInZoomControls(false)
                minZoomLevel = 4.0
                maxZoomLevel = 19.0

                // Enable compass if requested
                if (showCompass) {
                    val compassOverlay = CompassOverlay(ctx, this)
                    compassOverlay.enableCompass()
                    overlays.add(compassOverlay)
                }
            }
        },
        update = { mapView ->
            // Update tile source
            when (mapType) {
                MapType.SATELLITE -> mapView.setTileSource(TileSourceFactory.USGS_SAT)
                MapType.TERRAIN -> mapView.setTileSource(TileSourceFactory.OpenTopo)
                MapType.HYBRID -> mapView.setTileSource(TileSourceFactory.USGS_SAT)
                MapType.STREET -> mapView.setTileSource(TileSourceFactory.MAPNIK)
            }

            currentLocation?.let { location ->
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                mapView.controller.animateTo(geoPoint)
                mapView.overlays.clear()

                // Current location marker with custom styling
                val marker = Marker(mapView).apply {
                    position = geoPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "📍 Your Location"
                    snippet = buildString {
                        append("Lat: ${String.format("%.5f", location.latitude)}°\n")
                        append("Lon: ${String.format("%.5f", location.longitude)}°\n")
                        append("Alt: ${location.altitude.toInt()}m | Acc: ±${location.accuracy.toInt()}m")
                    }
                }
                mapView.overlays.add(marker)

                // Destination marker and route line
                destination?.let { dest ->
                    val destPoint = GeoPoint(dest.latitude, dest.longitude)

                    // Route line
                    val routeLine = Polyline().apply {
                        addPoint(geoPoint)
                        addPoint(destPoint)
                        outlinePaint.color = android.graphics.Color.parseColor("#7B61FF")
                        outlinePaint.strokeWidth = 8f
                    }
                    mapView.overlays.add(routeLine)

                    // Destination marker
                    val destMarker = Marker(mapView).apply {
                        position = destPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "🎯 Destination"
                    }
                    mapView.overlays.add(destMarker)
                }

                // Re-add compass if needed
                if (showCompass) {
                    val compassOverlay = CompassOverlay(context, mapView)
                    compassOverlay.enableCompass()
                    mapView.overlays.add(compassOverlay)
                }

                mapView.invalidate()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun NexusTopBar(
    isTracking: Boolean,
    currentLocation: NavigationLocation?,
    pulseAlpha: Float,
    gpsScale: Float,
    onClose: () -> Unit,
    onMapTypeClick: () -> Unit,
    onSearchClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        NavColors.glassCore,
                        NavColors.glassSurface
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        NavColors.primary.copy(alpha = 0.4f * pulseAlpha),
                        NavColors.secondary.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close button
            NexusIconButton(
                icon = Icons.Default.Close,
                onClick = onClose,
                tint = NavColors.accent,
                backgroundColor = NavColors.accent.copy(alpha = 0.15f),
                borderColor = NavColors.accent.copy(alpha = 0.4f * pulseAlpha)
            )

            // GPS Status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .scale(gpsScale)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTracking && currentLocation != null)
                                NavColors.gpsActive
                            else
                                NavColors.gpsInactive
                        )
                )
                Column {
                    Text(
                        text = if (isTracking && currentLocation != null) "GPS ACTIVE" else "GPS OFFLINE",
                        color = if (isTracking && currentLocation != null)
                            NavColors.gpsActive else NavColors.gpsInactive,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    if (currentLocation != null) {
                        Text(
                            text = "±${currentLocation.accuracy.toInt()}m accuracy",
                            color = NavColors.textMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NexusIconButton(
                    icon = Icons.Default.Layers,
                    onClick = onMapTypeClick,
                    tint = NavColors.secondary,
                    backgroundColor = NavColors.secondary.copy(alpha = 0.15f)
                )
                NexusIconButton(
                    icon = Icons.Default.Search,
                    onClick = onSearchClick,
                    tint = NavColors.primary,
                    backgroundColor = NavColors.primary.copy(alpha = 0.15f)
                )
                NexusIconButton(
                    icon = Icons.Default.Share,
                    onClick = onShareClick,
                    tint = NavColors.info,
                    backgroundColor = NavColors.info.copy(alpha = 0.15f)
                )
            }
        }
    }
}

@Composable
private fun NexusIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color,
    backgroundColor: Color,
    borderColor: Color = tint.copy(alpha = 0.3f),
    size: Int = 40
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(size.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size((size * 0.5f).dp)
        )
    }
}

@Composable
private fun NexusFloatingControls(
    modifier: Modifier = Modifier,
    showCompass: Boolean,
    isRecording: Boolean,
    pulseAlpha: Float,
    onCenterLocation: () -> Unit,
    onToggleCompass: () -> Unit,
    onToggleRecording: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(NavColors.glassCore)
            .border(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        NavColors.primary.copy(alpha = 0.3f),
                        NavColors.secondary.copy(alpha = 0.2f)
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Zoom In
        FloatingControlButton(
            icon = Icons.Default.Add,
            onClick = onZoomIn,
            tint = NavColors.textPrimary
        )

        // Zoom Out
        FloatingControlButton(
            icon = Icons.Default.Remove,
            onClick = onZoomOut,
            tint = NavColors.textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Center on location
        FloatingControlButton(
            icon = Icons.Default.MyLocation,
            onClick = onCenterLocation,
            tint = NavColors.primary,
            isHighlighted = true,
            pulseAlpha = pulseAlpha
        )

        // Compass toggle
        FloatingControlButton(
            icon = Icons.Default.Explore,
            onClick = onToggleCompass,
            tint = if (showCompass) NavColors.warning else NavColors.textMuted,
            isActive = showCompass
        )

        // Recording toggle
        FloatingControlButton(
            icon = Icons.Default.FiberManualRecord,
            onClick = onToggleRecording,
            tint = if (isRecording) NavColors.accent else NavColors.textMuted,
            isActive = isRecording,
            pulseAlpha = if (isRecording) pulseAlpha else 1f
        )
    }
}

@Composable
private fun FloatingControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color,
    isHighlighted: Boolean = false,
    isActive: Boolean = false,
    pulseAlpha: Float = 1f
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                if (isHighlighted || isActive)
                    tint.copy(alpha = 0.15f * pulseAlpha)
                else
                    NavColors.surface.copy(alpha = 0.5f)
            )
            .then(
                if (isHighlighted) Modifier.border(
                    1.dp,
                    tint.copy(alpha = 0.5f * pulseAlpha),
                    CircleShape
                ) else Modifier
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun NexusStatsPanel(
    currentLocation: NavigationLocation?,
    routeInfo: RouteStatistics?,
    isTracking: Boolean,
    isRecording: Boolean,
    pulseAlpha: Float,
    onToggleTracking: () -> Unit,
    onMinimize: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NavColors.glassCore,
                        NavColors.deepSpace.copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NavColors.primary.copy(alpha = 0.5f * pulseAlpha),
                        NavColors.secondary.copy(alpha = 0.3f),
                        NavColors.accent.copy(alpha = 0.2f * pulseAlpha)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .drawBehind {
                // Inner glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NavColors.primary.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    ),
                    radius = size.width * 0.4f,
                    center = Offset(size.width * 0.2f, size.height * 0.3f)
                )
            }
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header with minimize
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isTracking) NavColors.gpsActive else NavColors.gpsInactive
                            )
                    )
                    Text(
                        text = "◈ LOCATION DATA",
                        color = NavColors.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .background(
                                    NavColors.accent.copy(alpha = 0.2f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "● REC",
                                color = NavColors.accent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onMinimize,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = "Minimize",
                        tint = NavColors.textMuted
                    )
                }
            }

            // Coordinates display
            if (currentLocation != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NavColors.surface.copy(alpha = 0.5f))
                        .border(1.dp, NavColors.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "COORDINATES",
                            color = NavColors.textMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = String.format("%.6f°, %.6f°",
                                currentLocation.latitude,
                                currentLocation.longitude
                            ),
                            color = NavColors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Icon(
                        Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = NavColors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NexusStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Speed,
                        value = String.format("%.1f", currentLocation.speed * 3.6),
                        unit = "km/h",
                        label = "SPEED",
                        color = NavColors.info
                    )
                    NexusStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Terrain,
                        value = "${currentLocation.altitude.toInt()}",
                        unit = "m",
                        label = "ALTITUDE",
                        color = NavColors.warning
                    )
                    NexusStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Explore,
                        value = "${currentLocation.bearing.toInt()}",
                        unit = "°",
                        label = "BEARING",
                        color = NavColors.secondary
                    )
                }

                // Route info if available
                routeInfo?.let { info ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NavColors.routeLine.copy(alpha = 0.1f))
                            .border(1.dp, NavColors.routeLine.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RouteInfoItem(
                            label = "DISTANCE",
                            value = String.format("%.2f km", info.totalDistanceKm),
                            color = NavColors.routeLine
                        )
                        RouteInfoItem(
                            label = "ELEVATION",
                            value = "${info.elevationGainMeters.toInt()}m",
                            color = NavColors.warning
                        )
                        RouteInfoItem(
                            label = "AVG SPEED",
                            value = String.format("%.1f km/h", info.averageSpeedKmh),
                            color = NavColors.info
                        )
                    }
                }

                // Tracking toggle button
                Button(
                    onClick = onToggleTracking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTracking)
                            NavColors.accent.copy(alpha = 0.9f)
                        else
                            NavColors.primary.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTracking) "STOP TRACKING" else "START TRACKING",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            } else {
                // No location yet
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = NavColors.primary,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Acquiring GPS signal...",
                            color = NavColors.textMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NexusStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    unit: String,
    label: String,
    color: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(NavColors.surface.copy(alpha = 0.6f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                color = NavColors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = unit,
                color = color,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        Text(
            text = label,
            color = NavColors.textMuted,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun RouteInfoItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = NavColors.textMuted,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun NexusExpandButton(
    onClick: () -> Unit,
    pulseAlpha: Float
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(NavColors.glassCore)
            .border(
                1.dp,
                NavColors.primary.copy(alpha = 0.4f * pulseAlpha),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.ExpandLess,
                contentDescription = null,
                tint = NavColors.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "SHOW STATS",
                color = NavColors.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun NexusLoadingOverlay(pulseAlpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(NavColors.glassCore)
                .border(
                    1.dp,
                    NavColors.primary.copy(alpha = 0.3f * pulseAlpha),
                    RoundedCornerShape(24.dp)
                )
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = NavColors.primary,
                modifier = Modifier.size(56.dp),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "◈ ACQUIRING GPS",
                color = NavColors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Connecting to satellites...",
                color = NavColors.textMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NexusMapTypePicker(
    currentType: MapType,
    onTypeSelected: (MapType) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(28.dp))
                .background(NavColors.glassCore)
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            NavColors.primary.copy(alpha = 0.4f),
                            NavColors.secondary.copy(alpha = 0.3f)
                        )
                    ),
                    RoundedCornerShape(28.dp)
                )
                .clickable(enabled = false) { }
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "◈ MAP STYLE",
                color = NavColors.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            MapType.values().forEach { type ->
                MapTypeOption(
                    type = type,
                    isSelected = currentType == type,
                    onClick = { onTypeSelected(type) }
                )
            }
        }
    }
}

@Composable
private fun MapTypeOption(
    type: MapType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (type) {
        MapType.SATELLITE -> Icons.Default.Satellite
        MapType.TERRAIN -> Icons.Default.Terrain
        MapType.HYBRID -> Icons.Default.Layers
        MapType.STREET -> Icons.Default.Map
    }

    val description = when (type) {
        MapType.SATELLITE -> "Satellite imagery view"
        MapType.TERRAIN -> "Topographic elevation map"
        MapType.HYBRID -> "Satellite with labels"
        MapType.STREET -> "Standard street map"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) NavColors.primary.copy(alpha = 0.15f)
                else NavColors.surface.copy(alpha = 0.3f)
            )
            .border(
                1.dp,
                if (isSelected) NavColors.primary.copy(alpha = 0.5f)
                else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) NavColors.primary else NavColors.textSecondary,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = type.name,
                    color = if (isSelected) NavColors.primary else NavColors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = description,
                    color = NavColors.textMuted,
                    fontSize = 11.sp
                )
            }
        }

        if (isSelected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = NavColors.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NexusSearchSheet(
    currentLocation: NavigationLocation?,
    onPOISelected: (PointOfInterest) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val categories = listOf(
        "🍽️ Restaurants" to POICategory.RESTAURANT,
        "⛽ Gas Stations" to POICategory.GAS_STATION,
        "🏨 Hotels" to POICategory.HOTEL,
        "🏥 Hospitals" to POICategory.HOSPITAL,
        "🏪 Shopping" to POICategory.SHOPPING,
        "🅿️ Parking" to POICategory.PARKING,
        "🏦 Banks" to POICategory.BANK,
        "🎯 Attractions" to POICategory.ATTRACTION
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f)
                .clip(RoundedCornerShape(28.dp))
                .background(NavColors.glassCore)
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            NavColors.primary.copy(alpha = 0.4f),
                            NavColors.secondary.copy(alpha = 0.2f)
                        )
                    ),
                    RoundedCornerShape(28.dp)
                )
                .clickable(enabled = false) { }
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "◈ SEARCH PLACES",
                    color = NavColors.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = NavColors.textMuted
                    )
                }
            }

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Search restaurants, hotels...",
                        color = NavColors.textMuted,
                        fontFamily = FontFamily.Monospace
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = NavColors.primary
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NavColors.textPrimary,
                    unfocusedTextColor = NavColors.textPrimary,
                    focusedBorderColor = NavColors.primary,
                    unfocusedBorderColor = NavColors.textMuted.copy(alpha = 0.3f),
                    cursorColor = NavColors.primary
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            // Categories
            Text(
                text = "QUICK SEARCH",
                color = NavColors.textMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { (label, category) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NavColors.surface.copy(alpha = 0.5f))
                            .clickable {
                                // Create sample POI for now
                                currentLocation?.let { loc ->
                                    onPOISelected(
                                        PointOfInterest(
                                            id = UUID.randomUUID().toString(),
                                            name = label,
                                            category = category,
                                            latitude = loc.latitude + 0.01,
                                            longitude = loc.longitude + 0.01
                                        )
                                    )
                                }
                            }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            color = NavColors.textPrimary,
                            fontSize = 14.sp
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = NavColors.textMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NexusShareSheet(
    currentLocation: NavigationLocation?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(28.dp))
                .background(NavColors.glassCore)
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            NavColors.info.copy(alpha = 0.4f),
                            NavColors.primary.copy(alpha = 0.2f)
                        )
                    ),
                    RoundedCornerShape(28.dp)
                )
                .clickable(enabled = false) { }
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "◈ SHARE LOCATION",
                    color = NavColors.info,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = NavColors.textMuted
                    )
                }
            }

            currentLocation?.let { location ->
                // Location card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NavColors.surface.copy(alpha = 0.6f))
                        .border(1.dp, NavColors.info.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LocationInfoRow("Latitude", "${location.latitude}°")
                    LocationInfoRow("Longitude", "${location.longitude}°")
                    LocationInfoRow("Altitude", "${location.altitude.toInt()}m")
                    LocationInfoRow("Accuracy", "±${location.accuracy.toInt()}m")
                }

                // Share options
                val shareOptions = listOf(
                    Triple(Icons.Default.Message, "Share via SMS", NavColors.primary),
                    Triple(Icons.Default.Email, "Share via Email", NavColors.info),
                    Triple(Icons.Default.ContentCopy, "Copy Coordinates", NavColors.warning),
                    Triple(Icons.Default.Map, "Open in Maps", NavColors.success)
                )

                shareOptions.forEach { (icon, label, color) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(color.copy(alpha = 0.1f))
                            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .clickable {
                                when (label) {
                                    "Open in Maps" -> {
                                        val uri = Uri.parse(
                                            "geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}"
                                        )
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }
                                    "Share via SMS" -> {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("smsto:")
                                            putExtra(
                                                "sms_body",
                                                "My location: https://maps.google.com/?q=${location.latitude},${location.longitude}"
                                            )
                                        }
                                        context.startActivity(intent)
                                    }
                                    // Add more share actions
                                }
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = label,
                            color = NavColors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Location not available",
                        color = NavColors.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = NavColors.textMuted,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = NavColors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// Add UUID import for PointOfInterest
private object UUID {
    fun randomUUID() = java.util.UUID.randomUUID()
}

