package app.menosan.android.feature.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.menosan.android.R
import app.menosan.android.core.analytics.QuantityUnit
import app.menosan.android.core.model.WasteCategory
import app.menosan.android.core.ui.components.Donut
import app.menosan.android.core.ui.components.SectionHeader
import app.menosan.android.core.ui.components.gramsText
import app.menosan.android.core.ui.components.quantityText
import app.menosan.android.data.remote.dto.CategoryComparisonDto
import app.menosan.android.data.remote.dto.ComparisonDto
import app.menosan.android.data.remote.dto.ComparisonRowDto
import app.menosan.android.data.remote.dto.SubcategoryComparisonDto
import app.menosan.android.data.remote.dto.Trend
import app.menosan.android.data.remote.dto.WeeklyStatsDto
import kotlin.math.roundToInt

private const val COLLAPSED_ROWS = 2

@Composable
fun TabHeading(title: String, subtitle: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader(title)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ProgressHeading(comparison: ComparisonDto?) {
    TabHeading(
        title = stringResource(R.string.report_progress_title),
        subtitle = comparison?.let {
            stringResource(R.string.report_progress_compared, formatWeekRange(it.previousWeekStart, it.previousWeekStart.plusDays(6)))
        },
    )
}

@Composable
fun ProgressTotals(comparison: ComparisonDto) {
    val hasPieces = comparison.pieces.previous > 0 || comparison.pieces.current > 0
    val hasGrams = comparison.grams.previous > 0 || comparison.grams.current > 0
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (hasPieces || !hasGrams) {
            TotalTile(
                Icons.Outlined.DeleteOutline,
                stringResource(R.string.report_progress_total_pieces),
                comparison.pieces,
                QuantityUnit.PIECES,
                Modifier.weight(1f).fillMaxHeight(),
            )
        }
        if (hasGrams) {
            TotalTile(
                Icons.Outlined.Scale,
                stringResource(R.string.report_totals_food),
                comparison.grams,
                QuantityUnit.GRAMS,
                Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun TotalTile(icon: ImageVector, label: String, row: ComparisonRowDto, unit: QuantityUnit, modifier: Modifier) {
    ReportCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val valueStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.quantity_change, totalText(row.previous, unit), totalText(row.current, unit)),
                style = valueStyle,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = valueStyle.fontSize),
            )
            TrendChange(row.trend, row.deltaPct, row.current)
            Text(changeText(row.delta, unit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun totalText(value: Int, unit: QuantityUnit): String = when (unit) {
    QuantityUnit.PIECES -> value.toString()
    QuantityUnit.GRAMS -> gramsText(value)
}

@Composable
private fun changeText(delta: Int, unit: QuantityUnit): String {
    val amount = absInt(delta)
    return when {
        delta == 0 -> stringResource(R.string.report_progress_no_change)
        unit == QuantityUnit.PIECES && delta < 0 -> pluralStringResource(R.plurals.report_progress_fewer_pieces, amount, amount)
        unit == QuantityUnit.PIECES -> pluralStringResource(R.plurals.report_progress_more_pieces, amount, amount)
        delta < 0 -> stringResource(R.string.report_progress_grams_less, gramsText(amount))
        else -> stringResource(R.string.report_progress_grams_more, gramsText(amount))
    }
}

@Composable
fun TrendChange(trend: Trend, deltaPct: Double?, current: Int, modifier: Modifier = Modifier) {
    val (icon, color) = trendArrow(trend)
    val text = when {
        deltaPct != null -> percentText(deltaPct)
        current > 0 -> stringResource(R.string.report_progress_new)
        else -> percentText(0.0)
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icon, contentDescription = trendDescription(trend), tint = color, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = color, maxLines = 1)
    }
}

@Composable
private fun trendArrow(trend: Trend): Pair<ImageVector, Color> = when (trend) {
    Trend.DECREASED -> Icons.Filled.ArrowDownward to MaterialTheme.colorScheme.primary
    Trend.INCREASED -> Icons.Filled.ArrowUpward to MaterialTheme.colorScheme.tertiary
    Trend.SAME, Trend.UNKNOWN -> Icons.AutoMirrored.Filled.ArrowForward to MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun trendDescription(trend: Trend): String? = when (trend) {
    Trend.DECREASED -> stringResource(R.string.impact_decreased)
    Trend.INCREASED -> stringResource(R.string.impact_increased)
    Trend.SAME -> stringResource(R.string.impact_same)
    Trend.UNKNOWN -> null
}

data class CategoryProgress(
    val category: WasteCategory,
    val sharePct: Double,
    val totals: List<CategoryComparisonDto>,
    val subcategories: List<SubcategoryComparisonDto>,
)

fun categoryProgress(comparison: ComparisonDto, stats: WeeklyStatsDto): List<CategoryProgress> =
    WasteCategory.entries.mapNotNull { category ->
        val totals = comparison.categories.filter { it.category == category && (it.previous > 0 || it.current > 0) }
        if (totals.isEmpty()) return@mapNotNull null
        CategoryProgress(
            category = category,
            sharePct = stats.categories.firstOrNull { it.category == category }?.sharePct ?: 0.0,
            totals = totals,
            subcategories = comparison.subcategories
                .filter { it.category == category && (it.previous > 0 || it.current > 0) }
                .sortedWith(compareByDescending<SubcategoryComparisonDto> { it.current }.thenByDescending { it.previous }.thenBy { it.code }),
        )
    }

@Composable
fun CategoryProgressCard(progress: CategoryProgress, state: ReportUiState) {
    var expanded by rememberSaveable(progress.category) { mutableStateOf(false) }
    val color = categoryColor(progress.category)
    val canExpand = progress.subcategories.size > COLLAPSED_ROWS
    ReportCard {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (canExpand) Modifier.clickable(role = Role.Button, onClick = { expanded = !expanded }) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ShareDonut(progress.sharePct, color)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(state.label(progress.category.name), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                progress.totals.forEach { total ->
                    Text(quantityText(total.current, total.unit), style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TrendChange(total.trend, total.deltaPct, total.current)
                        if (total.delta != 0) {
                            Text(
                                stringResource(R.string.report_progress_change_in_brackets, changeText(total.delta, total.unit)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (canExpand) {
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val shown = if (expanded) progress.subcategories else progress.subcategories.take(COLLAPSED_ROWS)
        if (shown.isNotEmpty()) {
            Column {
                shown.forEach { sub ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SubcategoryProgressRow(sub, color, state)
                }
            }
        }
        if (canExpand) ShowMoreButton(expanded, onClick = { expanded = !expanded })
    }
}

@Composable
private fun ShareDonut(sharePct: Double, color: Color) {
    val share = (sharePct / 100.0).toFloat().coerceIn(0f, 1f)
    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Donut(
            slices = listOf(share to color, (1f - share) to color.copy(alpha = 0.25f)),
            thickness = 9.dp,
        )
        Text(
            stringResource(R.string.report_category_share, sharePct.roundToInt().toString()),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun SubcategoryProgressRow(sub: SubcategoryComparisonDto, color: Color, state: ReportUiState) {
    Row(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(subcategoryIcon(sub.code), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(state.label(sub.code), style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Text(
                stringResource(R.string.quantity_change, quantityText(sub.previous, sub.unit), quantityText(sub.current, sub.unit)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MiniBars(sub.previous, sub.current, color)
        TrendChange(sub.trend, sub.deltaPct, sub.current, Modifier.widthIn(min = 64.dp))
    }
}

@Composable
private fun MiniBars(previous: Int, current: Int, color: Color) {
    val faded = color.copy(alpha = 0.35f)
    Canvas(Modifier.size(width = 26.dp, height = 22.dp)) {
        val top = maxOf(previous, current)
        if (top <= 0) return@Canvas
        val gap = 4.dp.toPx()
        val barWidth = (size.width - gap) / 2f
        val radius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        listOf(previous to faded, current to color).forEachIndexed { index, (value, barColor) ->
            val height = if (value > 0) maxOf(size.height * value / top, 2.dp.toPx()) else 0f
            if (height > 0f) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(index * (barWidth + gap), size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = radius,
                )
            }
        }
    }
}

@Composable
private fun ShowMoreButton(expanded: Boolean, onClick: () -> Unit) {
    ExpandToggle(
        text = stringResource(if (expanded) R.string.report_progress_show_less else R.string.report_progress_show_more),
        expanded = expanded,
        onClick = onClick,
    )
}
