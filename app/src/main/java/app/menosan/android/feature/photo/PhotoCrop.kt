package app.menosan.android.feature.photo

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.menosan.android.R
import app.menosan.android.core.ui.components.ScreenHeader
import app.menosan.android.core.ui.components.screenInsetsPadding
import kotlin.math.roundToInt

private val CropRectSaver = listSaver<CropRect, Float>(
    save = { listOf(it.left, it.top, it.right, it.bottom) },
    restore = { CropRect(it[0], it[1], it[2], it[3]) },
)

@Composable
fun PhotoCropScreen(
    jpeg: ByteArray,
    onBack: () -> Unit,
    onAnalyze: (CropRect) -> Unit,
    onAnotherPhoto: () -> Unit,
) {
    val image = remember(jpeg) { BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)?.asImageBitmap() }
    var area by rememberSaveable(stateSaver = CropRectSaver) { mutableStateOf(CropRect.FULL) }

    Column(Modifier.fillMaxSize().screenInsetsPadding()) {
        ScreenHeader(title = stringResource(R.string.photo_crop_title), onBack = onBack)
        Text(
            stringResource(R.string.photo_crop_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (image != null) CropArea(image = image, area = area, onAreaChange = { area = it })
        }
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PrimaryButton(stringResource(R.string.photo_crop_analyze), onClick = { onAnalyze(if (image == null) CropRect.FULL else area) })
            if (!area.isFull) SecondaryButton(stringResource(R.string.photo_crop_reset), onClick = { area = CropRect.FULL })
            TextButton(onClick = onAnotherPhoto, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.photo_try_another), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun CropArea(image: ImageBitmap, area: CropRect, onAreaChange: (CropRect) -> Unit) {
    val description = stringResource(R.string.photo_crop_image_description)
    val currentArea by rememberUpdatedState(area)
    val currentOnAreaChange by rememberUpdatedState(onAreaChange)

    Canvas(
        Modifier
            .aspectRatio(image.width.toFloat() / image.height)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                val touchRadius = 32.dp.toPx()
                var handle: CropHandle? = null
                var dragging = currentArea
                detectDragGestures(
                    onDragStart = { start ->
                        dragging = currentArea
                        handle = dragging.handleAt(start.x, start.y, size.width.toFloat(), size.height.toFloat(), touchRadius)
                    },
                    onDragEnd = { handle = null },
                    onDragCancel = { handle = null },
                    onDrag = { change, drag ->
                        val active = handle
                        if (active != null) {
                            change.consume()
                            dragging = dragging.dragged(active, drag.x / size.width, drag.y / size.height)
                            currentOnAreaChange(dragging)
                        }
                    },
                )
            },
    ) {
        drawImage(image, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))

        val left = area.left * size.width
        val top = area.top * size.height
        val right = area.right * size.width
        val bottom = area.bottom * size.height
        val scrim = Color.Black.copy(alpha = 0.55f)
        drawRect(scrim, Offset.Zero, Size(size.width, top))
        drawRect(scrim, Offset(0f, bottom), Size(size.width, size.height - bottom))
        drawRect(scrim, Offset(0f, top), Size(left, bottom - top))
        drawRect(scrim, Offset(right, top), Size(size.width - right, bottom - top))

        drawRect(Color.White, Offset(left, top), Size(right - left, bottom - top), style = Stroke(2.dp.toPx()))
        val handleRadius = 8.dp.toPx()
        listOf(Offset(left, top), Offset(right, top), Offset(left, bottom), Offset(right, bottom)).forEach { corner ->
            drawCircle(Color.White, handleRadius, corner)
            drawCircle(Color.Black.copy(alpha = 0.35f), handleRadius, corner, style = Stroke(1.dp.toPx()))
        }
    }
}
