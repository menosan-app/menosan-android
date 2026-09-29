package app.menosan.android.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Opacity of an action that is shown but locked, e.g. photo logging while offline. */
const val DISABLED_ALPHA = 0.45f

/**
 * Colors for every filled main button. The plain `Button` default is `primary`, which is a lighter
 * green than the rest of the app's buttons in dark mode.
 */
@Composable
fun primaryButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

/** Border for outlined secondary buttons, matching the back button and the photo screens. */
@Composable
fun secondaryButtonBorder(): BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
