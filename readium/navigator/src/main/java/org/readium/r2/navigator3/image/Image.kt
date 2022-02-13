package org.readium.r2.navigator3.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
internal fun Image(
    bitmap: ImageBitmap,
    scale: Float
) {
    val painter = remember(bitmap) { BitmapPainter(bitmap) }
    val contentScale = if (scale == 1f) ContentScale.Fit else FixedScale(scale)

    Layout(
        {},
        Modifier
            .paint(
                painter,
                alignment = Alignment.Center,
                sizeToIntrinsics = true,
                contentScale = contentScale,
                alpha = DefaultAlpha,
                colorFilter = null
            )
    ) { _, constraints ->
        layout(constraints.minWidth, constraints.minHeight) {}
    }
}

/**
 * Paint the content using [painter].
 *
 * @param sizeToIntrinsics `true` to size the element relative to [Painter.intrinsicSize]
 * @param alignment specifies alignment of the [painter] relative to content
 * @param contentScale strategy for scaling [painter] if its size does not match the content size
 * @param alpha opacity of [painter]
 * @param colorFilter optional [ColorFilter] to apply to [painter]
 */
private fun Modifier.paint(
    painter: BitmapPainter,
    sizeToIntrinsics: Boolean = true,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Inside,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
) = this.then(
    PainterModifier(
        painter = painter,
        sizeToIntrinsics = sizeToIntrinsics,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
        inspectorInfo = debugInspectorInfo {
            name = "paint"
            properties["painter"] = painter
            properties["sizeToIntrinsics"] = sizeToIntrinsics
            properties["alignment"] = alignment
            properties["contentScale"] = contentScale
            properties["alpha"] = alpha
            properties["colorFilter"] = colorFilter
        }
    )
)

/**
 * [DrawModifier] used to draw the provided [BitmapPainter] followed by the contents
 * of the component itself
 */
private class PainterModifier(
    val painter: BitmapPainter,
    val sizeToIntrinsics: Boolean,
    val alignment: Alignment = Alignment.Center,
    val contentScale: ContentScale = ContentScale.Inside,
    val alpha: Float = DefaultAlpha,
    val colorFilter: ColorFilter? = null,
    inspectorInfo: InspectorInfo.() -> Unit
) : LayoutModifier, DrawModifier, InspectorValueInfo(inspectorInfo) {

    private val Constraints.minSize
        get() = Size(minWidth.toFloat(), minHeight.toFloat())

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(modifyConstraints(constraints))
        return layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        return if (sizeToIntrinsics) {
            val constraints = Constraints(maxHeight = height)
            val size = modifyConstraints(constraints).minSize
            val scaledSize = calculateScaledSize(size)
            val imageWidth = max(scaledSize.width.roundToInt(), size.width.roundToInt())
            val layoutWidth = measurable.minIntrinsicWidth(height)
            max(imageWidth, layoutWidth)
        } else {
            measurable.minIntrinsicWidth(height)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        return measurable.maxIntrinsicWidth(height)
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        return if (sizeToIntrinsics) {
            val constraints = Constraints(maxWidth = width)
            val size = modifyConstraints(constraints).minSize
            val scaledSize = calculateScaledSize(size)
            val imageHeight = max(scaledSize.height.roundToInt(), size.height.roundToInt())
            val layoutHeight = measurable.minIntrinsicHeight(width)
            max(imageHeight, layoutHeight)
        } else {
            measurable.minIntrinsicHeight(width)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        return measurable.maxIntrinsicHeight(width)
    }

    private fun calculateScaledSize(dstSize: Size): Size {
        return if (!this.sizeToIntrinsics) {
            dstSize
        } else {
            val srcSize = painter.intrinsicSize
            if (dstSize.width != 0f && dstSize.height != 0f) {
                srcSize * contentScale.computeScaleFactor(srcSize, dstSize)
            } else {
                Size.Zero
            }
        }
    }

    private fun modifyConstraints(constraints: Constraints): Constraints {
        val hasFixedDimens =constraints.hasFixedWidth && constraints.hasFixedHeight
        // If we have fixed constraints, do not attempt to modify them.
        if (hasFixedDimens) {
            return constraints
        }

        val hasBoundedDimens = constraints.hasBoundedWidth && constraints.hasBoundedHeight
        // If we are not attempting to size the composable based on the size of the Painter, do not
        // attempt to modify them. In case of unbounded constraints, we use the size of the Painter
        // whatever the value of sizeToIntrinsics is.
        if (!sizeToIntrinsics && hasBoundedDimens) {
            return constraints.copy(
                minWidth = constraints.maxWidth,
                minHeight = constraints.maxHeight
            )
        }

        // Otherwise rely on Alignment and ContentScale to determine how to position
        // the drawing contents of the Painter within the provided bounds
        val intrinsicWidth = painter.intrinsicSize.width.roundToInt()
        val intrinsicHeight = painter.intrinsicSize.height.roundToInt()

        // Scale the width and height appropriately based on the given constraints
        // and ContentScale
        val constrainedWidth = constraints.constrainWidth(intrinsicWidth)
        val constrainedHeight = constraints.constrainHeight(intrinsicHeight)
        val scaledSize = calculateScaledSize(
            Size(constrainedWidth.toFloat(), constrainedHeight.toFloat())
        )

        // For both width and height constraints, consume the minimum of the scaled width
        // and the maximum constraint as some scale types can scale larger than the maximum
        // available size (ex ContentScale.Crop)
        // In this case the larger of the 2 dimensions is used and the aspect ratio is
        // maintained. Even if the size of the composable is smaller, the painter will
        // draw its content clipped
        val minWidth = constraints.constrainWidth(scaledSize.width.roundToInt())
        val minHeight = constraints.constrainHeight(scaledSize.height.roundToInt())
        return constraints.copy(minWidth = minWidth, minHeight = minHeight)
    }

    override fun ContentDrawScope.draw() {
        val srcSize = painter.intrinsicSize

        // Compute the offset to translate the content based on the given alignment
        // and size to draw based on the ContentScale parameter
        val scaledSize = if (size.width != 0f && size.height != 0f) {
            srcSize * contentScale.computeScaleFactor(srcSize, size)
        } else {
            Size.Zero
        }

        val alignedPosition = alignment.align(
            IntSize(scaledSize.width.roundToInt(), scaledSize.height.roundToInt()),
            IntSize(size.width.roundToInt(), size.height.roundToInt()),
            layoutDirection
        )

        val dx = alignedPosition.x.toFloat()
        val dy = alignedPosition.y.toFloat()

        // Only translate the current drawing position while delegating the Painter to draw
        // with scaled size.
        // Individual Painter implementations should be responsible for scaling their drawing
        // content accordingly to fit within the drawing area.
        translate(dx, dy) {
            with(painter) {
                draw(size = scaledSize, alpha = alpha, colorFilter = colorFilter)
            }
        }
    }

    override fun hashCode(): Int {
        var result = painter.hashCode()
        result = 31 * result + this.sizeToIntrinsics.hashCode()
        result = 31 * result + alignment.hashCode()
        result = 31 * result + contentScale.hashCode()
        result = 31 * result + alpha.hashCode()
        result = 31 * result + (colorFilter?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? PainterModifier ?: return false
        return painter == otherModifier.painter &&
                this.sizeToIntrinsics == otherModifier.sizeToIntrinsics &&
                alignment == otherModifier.alignment &&
                contentScale == otherModifier.contentScale &&
                alpha == otherModifier.alpha &&
                colorFilter == otherModifier.colorFilter
    }

    override fun toString(): String =
        "PainterModifier(" +
                "painter=$painter, " +
                "sizeToIntrinsics=${this.sizeToIntrinsics}, " +
                "alignment=$alignment, " +
                "alpha=$alpha, " +
                "colorFilter=$colorFilter)"
}
