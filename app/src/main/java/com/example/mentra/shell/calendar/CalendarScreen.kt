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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

/**
 * ═══════════════════════════════════════════════════════════════════
 * MENTRA NEXUS CALENDAR - MODAL EDITION
 * Super futuristic glassmorphic calendar modal
 * ═══════════════════════════════════════════════════════════════════
 */

// Color palette - Futuristic neon theme
private object CalendarColors {
    val voidBlack = Color(0xFF020206)
    val deepSpace = Color(0xFF080810)
    val background = Color(0xFF080810)
    val surface = Color(0xFF0F1118)
    val glassSurface = Color(0xFF151820).copy(alpha = 0.85f)
    val glassCore = Color(0xFF0A0C12).copy(alpha = 0.92f)
    val cardSurface = Color(0xFF1A1D28)

    val primary = Color(0xFF00F5D4)      // Cyan
    val secondary = Color(0xFF7B61FF)    // Purple
    val accent = Color(0xFFFF2E63)       // Pink
    val warning = Color(0xFFFFD93D)      // Yellow
    val success = Color(0xFF00E676)      // Green

    val today = Color(0xFF00F5D4)
    val selected = Color(0xFF7B61FF)
    val weekend = Color(0xFFFF2E63).copy(alpha = 0.3f)
    val eventDot = Color(0xFFFFD93D)

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B8C4)
    val textMuted = Color(0xFF6B7280)
    val textDim = Color(0xFF4A5568)

    val borderGlow = Color(0xFF00F5D4).copy(alpha = 0.4f)
    val purpleGlow = Color(0xFF7B61FF).copy(alpha = 0.4f)
    val gridLine = Color(0xFF2D3748).copy(alpha = 0.5f)
}

// Event data class
data class CalendarEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: LocalDate,
    val time: LocalTime? = null,
    val color: Color = CalendarColors.primary,
    val description: String = "",
    val isAllDay: Boolean = false
)

@Composable
fun CalendarScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTH) }
    var events by remember { mutableStateOf(getSampleEvents()) }
    var showAddEvent by remember { mutableStateOf(false) }
    var showEventDetails by remember { mutableStateOf<CalendarEvent?>(null) }

    val scope = rememberCoroutineScope()

    // Animated effects
    val infiniteTransition = rememberInfiniteTransition(label = "calendar_effects")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val orbitalRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbital"
    )

    val glowSize by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowSize"
    )

    // Full screen backdrop
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        // Animated background effects
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = orbitalRotation }
                .drawBehind {
                    // Orbital rings
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                CalendarColors.secondary.copy(alpha = 0.08f * pulseAlpha),
                                Color.Transparent,
                                CalendarColors.primary.copy(alpha = 0.08f * pulseAlpha),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension * 0.55f * glowSize,
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                CalendarColors.accent.copy(alpha = 0.05f * pulseAlpha),
                                Color.Transparent,
                                CalendarColors.warning.copy(alpha = 0.05f * pulseAlpha),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension * 0.7f * glowSize,
                        style = Stroke(width = 1f)
                    )
                }
        )

        // Main modal container
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CalendarColors.glassCore,
                            CalendarColors.deepSpace.copy(alpha = 0.95f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            CalendarColors.secondary.copy(alpha = 0.5f * pulseAlpha),
                            CalendarColors.primary.copy(alpha = 0.4f),
                            CalendarColors.accent.copy(alpha = 0.3f * pulseAlpha),
                            CalendarColors.secondary.copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .drawBehind {
                    // Inner glow effects
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                CalendarColors.secondary.copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        ),
                        radius = size.maxDimension * 0.45f,
                        center = Offset(size.width * 0.2f, size.height * 0.15f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                CalendarColors.primary.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        ),
                        radius = size.maxDimension * 0.35f,
                        center = Offset(size.width * 0.85f, size.height * 0.6f)
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                CalendarHeader(
                    currentMonth = currentMonth,
                    viewMode = viewMode,
                    onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                    onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                    onViewModeChange = { viewMode = it },
                    onAddEvent = { showAddEvent = true },
                    onClose = onClose,
                    pulseAlpha = pulseAlpha
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Today's date highlight
                TodayCard(selectedDate = selectedDate, pulseAlpha = pulseAlpha)

                Spacer(modifier = Modifier.height(12.dp))

                // Calendar view based on mode
                when (viewMode) {
                    CalendarViewMode.MONTH -> {
                        MonthView(
                            currentMonth = currentMonth,
                            selectedDate = selectedDate,
                            events = events,
                            onDateSelected = { selectedDate = it },
                            onMonthSwipe = { direction ->
                                currentMonth = if (direction > 0)
                                    currentMonth.minusMonths(1)
                                else
                                    currentMonth.plusMonths(1)
                            },
                            pulseAlpha = pulseAlpha
                        )
                    }
                    CalendarViewMode.WEEK -> {
                        WeekView(
                            selectedDate = selectedDate,
                            events = events,
                            onDateSelected = { selectedDate = it }
                        )
                    }
                    CalendarViewMode.AGENDA -> {
                        AgendaView(
                            events = events.filter { it.date >= LocalDate.now() }
                                .sortedBy { it.date },
                            onEventClick = { showEventDetails = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Events for selected date
                if (viewMode != CalendarViewMode.AGENDA) {
                    SelectedDateEvents(
                        date = selectedDate,
                        events = events.filter { it.date == selectedDate },
                        onEventClick = { showEventDetails = it },
                        onAddEvent = { showAddEvent = true }
                    )
                }
            }

            // Add Event Dialog
            if (showAddEvent) {
                AddEventDialog(
                    selectedDate = selectedDate,
                    onDismiss = { showAddEvent = false },
                    onAddEvent = { event ->
                        events = events + event
                        showAddEvent = false
                    }
                )
            }

            // Event Details Dialog
            showEventDetails?.let { event ->
                EventDetailsDialog(
                    event = event,
                    onDismiss = { showEventDetails = null },
                    onDelete = {
                        events = events.filter { it.id != event.id }
                        showEventDetails = null
                    }
                )
            }
        }
    }
}

enum class CalendarViewMode {
    MONTH, WEEK, AGENDA
}

@Composable
private fun CalendarHeader(
    currentMonth: YearMonth,
    viewMode: CalendarViewMode,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onViewModeChange: (CalendarViewMode) -> Unit,
    onAddEvent: () -> Unit,
    onClose: () -> Unit,
    pulseAlpha: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close button with glow
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            CalendarColors.accent.copy(alpha = 0.2f),
                            CalendarColors.surface.copy(alpha = 0.8f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = CalendarColors.accent.copy(alpha = 0.4f * pulseAlpha),
                    shape = CircleShape
                )
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = CalendarColors.accent,
                modifier = Modifier.size(18.dp)
            )
        }

        // Month/Year with navigation
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CalendarColors.surface.copy(alpha = 0.5f))
                    .clickable { onPreviousMonth() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous",
                    tint = CalendarColors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        .uppercase(),
                    color = CalendarColors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "◈ ${currentMonth.year}",
                    color = CalendarColors.primary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CalendarColors.surface.copy(alpha = 0.5f))
                    .clickable { onNextMonth() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next",
                    tint = CalendarColors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // View mode toggle
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(CalendarColors.surface.copy(alpha = 0.6f))
                    .border(1.dp, CalendarColors.secondary.copy(alpha = 0.3f), CircleShape)
                    .clickable {
                        onViewModeChange(
                            when (viewMode) {
                                CalendarViewMode.MONTH -> CalendarViewMode.WEEK
                                CalendarViewMode.WEEK -> CalendarViewMode.AGENDA
                                CalendarViewMode.AGENDA -> CalendarViewMode.MONTH
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (viewMode) {
                        CalendarViewMode.MONTH -> Icons.Default.CalendarViewMonth
                        CalendarViewMode.WEEK -> Icons.Default.ViewWeek
                        CalendarViewMode.AGENDA -> Icons.Default.ViewAgenda
                    },
                    contentDescription = "View Mode",
                    tint = CalendarColors.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Add event button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(CalendarColors.primary.copy(alpha = 0.2f))
                    .border(
                        1.dp,
                        CalendarColors.primary.copy(alpha = 0.5f * pulseAlpha),
                        CircleShape
                    )
                    .clickable { onAddEvent() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Event",
                    tint = CalendarColors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TodayCard(selectedDate: LocalDate, pulseAlpha: Float) {
    val today = LocalDate.now()
    val isToday = selectedDate == today

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        CalendarColors.primary.copy(alpha = 0.08f),
                        CalendarColors.secondary.copy(alpha = 0.08f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        CalendarColors.primary.copy(alpha = 0.3f * pulseAlpha),
                        CalendarColors.secondary.copy(alpha = 0.3f * pulseAlpha)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isToday) "⚡ TODAY" else "◇ ${selectedDate.dayOfWeek.getDisplayName(
                        TextStyle.FULL, Locale.getDefault()
                    ).uppercase()}",
                    color = CalendarColors.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                    color = CalendarColors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light
                )
            }

            // Animated day number
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (isToday) CalendarColors.primary.copy(alpha = 0.15f)
                        else CalendarColors.surface.copy(alpha = 0.5f)
                    )
                    .border(
                        width = 2.dp,
                        brush = if (isToday) Brush.sweepGradient(
                            colors = listOf(
                                CalendarColors.primary,
                                CalendarColors.secondary.copy(alpha = 0.5f),
                                CalendarColors.primary
                            )
                        ) else Brush.linearGradient(
                            colors = listOf(
                                CalendarColors.textMuted.copy(alpha = 0.5f),
                                CalendarColors.textMuted.copy(alpha = 0.3f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedDate.dayOfMonth.toString(),
                    color = if (isToday) CalendarColors.primary else CalendarColors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun MonthView(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    events: List<CalendarEvent>,
    onDateSelected: (LocalDate) -> Unit,
    onMonthSwipe: (Float) -> Unit,
    pulseAlpha: Float
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1)
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 0 = Sunday

    var swipeOffset by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CalendarColors.surface.copy(alpha = 0.4f))
            .border(
                width = 1.dp,
                color = CalendarColors.gridLine,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(10.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (kotlin.math.abs(swipeOffset) > 100) {
                            onMonthSwipe(swipeOffset)
                        }
                        swipeOffset = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        swipeOffset += dragAmount
                    }
                )
            }
    ) {
        // Day headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEachIndexed { index, day ->
                Text(
                    text = day,
                    color = if (index == 0 || index == 6)
                        CalendarColors.accent.copy(alpha = 0.7f)
                    else
                        CalendarColors.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Calendar grid
        val totalCells = ((startDayOfWeek + daysInMonth + 6) / 7) * 7

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.heightIn(max = 240.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(totalCells) { index ->
                val dayNumber = index - startDayOfWeek + 1
                val isValidDay = dayNumber in 1..daysInMonth

                if (isValidDay) {
                    val date = currentMonth.atDay(dayNumber)
                    val isToday = date == LocalDate.now()
                    val isSelected = date == selectedDate
                    val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY ||
                                   date.dayOfWeek == DayOfWeek.SUNDAY
                    val dayEvents = events.filter { it.date == date }

                    DayCell(
                        day = dayNumber,
                        isToday = isToday,
                        isSelected = isSelected,
                        isWeekend = isWeekend,
                        hasEvents = dayEvents.isNotEmpty(),
                        eventCount = dayEvents.size,
                        onClick = { onDateSelected(date) },
                        pulseAlpha = pulseAlpha
                    )
                } else {
                    Box(modifier = Modifier.aspectRatio(1f))
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    isWeekend: Boolean,
    hasEvents: Boolean,
    eventCount: Int,
    onClick: () -> Unit,
    pulseAlpha: Float
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isSelected -> CalendarColors.selected.copy(alpha = 0.25f)
                    isToday -> CalendarColors.today.copy(alpha = 0.12f)
                    isWeekend -> CalendarColors.weekend.copy(alpha = 0.08f)
                    else -> Color.Transparent
                }
            )
            .then(
                if (isToday || isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = if (isSelected) CalendarColors.selected.copy(alpha = 0.6f * pulseAlpha)
                               else CalendarColors.today.copy(alpha = 0.4f * pulseAlpha),
                        shape = RoundedCornerShape(10.dp)
                    )
                } else Modifier
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                color = when {
                    isSelected -> CalendarColors.selected
                    isToday -> CalendarColors.today
                    isWeekend -> CalendarColors.accent.copy(alpha = 0.7f)
                    else -> CalendarColors.textPrimary
                },
                fontSize = 13.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace
            )

            // Event indicators
            if (hasEvents) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(minOf(eventCount, 3)) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(CalendarColors.eventDot)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekView(
    selectedDate: LocalDate,
    events: List<CalendarEvent>,
    onDateSelected: (LocalDate) -> Unit
) {
    val startOfWeek = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() % 7)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CalendarColors.surface.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(7) { dayOffset ->
                val date = startOfWeek.plusDays(dayOffset.toLong())
                val isToday = date == LocalDate.now()
                val isSelected = date == selectedDate
                val dayEvents = events.filter { it.date == date }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) CalendarColors.selected.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .clickable { onDateSelected(date) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            .uppercase(),
                        color = CalendarColors.textMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isToday) CalendarColors.today.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .then(
                                if (isToday) Modifier.border(1.dp, CalendarColors.today, CircleShape)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            color = if (isToday) CalendarColors.today else CalendarColors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    if (dayEvents.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${dayEvents.size}",
                            color = CalendarColors.eventDot,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaView(
    events: List<CalendarEvent>,
    onEventClick: (CalendarEvent) -> Unit
) {
    if (events.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.EventBusy,
                    contentDescription = null,
                    tint = CalendarColors.textMuted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No upcoming events",
                    color = CalendarColors.textMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(events) { event ->
                EventCard(
                    event = event,
                    onClick = { onEventClick(event) }
                )
            }
        }
    }
}

@Composable
private fun SelectedDateEvents(
    date: LocalDate,
    events: List<CalendarEvent>,
    onEventClick: (CalendarEvent) -> Unit,
    onAddEvent: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp, max = 300.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EVENTS",
                color = CalendarColors.textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "${events.size} event${if (events.size != 1) "s" else ""}",
                color = CalendarColors.primary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CalendarColors.surface.copy(alpha = 0.3f))
                    .border(
                        width = 1.dp,
                        color = CalendarColors.gridLine,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onAddEvent() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = CalendarColors.textMuted,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No events for this day",
                        color = CalendarColors.textMuted,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Tap to add one",
                        color = CalendarColors.primary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events) { event ->
                    EventCard(
                        event = event,
                        onClick = { onEventClick(event) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EventCard(
    event: CalendarEvent,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CalendarColors.cardSurface)
            .border(
                width = 1.dp,
                color = event.color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(event.color)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                color = CalendarColors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (event.time != null || event.isAllDay) {
                Text(
                    text = if (event.isAllDay) "All day"
                           else event.time?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "",
                    color = CalendarColors.textMuted,
                    fontSize = 12.sp
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = CalendarColors.textMuted
        )
    }
}

@Composable
private fun AddEventDialog(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onAddEvent: (CalendarEvent) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isAllDay by remember { mutableStateOf(true) }
    var selectedHour by remember { mutableStateOf(9) }
    var selectedMinute by remember { mutableStateOf(0) }
    var selectedColor by remember { mutableStateOf(CalendarColors.primary) }

    val colors = listOf(
        CalendarColors.primary,
        CalendarColors.secondary,
        CalendarColors.accent,
        CalendarColors.warning,
        CalendarColors.success
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(CalendarColors.surface)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            CalendarColors.borderGlow,
                            CalendarColors.purpleGlow
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable(enabled = false) { }
                .padding(24.dp)
        ) {
            Text(
                text = "NEW EVENT",
                color = CalendarColors.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                color = CalendarColors.textSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Event Title") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CalendarColors.textPrimary,
                    unfocusedTextColor = CalendarColors.textPrimary,
                    focusedBorderColor = CalendarColors.primary,
                    unfocusedBorderColor = CalendarColors.textMuted,
                    focusedLabelColor = CalendarColors.primary,
                    unfocusedLabelColor = CalendarColors.textMuted,
                    cursorColor = CalendarColors.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // All day toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All Day",
                    color = CalendarColors.textSecondary
                )
                Switch(
                    checked = isAllDay,
                    onCheckedChange = { isAllDay = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CalendarColors.primary,
                        checkedTrackColor = CalendarColors.primary.copy(alpha = 0.3f)
                    )
                )
            }

            // Time picker (if not all day)
            AnimatedVisibility(visible = !isAllDay) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Hour picker
                        OutlinedTextField(
                            value = selectedHour.toString().padStart(2, '0'),
                            onValueChange = {
                                it.toIntOrNull()?.let { h ->
                                    if (h in 0..23) selectedHour = h
                                }
                            },
                            label = { Text("Hour") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CalendarColors.textPrimary,
                                unfocusedTextColor = CalendarColors.textPrimary,
                                focusedBorderColor = CalendarColors.primary,
                                unfocusedBorderColor = CalendarColors.textMuted
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // Minute picker
                        OutlinedTextField(
                            value = selectedMinute.toString().padStart(2, '0'),
                            onValueChange = {
                                it.toIntOrNull()?.let { m ->
                                    if (m in 0..59) selectedMinute = m
                                }
                            },
                            label = { Text("Min") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CalendarColors.textPrimary,
                                unfocusedTextColor = CalendarColors.textPrimary,
                                focusedBorderColor = CalendarColors.primary,
                                unfocusedBorderColor = CalendarColors.textMuted
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color selection
            Text(
                text = "Color",
                color = CalendarColors.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (selectedColor == color) {
                                    Modifier.border(2.dp, CalendarColors.textPrimary, CircleShape)
                                } else Modifier
                            )
                            .clickable { selectedColor = color }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CalendarColors.textSecondary
                    )
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onAddEvent(
                                CalendarEvent(
                                    title = title,
                                    date = selectedDate,
                                    time = if (isAllDay) null else LocalTime.of(selectedHour, selectedMinute),
                                    color = selectedColor,
                                    description = description,
                                    isAllDay = isAllDay
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CalendarColors.primary,
                        contentColor = CalendarColors.background
                    )
                ) {
                    Text("Add Event")
                }
            }
        }
    }
}

@Composable
private fun EventDetailsDialog(
    event: CalendarEvent,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
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
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(CalendarColors.surface)
                .border(
                    width = 1.dp,
                    color = event.color.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable(enabled = false) { }
                .padding(24.dp)
        ) {
            // Color bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(event.color)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = event.title,
                color = CalendarColors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = CalendarColors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                    color = CalendarColors.textSecondary,
                    fontSize = 14.sp
                )
            }

            if (event.time != null || event.isAllDay) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = CalendarColors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (event.isAllDay) "All day"
                               else event.time?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "",
                        color = CalendarColors.textSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            if (event.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = event.description,
                    color = CalendarColors.textSecondary,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CalendarColors.accent
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(
                            listOf(CalendarColors.accent, CalendarColors.accent)
                        )
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CalendarColors.primary,
                        contentColor = CalendarColors.background
                    )
                ) {
                    Text("Close")
                }
            }
        }
    }
}

// Sample events for demo
private fun getSampleEvents(): List<CalendarEvent> {
    val today = LocalDate.now()
    return listOf(
        CalendarEvent(
            title = "Team Meeting",
            date = today,
            time = LocalTime.of(10, 0),
            color = CalendarColors.primary
        ),
        CalendarEvent(
            title = "Lunch with Alex",
            date = today,
            time = LocalTime.of(12, 30),
            color = CalendarColors.secondary
        ),
        CalendarEvent(
            title = "Project Deadline",
            date = today.plusDays(2),
            isAllDay = true,
            color = CalendarColors.accent
        ),
        CalendarEvent(
            title = "Doctor Appointment",
            date = today.plusDays(5),
            time = LocalTime.of(14, 0),
            color = CalendarColors.warning
        ),
        CalendarEvent(
            title = "Birthday Party",
            date = today.plusDays(7),
            time = LocalTime.of(18, 0),
            color = CalendarColors.success
        )
    )
}
