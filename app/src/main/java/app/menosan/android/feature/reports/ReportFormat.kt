package app.menosan.android.feature.reports

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.menosan.android.R
import app.menosan.android.core.analytics.QuantityUnit
import app.menosan.android.core.model.WasteCategory
import app.menosan.android.core.network.ApiError
import app.menosan.android.core.ui.components.quantityText
import app.menosan.android.core.ui.theme.MenosanTheme
import app.menosan.android.data.remote.dto.CostLevel
import app.menosan.android.data.remote.dto.Effort
import app.menosan.android.data.remote.dto.HotspotCriterion
import app.menosan.android.data.remote.dto.InterventionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val MONTH_DAY = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
private val DAY = DateTimeFormatter.ofPattern("d", Locale.ENGLISH)

fun formatWeekRange(start: LocalDate, end: LocalDate): String =
    if (start.month == end.month) "${MONTH_DAY.format(start)} – ${DAY.format(end)}" else "${MONTH_DAY.format(start)} – ${MONTH_DAY.format(end)}"

@Composable
@ReadOnlyComposable
fun weekRangeWithYear(start: LocalDate, end: LocalDate): String =
    stringResource(R.string.reports_week_range_year, formatWeekRange(start, end), end.year)

@Composable
@ReadOnlyComposable
fun entriesText(count: Int): String = pluralStringResource(R.plurals.reports_entries, count, count)

@Composable
@ReadOnlyComposable
fun piecesText(count: Int): String = pluralStringResource(R.plurals.reports_pieces, count, count)

@Composable
@ReadOnlyComposable
fun entriesAndPieces(entries: Int, pieces: Int): String =
    stringResource(R.string.reports_entries_and_pieces, entriesText(entries), piecesText(pieces))

@Composable
@ReadOnlyComposable
fun entriesAndQuantity(entries: Int, quantity: Int, unit: QuantityUnit): String =
    stringResource(R.string.reports_entries_and_pieces, entriesText(entries), quantityText(quantity, unit))

/** An unsigned change like "14.7%" or "0%"; the trend arrow next to it shows the direction. */
fun percentText(value: Double): String =
    String.format(Locale.ENGLISH, "%.1f", abs(value)).removeSuffix(".0") + "%"

fun absInt(value: Int): Int = abs(value)

@Composable
@ReadOnlyComposable
fun categoryColor(category: WasteCategory): Color = when (category) {
    WasteCategory.BIODEGRADABLE -> MenosanTheme.colors.biodegradable
    WasteCategory.RECYCLABLE -> MenosanTheme.colors.recyclable
    WasteCategory.RESIDUAL -> MenosanTheme.colors.residual
    WasteCategory.SPECIAL -> MenosanTheme.colors.special
}

/** A picture for each subcategory code (taxonomy codes are stable, plan §3). */
fun subcategoryIcon(code: String): ImageVector = when (code) {
    "BIO_FOOD_LEFTOVERS" -> WasteIcons.Leftovers
    "BIO_SPOILED_FOOD" -> WasteIcons.SpoiledFood
    "BIO_PEELS_SCRAPS" -> WasteIcons.PeelsScraps
    "BIO_YARD_WASTE" -> Icons.Outlined.Grass
    "BIO_OTHER" -> Icons.Outlined.Eco
    "REC_PET_BOTTLES" -> WasteIcons.PetBottle
    "REC_RIGID_PLASTICS" -> WasteIcons.PlasticTub
    "REC_PAPER_CARDBOARD" -> Icons.Outlined.Inventory2
    "REC_GLASS" -> WasteIcons.GlassJar
    "REC_METAL_CANS" -> WasteIcons.MetalCan
    "REC_OTHER" -> Icons.Outlined.Recycling
    "RES_SACHETS" -> WasteIcons.Sachet
    "RES_PLASTIC_BAGS" -> WasteIcons.PlasticBag
    "RES_SNACK_WRAPPERS" -> WasteIcons.SnackWrapper
    "RES_STYROFOAM" -> WasteIcons.Clamshell
    "RES_DISPOSABLES" -> WasteIcons.CupStraw
    "RES_TISSUE" -> WasteIcons.TissueBox
    "RES_DIAPERS_SANITARY" -> WasteIcons.Diaper
    else -> Icons.Outlined.DeleteOutline
}

@Composable
@ReadOnlyComposable
fun criterionLabel(criterion: HotspotCriterion): String? = when (criterion) {
    HotspotCriterion.MOST_FREQUENT -> stringResource(R.string.report_criterion_most_frequent)
    HotspotCriterion.HIGHEST_QUANTITY -> stringResource(R.string.report_criterion_highest_quantity)
    HotspotCriterion.AVOIDABLE -> stringResource(R.string.report_criterion_avoidable)
    HotspotCriterion.UNKNOWN -> null
}

@Composable
@ReadOnlyComposable
fun typeLabel(type: InterventionType): String? = when (type) {
    InterventionType.PREVENT -> stringResource(R.string.report_type_prevent)
    InterventionType.REDUCE -> stringResource(R.string.report_type_reduce)
    InterventionType.REUSE -> stringResource(R.string.report_type_reuse)
    InterventionType.UNKNOWN -> null
}

@Composable
@ReadOnlyComposable
fun costLabel(cost: CostLevel): String? = when (cost) {
    CostLevel.FREE -> stringResource(R.string.report_cost_free)
    CostLevel.SAVES_MONEY -> stringResource(R.string.report_cost_saves_money)
    CostLevel.SMALL_ONE_TIME_COST -> stringResource(R.string.report_cost_small_one_time)
    CostLevel.UNKNOWN -> null
}

@Composable
@ReadOnlyComposable
fun effortLabel(effort: Effort): String? = when (effort) {
    Effort.LOW -> stringResource(R.string.report_effort_low)
    Effort.MEDIUM -> stringResource(R.string.report_effort_medium)
    Effort.UNKNOWN -> null
}

enum class ReportProblem { Offline, Server }

fun ApiError.toProblem(): ReportProblem = if (this is ApiError.Network) ReportProblem.Offline else ReportProblem.Server

@Composable
@ReadOnlyComposable
fun problemText(problem: ReportProblem): String = when (problem) {
    ReportProblem.Offline -> stringResource(R.string.reports_error_offline)
    ReportProblem.Server -> stringResource(R.string.reports_error_server)
}
