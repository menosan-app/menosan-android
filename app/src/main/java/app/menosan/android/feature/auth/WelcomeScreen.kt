package app.menosan.android.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.menosan.android.R
import app.menosan.android.core.ui.theme.MenosanTheme

@Composable
fun WelcomeScreen(
    onCreateAccount: () -> Unit,
    onLogIn: () -> Unit,
) = BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    val artMaxHeight = maxHeight * ART_HEIGHT_FRACTION
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Image(
            painter = painterResource(R.drawable.illustration_welcome),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .heightIn(max = artMaxHeight)
                .fillMaxWidth()
                .aspectRatio(WELCOME_ART_RATIO),
        )
        Text(
            text = stringResource(R.string.brand_wordmark),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.heightIn(min = 8.dp))
        Text(
            text = stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.heightIn(min = 24.dp))
        Button(
            onClick = onCreateAccount,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Text(stringResource(R.string.welcome_create_account), style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(
            onClick = onLogIn,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        ) {
            Text(stringResource(R.string.welcome_log_in), style = MaterialTheme.typography.titleMedium)
        }
    }
}

private const val WELCOME_ART_RATIO = 868f / 869f

internal const val ART_HEIGHT_FRACTION = 0.45f

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    MenosanTheme { WelcomeScreen(onCreateAccount = {}, onLogIn = {}) }
}
