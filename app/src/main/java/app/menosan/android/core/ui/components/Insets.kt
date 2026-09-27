package app.menosan.android.core.ui.components

import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun Modifier.screenInsetsPadding(): Modifier = this
    .statusBarsPadding()
    .displayCutoutPadding()
    .padding(top = SCREEN_TOP_GAP)

private val SCREEN_TOP_GAP = 8.dp
