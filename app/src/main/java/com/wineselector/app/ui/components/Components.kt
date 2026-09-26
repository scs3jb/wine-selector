package com.wineselector.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wineselector.app.ui.theme.Gold
import com.wineselector.app.ui.theme.scoreColor
import com.wineselector.app.ui.theme.styleColor
import com.wineselector.core.model.FoodCategory
import com.wineselector.core.model.WineStyle
import com.wineselector.core.vintage.DrinkingStatus
import com.wineselector.core.vintage.DrinkingWindow
import java.time.Year

/** Small glass-coloured dot for a wine style. */
@Composable
fun StyleSwatch(style: WineStyle?, size: Dp = 12.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(styleColor(style))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
    )
}

/** Circular pairing score out of 10. */
@Composable
fun ScoreRing(score: Int, size: Dp = 52.dp, label: String? = "/10") {
    val color = scoreColor(score)
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            val stroke = size.toPx() * 0.1f
            drawArc(track, -90f, 360f, false, style = Stroke(stroke))
            drawArc(color, -90f, 360f * score / 10f, false, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(score.toString(), fontWeight = FontWeight.Bold, fontSize = (size.value * 0.36f).sp, color = color, lineHeight = (size.value * 0.4f).sp)
            if (label != null && size >= 48.dp) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 10.sp)
            }
        }
    }
}

@Composable
fun RatingStars(rating: Float, max: Int = 5, size: Dp = 16.dp, color: Color = Gold) {
    Row {
        for (i in 1..max) {
            val icon = when {
                rating >= i -> Icons.Filled.Star
                rating >= i - 0.5f -> Icons.Filled.StarHalf
                else -> Icons.Filled.StarBorder
            }
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size))
        }
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    icon: ImageVector? = null
) {
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50), modifier = modifier) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    ElevatedCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                trailing?.invoke()
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** Horizontal food picker. [selected] null means "no food / just drinking". */
@Composable
fun FoodChipRow(
    selected: FoodCategory?,
    onSelect: (FoodCategory?) -> Unit,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp)
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = contentPadding) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("🍷  Just drinking") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
        items(FoodCategory.entries) { food ->
            FilterChip(
                selected = selected == food,
                onClick = { onSelect(food) },
                label = { Text("${food.icon}  ${food.displayName}") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

/** Timeline from "ready" to "fade" with the peak shaded and a marker for this year. */
@Composable
fun DrinkingWindowBar(window: DrinkingWindow, modifier: Modifier = Modifier) {
    val now = Year.now().value
    val track = MaterialTheme.colorScheme.surfaceVariant
    val peak = MaterialTheme.colorScheme.tertiary
    val marker = MaterialTheme.colorScheme.primary
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(22.dp)) {
            val span = (window.toYear - window.fromYear).coerceAtLeast(1).toFloat()
            fun x(year: Int) = ((year - window.fromYear) / span).coerceIn(0f, 1f) * size.width
            val barTop = size.height * 0.3f
            val barH = size.height * 0.4f
            drawRoundRect(track, Offset(0f, barTop), Size(size.width, barH), CornerRadius(barH / 2))
            drawRoundRect(peak, Offset(x(window.peakFrom), barTop), Size(x(window.peakTo) - x(window.peakFrom), barH), CornerRadius(barH / 2))
            val mx = x(now)
            drawCircle(marker, radius = size.height * 0.36f, center = Offset(mx.coerceIn(8f, size.width - 8f), size.height / 2))
        }
        Row(Modifier.fillMaxWidth()) {
            Text(window.fromYear.toString(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text("peak ${window.peakFrom}–${window.peakTo}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(window.toYear.toString(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
    }
}

fun drinkingStatusColor(status: DrinkingStatus): Color = when (status) {
    DrinkingStatus.PEAK, DrinkingStatus.DRINK_NOW -> Color(0xFF2E7D4F)
    DrinkingStatus.APPROACHING -> Color(0xFF7A9A2E)
    DrinkingStatus.TOO_YOUNG -> Color(0xFF3B6EA5)
    DrinkingStatus.DRINK_UP -> Color(0xFFD08A1E)
    DrinkingStatus.PAST_PEAK -> Color(0xFFB3261E)
}

@Composable
fun ClickableRow(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick)) { content() }
}
