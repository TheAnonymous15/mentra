package com.example.mentra.health.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.cos
import kotlin.math.sin

/**
 * Health Dashboard Screen
 * Beautiful health tracking with real-time metrics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    viewModel: HealthViewModel = hiltViewModel()
) {
    val healthMetrics by viewModel.healthMetrics.collectAsState()
    val healthSummary by viewModel.healthSummary.collectAsState()
    val activityState by viewModel.activityState.collectAsState()

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header
            HealthHeader()

            Spacer(modifier = Modifier.height(24.dp))

            // Main Health Score Circle
            HealthScoreCircle(score = healthSummary.healthScore)

            Spacer(modifier = Modifier.height(32.dp))

            // Activity Status
            ActivityStatusCard(activityState = activityState)

            Spacer(modifier = Modifier.height(24.dp))

            // Metrics Grid
            HealthMetricsGrid(
                steps = healthMetrics.steps,
                distance = healthMetrics.distance,
                calories = healthMetrics.calories,
                heartRate = healthMetrics.heartRate
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Daily Goals
            DailyGoalsSection(
                steps = healthMetrics.steps,
                calories = healthMetrics.calories
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Activity Level
            ActivityLevelCard(activityLevel = healthSummary.activityLevel)
        }
    }
}

@Composable
fun HealthHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "HEALTH",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF4EC9B0),
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
            Text(
                text = "Your wellness dashboard",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        IconButton(
            onClick = { /* Settings */ },
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = Color(0xFF4EC9B0).copy(alpha = 0.2f),
                    shape = CircleShape
                )
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color(0xFF4EC9B0)
            )
        }
    }
}

@Composable
fun HealthScoreCircle(score: Int) {
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "score"
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            // Animated circular progress
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 20.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)

                // Background circle
                drawCircle(
                    color = Color(0xFF2A2F4A),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )

                // Progress arc
                val sweepAngle = (animatedScore / 100f) * 360f
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF4EC9B0),
                            Color(0xFF569CD6),
                            Color(0xFF4EC9B0)
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round
                    ),
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(center.x - radius, center.y - radius)
                )
            }

            // Score display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$animatedScore",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Health Score",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF4EC9B0)
                )
            }
        }
    }
}

@Composable
fun ActivityStatusCard(activityState: String) {
    val (icon, color, label) = when (activityState) {
        "WALKING" -> Triple(Icons.Default.DirectionsWalk, Color(0xFF4EC9B0), "Walking")
        "RUNNING", "JOGGING" -> Triple(Icons.Default.DirectionsRun, Color(0xFFCE9178), "Running")
        "RESTING" -> Triple(Icons.Default.SelfImprovement, Color(0xFF569CD6), "Resting")
        else -> Triple(Icons.Default.AccessibilityNew, Color(0xFFDCDCAA), "Idle")
    }

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = color.copy(alpha = 0.3f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = color,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column {
                    Text(
                        text = "Current Activity",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleLarge,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Pulse indicator
            Box(
                modifier = Modifier
                    .size(12.dp * pulse)
                    .background(
                        color = color,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun HealthMetricsGrid(
    steps: Int,
    distance: Float,
    calories: Int,
    heartRate: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HealthMetricCard(
                icon = Icons.Default.DirectionsWalk,
                value = steps.toString(),
                label = "Steps",
                color = Color(0xFF4EC9B0),
                modifier = Modifier.weight(1f)
            )
            HealthMetricCard(
                icon = Icons.Default.Terrain,
                value = String.format("%.2f", distance),
                label = "km",
                color = Color(0xFF569CD6),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HealthMetricCard(
                icon = Icons.Default.LocalFireDepartment,
                value = calories.toString(),
                label = "kcal",
                color = Color(0xFFCE9178),
                modifier = Modifier.weight(1f)
            )
            HealthMetricCard(
                icon = Icons.Default.Favorite,
                value = if (heartRate > 0) heartRate.toString() else "--",
                label = "bpm",
                color = Color(0xFFC586C0),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun HealthMetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(120.dp),
        color = Color(0xFF1A1F3A).copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun DailyGoalsSection(steps: Int, calories: Int) {
    val stepGoal = 10000
    val calorieGoal = 400

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "DAILY GOALS",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF4EC9B0),
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        GoalProgressBar(
            current = steps,
            goal = stepGoal,
            label = "Steps",
            color = Color(0xFF4EC9B0)
        )

        GoalProgressBar(
            current = calories,
            goal = calorieGoal,
            label = "Calories",
            color = Color(0xFFCE9178)
        )
    }
}

@Composable
fun GoalProgressBar(
    current: Int,
    goal: Int,
    label: String,
    color: Color
) {
    val progress = (current.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = "progress"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Text(
                text = "$current / $goal",
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2A2F4A))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(color, color.copy(alpha = 0.7f))
                        ),
                        shape = RoundedCornerShape(6.dp)
                    )
            )
        }
    }
}

@Composable
fun ActivityLevelCard(activityLevel: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1A1F3A).copy(alpha = 0.6f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Activity Level",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = activityLevel,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF4EC9B0),
                    fontWeight = FontWeight.Bold
                )
            }

            Icon(
                Icons.Default.TrendingUp,
                contentDescription = "Trend",
                tint = Color(0xFF4EC9B0),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

