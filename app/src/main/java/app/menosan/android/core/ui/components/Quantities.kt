package app.menosan.android.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.menosan.android.R
import app.menosan.android.core.analytics.QuantityUnit
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

@Composable
@ReadOnlyComposable
fun quantityText(quantity: Int, unit: QuantityUnit): String = when (unit) {
    QuantityUnit.PIECES -> pluralStringResource(R.plurals.count_pieces, quantity, quantity)
    QuantityUnit.GRAMS -> gramsText(quantity)
}

@Composable
@ReadOnlyComposable
fun gramsText(grams: Int): String =
    if (grams >= 1000) {
        stringResource(R.string.quantity_kilograms, formatKilograms(grams))
    } else {
        stringResource(R.string.quantity_grams, String.format(Locale.US, "%,d", grams))
    }

@Composable
@ReadOnlyComposable
fun piecesAndGramsText(pieces: Int, grams: Int): String {
    val parts = buildList {
        if (pieces > 0 || grams == 0) add(quantityText(pieces, QuantityUnit.PIECES))
        if (grams > 0) add(gramsText(grams))
    }
    return parts.joinToString(" · ")
}

fun formatKilograms(grams: Int): String =
    BigDecimal(grams).divide(BigDecimal(1000), 1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
