package app.menosan.android.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Donut(slices: List<Pair<Float, Color>>, modifier: Modifier = Modifier, thickness: Dp = 16.dp) {
    val visible = slices.filter { it.first > 0f }
    val total = visible.sumOf { it.first.toDouble() }.toFloat()
    if (total <= 0f) return
    Canvas(modifier.fillMaxSize()) {
        val stroke = thickness.toPx()
        val gap = if (visible.size > 1) 4f else 0f
        val arcSize = Size(size.minDimension - stroke, size.minDimension - stroke)
        val topLeft = Offset((size.width - arcSize.width) / 2f, (size.height - arcSize.height) / 2f)
        var start = -90f
        visible.forEach { (value, color) ->
            val sweep = 360f * value / total
            drawArc(
                color = color,
                startAngle = start + gap / 2f,
                sweepAngle = (sweep - gap).coerceAtLeast(1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt),
            )
            start += sweep
        }
    }
}
