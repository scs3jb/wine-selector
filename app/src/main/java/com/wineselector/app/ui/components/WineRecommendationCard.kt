package com.wineselector.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wineselector.app.data.HighlightTier
import com.wineselector.app.data.WineAlternative
import com.wineselector.app.data.WineRecommendation
import com.wineselector.app.data.XWineEntry

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WineRecommendationCard(
    recommendation: WineRecommendation,
    modifier: Modifier = Modifier
) {
    var expandedAlternativeIndex by remember { mutableIntStateOf(-1) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TierDot(tier = HighlightTier.GOLD, size = 14.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Top Pick",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = recommendation.wineName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            if (recommendation.price != null && recommendation.price != "Not visible") {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = recommendation.price,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = recommendation.reasoning,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            // Vintage note
            if (recommendation.vintageNote != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = recommendation.vintageNote,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            // X-Wines enriched data
            val xWine = recommendation.xWinesMatch
            if (xWine != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                XWinesDetails(xWine)
            }

            // Clickable alternatives
            if (recommendation.alternatives.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Other options:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                recommendation.alternatives.forEachIndexed { index, alt ->
                    val altTier = when (index) {
                        0 -> HighlightTier.SILVER
                        1 -> HighlightTier.BRONZE
                        else -> HighlightTier.RED
                    }
                    AlternativeWineRow(
                        alternative = alt,
                        highlightTier = altTier,
                        isExpanded = expandedAlternativeIndex == index,
                        onToggleExpand = {
                            expandedAlternativeIndex = if (expandedAlternativeIndex == index) -1 else index
                        }
                    )
                    if (index < recommendation.alternatives.lastIndex) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun XWinesDetails(
    xWine: XWineEntry,
    compact: Boolean = false
) {
    if (!compact) {
        Text(
            text = "Wine Database",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = xWine.wineName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Spacer(modifier = Modifier.height(8.dp))
    } else {
        Text(
            text = xWine.wineName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Spacer(modifier = Modifier.height(4.dp))
    }

    // User rating
    if (xWine.averageRating != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(if (compact) 14.dp else 18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${xWine.averageRating}/5 user rating",
                style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }

    // Wine details: type, body, acidity
    Text(
        text = "${xWine.type} \u00B7 ${xWine.body} \u00B7 ${xWine.acidity} acidity" +
            if (xWine.abv != null) " \u00B7 ${xWine.abv}% ABV" else "",
        style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
    )

    // Grapes
    if (xWine.grapes.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Grapes: ${xWine.grapes.joinToString(", ")}",
            style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
        )
    }

    // Region & country
    Text(
        text = "${xWine.regionName}, ${xWine.country}",
        style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
    )

    // Food harmonizations as chips
    if (xWine.harmonize.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Pairs well with:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (food in xWine.harmonize) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = food,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlternativeWineRow(
    alternative: WineAlternative,
    highlightTier: HighlightTier,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TierDot(tier = highlightTier, size = 10.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alternative.wineName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    if (alternative.price != null) {
                        Text(
                            text = alternative.price,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                Text(
                    text = "${alternative.score}/10",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                                  else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = alternative.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )

                if (alternative.vintageNote != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = alternative.vintageNote,
                        style = MaterialTheme.typography.labelSmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                val xWine = alternative.xWinesMatch
                if (xWine != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    XWinesDetails(xWine, compact = true)
                }
            }
        }
    }
}

/** Maps highlight tier to its full-opacity border color (matches the photo overlay). */
private fun tierIndicatorColor(tier: HighlightTier): Color = when (tier) {
    HighlightTier.GOLD -> Color(0xFFD4A843)
    HighlightTier.SILVER -> Color(0xFFA8A8A8)
    HighlightTier.BRONZE -> Color(0xFFCD7F32)
    HighlightTier.RED -> Color(0xFFA35D67)
}

@Composable
private fun TierDot(tier: HighlightTier, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(tierIndicatorColor(tier))
    )
}
