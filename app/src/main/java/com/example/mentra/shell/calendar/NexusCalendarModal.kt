package com.example.mentra.shell.calendar

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

/**
 * ═══════════════════════════════════════════════════════════════════
 * NEXUS CALENDAR MODAL - ALIEN GLASSMORPHIC DESIGN
 * Super futuristic, stunning calendar UI/UX
 * ═══════════════════════════════════════════════════════════════════
 */

private object NexusCalendarColors {
    val voidBlack = Color(0xFF010104)
    val deepSpace = Color(0xFF050508)
    val glassCore = Color(0xFF0A0C14).copy(alpha = 0.94f)
    val glassSurface = Color(0xFF12151F).copy(alpha = 0.88f)
    val cardSurface = Color(0xFF151A28)

    val hologramBlue = Color(0xFF00D4FF)
    val neonCyan = Color(0xFF00F5D4)
    val electricPurple = Color(0xFF7B61FF)
    val plasmaRed = Color(0xFFFF2E63)
    val solarYellow = Color(0xFFFFD93D)
    val matrixGreen = Color(0xFF00E676)
    val cosmicOrange = Color(0xFFFF8C00)

    val today = Color(0xFF00F5D4)
    val selected = Color(0xFF7B61FF)
    val weekend = Color(0xFFFF2E63).copy(alpha = 0.4f)
    val eventDot = Color(0xFFFFD93D)

    val textPure = Color(0xFFFFFFFF)
    val textDim = Color(0xFF8892A4)
    val textMuted = Color(0xFF4A5568)
    val textFaded = Color(0xFF2D3748)

    val glowCyan = Color(0xFF00F5D4).copy(alpha = 0.5f)
    val glowPurple = Color(0xFF7B61FF).copy(alpha = 0.4f)
}

data class NexusEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: LocalDate,
    val time: LocalTime? = null,
    val color: Color = NexusCalendarColors.neonCyan,
    val description: String = ""
)

@Composable
fun NexusCalendarModal(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var events by remember { mutableStateOf(getSampleEvents()) }
    var showAddEvent by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "calendar_fx")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
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

    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    // Full screen backdrop
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        // Animated orbital rings background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = orbitalRotation }
                .drawBehind {
                    // Outer orbital ring
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                NexusCalendarColors.neonCyan.copy(alpha = 0.06f * pulseAlpha),
                                Color.Transparent,
                                NexusCalendarColors.electricPurple.copy(alpha = 0.06f * pulseAlpha),
                                Color.Transparent,
                                NexusCalendarColors.plasmaRed.copy(alpha = 0.04f * pulseAlpha),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension * 0.55f * glowScale,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                    // Inner ring
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                NexusCalendarColors.solarYellow.copy(alpha = 0.04f * pulseAlpha),
                                Color.Transparent,
                                NexusCalendarColors.matrixGreen.copy(alpha = 0.04f * pulseAlpha),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension * 0.4f * glowScale,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                    )
                }
        )

        // Main Modal Container
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(36.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NexusCalendarColors.glassCore,
                            NexusCalendarColors.deepSpace.copy(alpha = 0.97f)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            NexusCalendarColors.neonCyan.copy(alpha = 0.5f * pulseAlpha),
                            NexusCalendarColors.electricPurple.copy(alpha = 0.4f),
                            NexusCalendarColors.plasmaRed.copy(alpha = 0.3f * pulseAlpha)
                        )
                    ),
                    shape = RoundedCornerShape(36.dp)
                )
                .drawBehind {
                    // Corner glow effects
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NexusCalendarColors.neonCyan.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        ),
                        radius = size.width * 0.4f,
                        center = Offset(0f, 0f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NexusCalendarColors.electricPurple.copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        ),
                        radius = size.width * 0.3f,
                        center = Offset(size.width, size.height)
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                NexusCalendarHeader(
                    currentMonth = currentMonth,
                    onPrevMonth = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentMonth = currentMonth.minusMonths(1)
                    },
                    onNextMonth = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentMonth = currentMonth.plusMonths(1)
                    },
                    onToday = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentMonth = YearMonth.now()
                        selectedDate = LocalDate.now()
                    },
                    onClose = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClose()
                    },
                    pulseAlpha = pulseAlpha
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Month/Year Display
                NexusMonthDisplay(
                    currentMonth = currentMonth,
                    pulseAlpha = pulseAlpha
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Weekday headers
                NexusWeekdayHeaders()

                Spacer(modifier = Modifier.height(8.dp))

                // Calendar Grid
                NexusCalendarGrid(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    events = events,
                    onDateSelect = { date ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedDate = date
                    },
                    pulseAlpha = pulseAlpha,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Selected Date Events Panel
                NexusEventsPanel(
                    selectedDate = selectedDate,
                    events = events.filter { it.date == selectedDate },
                    onAddEvent = { showAddEvent = true },
                    pulseAlpha = pulseAlpha
                )
            }
        }

        // Add Event Dialog
        if (showAddEvent) {
            NexusAddEventDialog(
                selectedDate = selectedDate,
                onDismiss = { showAddEvent = false },
                onAddEvent = { event ->
                    events = events + event
                    showAddEvent = false
                }
            )
        }
    }
}

@Composable
private fun NexusCalendarHeader(
    currentMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onClose: () -> Unit,
    pulseAlpha: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(NexusCalendarColors.plasmaRed.copy(alpha = 0.15f))
                .border(1.dp, NexusCalendarColors.plasmaRed.copy(alpha = 0.5f * pulseAlpha), CircleShape)
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = NexusCalendarColors.plasmaRed,
                modifier = Modifier.size(20.dp)
            )
        }

        // Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(NexusCalendarColors.neonCyan)
            )
            Text(
                text = "◈ NEXUS",
                color = NexusCalendarColors.neonCyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 3.sp
            )
            Text(
                text = "CALENDAR",
                color = NexusCalendarColors.electricPurple,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Today button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(NexusCalendarColors.matrixGreen.copy(alpha = 0.15f))
                .border(1.dp, NexusCalendarColors.matrixGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .clickable { onToday() }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = "TODAY",
                color = NexusCalendarColors.matrixGreen,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NexusMonthDisplay(
    currentMonth: YearMonth,
    pulseAlpha: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        NexusCalendarColors.glassSurface,
                        NexusCalendarColors.cardSurface.copy(alpha = 0.6f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        NexusCalendarColors.neonCyan.copy(alpha = 0.3f * pulseAlpha),
                        NexusCalendarColors.electricPurple.copy(alpha = 0.2f)
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase(),
                color = NexusCalendarColors.textPure,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp
            )

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currentMonth.year.toString(),
                    color = NexusCalendarColors.neonCyan,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "◈ ${currentMonth.lengthOfMonth()} DAYS",
                    color = NexusCalendarColors.textDim,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun NexusWeekdayHeaders() {
    val weekdays = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekdays.forEachIndexed { index, day ->
            val isWeekend = index == 0 || index == 6
            Text(
                text = day,
                color = if (isWeekend) NexusCalendarColors.plasmaRed.copy(alpha = 0.7f)
                        else NexusCalendarColors.textDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NexusCalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    events: List<NexusEvent>,
    onDateSelect: (LocalDate) -> Unit,
    pulseAlpha: Float,
    modifier: Modifier = Modifier
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val startOffset = firstDayOfMonth.dayOfWeek.value % 7

    val days = buildList {
        repeat(startOffset) { add(null) }
        for (day in 1..daysInMonth) {
            add(currentMonth.atDay(day))
        }
        while (size % 7 != 0) { add(null) }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(days) { date ->
            NexusDayCell(
                date = date,
                isSelected = date == selectedDate,
                isToday = date == LocalDate.now(),
                hasEvents = events.any { it.date == date },
                isCurrentMonth = date?.month == currentMonth.month,
                onSelect = { date?.let { onDateSelect(it) } },
                pulseAlpha = pulseAlpha
            )
        }
    }
}

@Composable
private fun NexusDayCell(
    date: LocalDate?,
    isSelected: Boolean,
    isToday: Boolean,
    hasEvents: Boolean,
    isCurrentMonth: Boolean,
    onSelect: () -> Unit,
    pulseAlpha: Float
) {
    if (date == null) {
        Box(modifier = Modifier.aspectRatio(1f))
        return
    }

    val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY

    val backgroundColor = when {
        isSelected -> NexusCalendarColors.selected.copy(alpha = 0.3f)
        isToday -> NexusCalendarColors.today.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val borderColor = when {
        isSelected -> NexusCalendarColors.selected
        isToday -> NexusCalendarColors.today.copy(alpha = 0.8f * pulseAlpha)
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> NexusCalendarColors.textPure
        isToday -> NexusCalendarColors.today
        isWeekend -> NexusCalendarColors.plasmaRed.copy(alpha = 0.7f)
        !isCurrentMonth -> NexusCalendarColors.textFaded
        else -> NexusCalendarColors.textDim
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(
                if (borderColor != Color.Transparent)
                    Modifier.border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable { onSelect() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = textColor,
                fontSize = 16.sp,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )

            if (hasEvents) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(NexusCalendarColors.eventDot)
                )
            }
        }
    }
}

@Composable
private fun NexusEventsPanel(
    selectedDate: LocalDate,
    events: List<NexusEvent>,
    onAddEvent: () -> Unit,
    pulseAlpha: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NexusCalendarColors.glassSurface,
                        NexusCalendarColors.cardSurface.copy(alpha = 0.8f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        NexusCalendarColors.neonCyan.copy(alpha = 0.3f * pulseAlpha),
                        NexusCalendarColors.electricPurple.copy(alpha = 0.2f)
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE")).uppercase(),
                        color = NexusCalendarColors.neonCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                        color = NexusCalendarColors.textDim,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NexusCalendarColors.matrixGreen.copy(alpha = 0.2f))
                        .border(1.dp, NexusCalendarColors.matrixGreen.copy(alpha = 0.5f), CircleShape)
                        .clickable { onAddEvent() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Event",
                        tint = NexusCalendarColors.matrixGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (events.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "◈ NO EVENTS",
                        color = NexusCalendarColors.textMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(events) { event ->
                        NexusEventItem(event = event)
                    }
                }
            }
        }
    }
}

@Composable
private fun NexusEventItem(event: NexusEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(NexusCalendarColors.cardSurface.copy(alpha = 0.6f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(event.color)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                color = NexusCalendarColors.textPure,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
            event.time?.let { time ->
                Text(
                    text = time.format(DateTimeFormatter.ofPattern("HH:mm")),
                    color = NexusCalendarColors.textDim,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun NexusAddEventDialog(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onAddEvent: (NexusEvent) -> Unit
) {
    var title by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(NexusCalendarColors.glassCore)
                .border(
                    1.5.dp,
                    NexusCalendarColors.neonCyan.copy(alpha = 0.4f),
                    RoundedCornerShape(24.dp)
                )
                .clickable(enabled = false) { }
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "◈ NEW EVENT",
                    color = NexusCalendarColors.neonCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title", fontFamily = FontFamily.Monospace) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexusCalendarColors.neonCyan,
                        unfocusedBorderColor = NexusCalendarColors.textMuted,
                        focusedLabelColor = NexusCalendarColors.neonCyan,
                        cursorColor = NexusCalendarColors.neonCyan,
                        focusedTextColor = NexusCalendarColors.textPure,
                        unfocusedTextColor = NexusCalendarColors.textDim
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NexusCalendarColors.textMuted.copy(alpha = 0.2f))
                            .clickable { onDismiss() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CANCEL",
                            color = NexusCalendarColors.textDim,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NexusCalendarColors.neonCyan)
                            .clickable {
                                if (title.isNotBlank()) {
                                    onAddEvent(NexusEvent(title = title, date = selectedDate))
                                }
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ADD EVENT",
                            color = NexusCalendarColors.voidBlack,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun getSampleEvents(): List<NexusEvent> {
    val today = LocalDate.now()
    return listOf(
        NexusEvent(
            title = "System Sync",
            date = today,
            time = LocalTime.of(9, 0),
            color = NexusCalendarColors.neonCyan
        ),
        NexusEvent(
            title = "Neural Update",
            date = today.plusDays(2),
            time = LocalTime.of(14, 30),
            color = NexusCalendarColors.electricPurple
        ),
        NexusEvent(
            title = "Core Maintenance",
            date = today.plusDays(5),
            time = LocalTime.of(10, 0),
            color = NexusCalendarColors.matrixGreen
        )
    )
}

