package com.tradesketch.estimator.domain.calc

import com.tradesketch.estimator.domain.model.Geometry
import com.tradesketch.estimator.domain.model.Space
import com.tradesketch.estimator.domain.model.SpaceTransform
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sqrt

object BlueprintLayoutOptimizer {
    fun optimize(
        spaces: List<Space>,
        gridFeet: Double = 1.0
    ): List<Space> {
        if (spaces.isEmpty()) return spaces
        val step = gridFeet.coerceAtLeast(MIN_GRID_FEET)
        val spacing = seedSpacing(spaces = spaces, stepFeet = step)
        val total = spaces.size
        val autoPlaced = BooleanArray(total)

        val seeded = spaces.mapIndexed { index, space ->
            val transform = space.transform
            val suggestion = suggestedTransformForIndex(index = index, total = total, spacingFeet = spacing)
            val isAtOrigin = abs(transform.xFeet) < 0.01 && abs(transform.zFeet) < 0.01
            val shouldAutoPlace = transform == SpaceTransform() || (isAtOrigin && total > 1)
            autoPlaced[index] = shouldAutoPlace
            val nextTransform = transform.copy(
                xFeet = if (shouldAutoPlace) suggestion.xFeet else snapToStep(transform.xFeet, step),
                yFeet = 0.0,
                zFeet = if (shouldAutoPlace) suggestion.zFeet else snapToStep(transform.zFeet, step),
                yawDegrees = normalizeDegrees(snapToStep(transform.yawDegrees, YAW_STEP_DEGREES)),
                colorHex = if (transform.colorHex == SpaceTransform().colorHex) {
                    palette[index % palette.size]
                } else {
                    transform.colorHex
                }
            )
            if (nextTransform == transform) {
                space
            } else {
                space.copy(transform = nextTransform)
            }
        }

        val resolvedTransforms = resolveOverlaps(
            spaces = seeded,
            autoPlaced = autoPlaced,
            stepFeet = step
        )

        return seeded.mapIndexed { index, space ->
            val transform = resolvedTransforms[index]
            val normalized = transform.copy(
                xFeet = snapToStep(transform.xFeet, step),
                yFeet = 0.0,
                zFeet = snapToStep(transform.zFeet, step),
                yawDegrees = normalizeDegrees(snapToStep(transform.yawDegrees, YAW_STEP_DEGREES))
            )
            if (normalized == space.transform) {
                space
            } else {
                space.copy(transform = normalized)
            }
        }
    }
}

private const val MIN_GRID_FEET = 0.25
private const val MIN_CLEARANCE_FEET = 0.5
private const val YAW_STEP_DEGREES = 15.0
private const val CLEARANCE_FACTOR = 0.5
private const val SHIFT_PADDING_FACTOR = 0.35
private const val MIN_ITERATIONS = 40
private const val ITERATIONS_PER_SPACE = 18

private data class FootprintSize(
    val widthFeet: Double,
    val depthFeet: Double
)

private val palette = listOf(
    0xFF4E79A7L,
    0xFFE15759L,
    0xFF76B7B2L,
    0xFFF28E2BL,
    0xFF59A14FL,
    0xFFEDC948L
)

private fun seedSpacing(spaces: List<Space>, stepFeet: Double): Double {
    val maxFootprint = spaces.maxOfOrNull { space ->
        val size = footprintDimensions(space.geometry)
        max(size.widthFeet, size.depthFeet)
    } ?: 16.0
    val rawSpacing = max(16.0, maxFootprint + 6.0)
    return snapToStep(rawSpacing, stepFeet).coerceAtLeast(16.0)
}

private fun resolveOverlaps(
    spaces: List<Space>,
    autoPlaced: BooleanArray,
    stepFeet: Double
): List<SpaceTransform> {
    if (spaces.size < 2) return spaces.map { it.transform }
    val transforms = spaces.map { it.transform }.toMutableList()
    val footprintSizes = spaces.map { space -> footprintDimensions(space.geometry) }
    val clearance = max(MIN_CLEARANCE_FEET, stepFeet * CLEARANCE_FACTOR)
    val maxIterations = max(MIN_ITERATIONS, spaces.size * ITERATIONS_PER_SPACE)

    repeat(maxIterations) { iteration ->
        var conflictCount = 0
        for (i in 0 until spaces.lastIndex) {
            for (j in (i + 1) until spaces.size) {
                val a = transforms[i]
                val b = transforms[j]
                val sizeA = footprintSizes[i]
                val sizeB = footprintSizes[j]
                val dx = b.xFeet - a.xFeet
                val dz = b.zFeet - a.zFeet
                val overlapX = (sizeA.widthFeet / 2.0 + sizeB.widthFeet / 2.0 + clearance) - abs(dx)
                val overlapZ = (sizeA.depthFeet / 2.0 + sizeB.depthFeet / 2.0 + clearance) - abs(dz)
                if (overlapX > 0.0 && overlapZ > 0.0) {
                    conflictCount += 1
                    val moverIndex = selectMover(i, j, autoPlaced)
                    val anchorIndex = if (moverIndex == i) j else i
                    val mover = transforms[moverIndex]
                    val anchor = transforms[anchorIndex]
                    val moveAlongX = overlapX <= overlapZ
                    val direction = directionForMove(
                        moverValue = if (moveAlongX) mover.xFeet else mover.zFeet,
                        anchorValue = if (moveAlongX) anchor.xFeet else anchor.zFeet,
                        fallbackSeed = moverIndex + iteration + conflictCount
                    )
                    val overlap = if (moveAlongX) overlapX else overlapZ
                    val shift = (overlap + stepFeet * SHIFT_PADDING_FACTOR)
                        .coerceAtLeast(stepFeet / 2.0)
                    val shifted = if (moveAlongX) {
                        mover.copy(
                            xFeet = snapToStep(mover.xFeet + (direction * shift), stepFeet),
                            yFeet = 0.0
                        )
                    } else {
                        mover.copy(
                            zFeet = snapToStep(mover.zFeet + (direction * shift), stepFeet),
                            yFeet = 0.0
                        )
                    }
                    transforms[moverIndex] = shifted
                }
            }
        }
        if (conflictCount == 0) {
            return transforms
        }
    }

    return repackConflictingSpaces(
        transforms = transforms,
        sizes = footprintSizes,
        stepFeet = stepFeet,
        clearanceFeet = clearance
    )
}

private fun repackConflictingSpaces(
    transforms: MutableList<SpaceTransform>,
    sizes: List<FootprintSize>,
    stepFeet: Double,
    clearanceFeet: Double
): List<SpaceTransform> {
    val conflictIndices = conflictingIndices(
        transforms = transforms,
        sizes = sizes,
        clearanceFeet = clearanceFeet
    )
    if (conflictIndices.isEmpty()) return transforms

    val sortedIndices = conflictIndices.toList().sorted()
    val centerX = sortedIndices.map { transforms[it].xFeet }.average()
    val centerZ = sortedIndices.map { transforms[it].zFeet }.average()
    val largestSpan = sortedIndices.maxOf { index ->
        max(sizes[index].widthFeet, sizes[index].depthFeet)
    }
    val spacing = max(stepFeet * 4.0, largestSpan + (clearanceFeet * 1.8))
    val columns = ceil(sqrt(sortedIndices.size.toDouble())).toInt().coerceAtLeast(1)
    val rows = ceil(sortedIndices.size.toDouble() / columns).toInt().coerceAtLeast(1)

    sortedIndices.forEachIndexed { index, spaceIndex ->
        val row = index / columns
        val col = index % columns
        val offsetX = (col - ((columns - 1) / 2.0)) * spacing
        val offsetZ = (row - ((rows - 1) / 2.0)) * spacing
        transforms[spaceIndex] = transforms[spaceIndex].copy(
            xFeet = snapToStep(centerX + offsetX, stepFeet),
            yFeet = 0.0,
            zFeet = snapToStep(centerZ + offsetZ, stepFeet)
        )
    }
    return transforms
}

private fun conflictingIndices(
    transforms: List<SpaceTransform>,
    sizes: List<FootprintSize>,
    clearanceFeet: Double
): Set<Int> {
    val conflicts = mutableSetOf<Int>()
    for (i in 0 until transforms.lastIndex) {
        val a = transforms[i]
        val sizeA = sizes[i]
        for (j in (i + 1) until transforms.size) {
            val b = transforms[j]
            val sizeB = sizes[j]
            val overlapX = (sizeA.widthFeet / 2.0 + sizeB.widthFeet / 2.0 + clearanceFeet) - abs(b.xFeet - a.xFeet)
            val overlapZ = (sizeA.depthFeet / 2.0 + sizeB.depthFeet / 2.0 + clearanceFeet) - abs(b.zFeet - a.zFeet)
            if (overlapX > 0.0 && overlapZ > 0.0) {
                conflicts += i
                conflicts += j
            }
        }
    }
    return conflicts
}

private fun selectMover(i: Int, j: Int, autoPlaced: BooleanArray): Int {
    val iAutoPlaced = autoPlaced.getOrElse(i) { false }
    val jAutoPlaced = autoPlaced.getOrElse(j) { false }
    return when {
        iAutoPlaced && !jAutoPlaced -> i
        !iAutoPlaced && jAutoPlaced -> j
        else -> j
    }
}

private fun directionForMove(
    moverValue: Double,
    anchorValue: Double,
    fallbackSeed: Int
): Double {
    val delta = moverValue - anchorValue
    if (abs(delta) >= 0.01) {
        return if (delta >= 0.0) 1.0 else -1.0
    }
    return if (fallbackSeed % 2 == 0) 1.0 else -1.0
}

private fun suggestedTransformForIndex(
    index: Int,
    total: Int,
    spacingFeet: Double
): SpaceTransform {
    if (total <= 0) return SpaceTransform(colorHex = palette[index % palette.size])
    val columns = ceil(sqrt(total.toDouble())).toInt().coerceAtLeast(1)
    val rows = ceil(total.toDouble() / columns).toInt().coerceAtLeast(1)
    val row = index / columns
    val col = index % columns
    val centeredCol = col - ((columns - 1) / 2.0)
    val centeredRow = row - ((rows - 1) / 2.0)
    return SpaceTransform(
        xFeet = centeredCol * spacingFeet,
        yFeet = 0.0,
        zFeet = centeredRow * spacingFeet,
        yawDegrees = 0.0,
        colorHex = palette[index % palette.size]
    )
}

private fun footprintDimensions(geometry: Geometry): FootprintSize {
    return when (geometry) {
        is Geometry.Rect -> FootprintSize(
            widthFeet = geometry.length.toFeet(),
            depthFeet = geometry.width.toFeet()
        )
        is Geometry.Slab -> FootprintSize(
            widthFeet = geometry.length.toFeet(),
            depthFeet = geometry.width.toFeet()
        )
        is Geometry.Wall -> FootprintSize(
            widthFeet = geometry.length.toFeet(),
            depthFeet = 0.75
        )
        is Geometry.Circle -> {
            val diameter = geometry.radius.toFeet() * 2.0
            FootprintSize(widthFeet = diameter, depthFeet = diameter)
        }
        is Geometry.LShape -> FootprintSize(
            widthFeet = max(geometry.rectA.length.toFeet(), geometry.rectB.length.toFeet()),
            depthFeet = max(geometry.rectA.width.toFeet(), geometry.rectB.width.toFeet())
        )
    }
}

private fun normalizeDegrees(value: Double): Double {
    var normalized = value % 360.0
    if (normalized > 180.0) normalized -= 360.0
    if (normalized < -180.0) normalized += 360.0
    return normalized
}

private fun snapToStep(value: Double, step: Double): Double {
    if (step <= 0.0) return value
    return round(value / step) * step
}
