package app.menosan.android.feature.reports

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Outline pictures for waste items that Material icons don't cover (a sachet, a PET bottle, a can…).
 * Drawn on Material's 24-unit grid with rounded strokes so they sit well next to Material outlined icons.
 * They're drawn in black and take the `tint` passed to `Icon`.
 */
internal object WasteIcons {
    val Leftovers by lazy {
        outline(
            "Leftovers",
            "M3 12h18",
            "M4 12c0 4.4 3.6 7 8 7s8-2.6 8-7",
            "M9.5 21h5",
            "M6.5 12c.5-2.5 3-4 5.5-4s5 1.5 5.5 4",
            "M10 2.5c-.8.8-.8 1.7 0 2.5",
            "M14 2.5c-.8.8-.8 1.7 0 2.5",
        )
    }

    val SpoiledFood by lazy {
        outline(
            "SpoiledFood",
            "M12 8c-1.5-1.2-3-1.5-4.5-1C5 7.8 4.2 10.5 4.8 13.5 5.5 17 7.8 20 10 20c.8 0 1.3-.4 2-.4s1.2.4 2 .4c2.2 0 4.5-3 5.2-6.5.6-3-.2-5.7-2.7-6.5-1.5-.5-3-.2-4.5 1z",
            "M12 8c0-2 .8-3.5 2.5-4.5",
            // One uneven bruise: round spots in pairs read as a face.
            FILL + "M12.2 12.3c1-1.3 3.2-1 3.7.7.4 1.5-.8 3-2.4 2.9-1.6-.1-2.4-2.3-1.3-3.6z",
            FILL + "M8.3 15.3a.8.8 0 1 0 1.6 0a.8.8 0 1 0 -1.6 0z",
        )
    }

    /** An apple core: what's left after eating. */
    val PeelsScraps by lazy {
        outline(
            "PeelsScraps",
            "M7 5.5c1-1.5 3-2.5 5-2.5s4 1 5 2.5c-1.5 1-3.2 1.5-5 1.5s-3.5-.5-5-1.5z",
            "M7.8 6.5c1.4 1.8 1.7 3.5 1.7 5.5s-.3 3.7-1.7 5.5",
            "M16.2 6.5c-1.4 1.8-1.7 3.5-1.7 5.5s.3 3.7 1.7 5.5",
            "M7 18.5c1.5-1 3.2-1.5 5-1.5s3.5.5 5 1.5c-1 1.5-3 2.5-5 2.5s-4-1-5-2.5z",
            "M12 3c0-.9.5-1.6 1.3-2",
            "M11 11v1.5",
            "M13 11v1.5",
        )
    }

    val PetBottle by lazy {
        outline(
            "PetBottle",
            "M10 2.5h4v2h-4z",
            "M10 4.5v1.5c0 .8-2.5 1.8-2.5 4V19a2 2 0 0 0 2 2h5a2 2 0 0 0 2-2V10c0-2.2-2.5-3.2-2.5-4V4.5",
            "M7.5 12.5h9",
            "M7.5 16h9",
        )
    }

    /** A detergent jug with a cap and handle (kept apart from the bin used for "other residual"). */
    val PlasticTub by lazy {
        outline(
            "PlasticTub",
            "M5 10.5a3 3 0 0 1 3-3h1.5V5h3v2.5H16a3 3 0 0 1 3 3V19a2 2 0 0 1 -2 2H7a2 2 0 0 1 -2-2z",
            "M9 3.5h4",
            "M15 7.5V5.5a1.5 1.5 0 0 1 3 0v3",
            "M8.5 13h7v4.5h-7z",
        )
    }

    val GlassJar by lazy {
        outline(
            "GlassJar",
            "M7.5 3h9a.5.5 0 0 1 .5.5v2a.5.5 0 0 1 -.5.5h-9a.5.5 0 0 1 -.5-.5v-2a.5.5 0 0 1 .5-.5z",
            "M8 6v1c-1.5.8-2 1.8-2 3v8.5A2.5 2.5 0 0 0 8.5 21h7a2.5 2.5 0 0 0 2.5-2.5V10c0-1.2-.5-2.2-2-3V6",
            "M9 11.5v5",
        )
    }

    val MetalCan by lazy {
        outline(
            "MetalCan",
            // A soda can: tapered rim and base, a label band, and the pull tab.
            "M8.5 3h7L17 5.5V19l-1.5 2h-7L7 19V5.5z",
            "M7 8.5h10",
            "M7 16h10",
            "M11 5.2h2",
        )
    }

    /** A small packet with crimped ends and a drop, like a shampoo or 3-in-1 sachet. */
    val Sachet by lazy {
        outline(
            "Sachet",
            "M5 6l1-1.5 1 1.5 1-1.5 1 1.5 1-1.5 1 1.5 1-1.5 1 1.5 1-1.5 1 1.5 1-1.5 1 1.5 1-1.5 1 1.5" +
                "V18l-1 1.5-1-1.5-1 1.5-1-1.5-1 1.5-1-1.5-1 1.5-1-1.5-1 1.5-1-1.5-1 1.5-1-1.5-1 1.5-1-1.5z",
            "M12 9c-1.6 2.1-2.4 3.3-2.4 4.4a2.4 2.4 0 0 0 4.8 0c0-1.1-.8-2.3-2.4-4.4z",
        )
    }

    /** A sando (T-shirt) bag with its two handles. */
    val PlasticBag by lazy {
        outline(
            "PlasticBag",
            "M5.5 9l-.9 10.4A1.5 1.5 0 0 0 6.1 21h11.8a1.5 1.5 0 0 0 1.5-1.6L18.5 9",
            "M5.5 9C7 9 8 7.8 8 6V3h2.2v3c0 1.4.8 2.5 1.8 2.5s1.8-1.1 1.8-2.5V3H16v3c0 1.8 1 3 2.5 3",
            // Side folds, so it reads as thin plastic rather than a shirt.
            "M9 12.5c.5 1.5.5 3 0 4.5",
            "M15 12.5c-.5 1.5-.5 3 0 4.5",
        )
    }

    /** A wrapped candy with twisted ends. */
    val SnackWrapper by lazy {
        outline(
            "SnackWrapper",
            "M8 12a4 3.2 0 1 0 8 0a4 3.2 0 1 0 -8 0",
            "M8.2 11l-4.7-3v8l4.7-3",
            "M15.8 11l4.7-3v8l-4.7-3",
        )
    }

    /** A closed styro clamshell, like carinderia takeout. */
    val Clamshell by lazy {
        outline(
            "Clamshell",
            "M3 12h18",
            "M4 12l1.5-5a1 1 0 0 1 1-.7h11a1 1 0 0 1 1 .7L20 12",
            "M4 12l1.5 6.3a1 1 0 0 0 1 .7h11a1 1 0 0 0 1-.7L20 12",
            "M10.5 12v1.8h3V12",
        )
    }

    /** A takeout cup with a lid and straw. */
    val CupStraw by lazy {
        outline(
            "CupStraw",
            "M6.2 8h11.6l-1.3 11.2a1.5 1.5 0 0 1 -1.5 1.3H9a1.5 1.5 0 0 1 -1.5-1.3z",
            "M5.5 8h13V6.8a1 1 0 0 0 -1-1h-11a1 1 0 0 0 -1 1z",
            "M12.5 5.8L14 2l3 .8",
        )
    }

    /** A paper roll with a sheet hanging down, for tissue and paper towels. */
    val TissueBox by lazy {
        outline(
            "TissueBox",
            "M3 9a4 4 0 1 0 8 0a4 4 0 1 0 -8 0",
            "M5.8 9a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0 -2.4 0",
            "M7 5h10c2.2 0 4 1.8 4 4v11.5h-5V13H7",
            "M18.5 16.5v1.5",
        )
    }

    val Diaper by lazy {
        outline(
            "Diaper",
            "M3 6h18v3c0 5-3.5 9.5-8 10.5h-2C6.5 18.5 3 14 3 9z",
            "M3 9h18",
            "M7.5 12.5c1.2.4 2 1.6 2.5 3.3",
            "M16.5 12.5c-1.2.4-2 1.6-2.5 3.3",
        )
    }

    /** Marks a path that is filled solid instead of stroked, for small details like a spot. */
    private const val FILL = "fill:"

    private fun outline(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                paths.forEach { data ->
                    if (data.startsWith(FILL)) {
                        addPath(pathData = addPathNodes(data.removePrefix(FILL)), fill = SolidColor(Color.Black))
                    } else {
                        addPath(
                            pathData = addPathNodes(data),
                            stroke = SolidColor(Color.Black),
                            strokeLineWidth = 1.8f,
                            strokeLineCap = StrokeCap.Round,
                            strokeLineJoin = StrokeJoin.Round,
                        )
                    }
                }
            }
            .build()
}
