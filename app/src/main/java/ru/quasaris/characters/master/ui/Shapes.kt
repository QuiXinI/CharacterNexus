package ru.quasaris.characters.master.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


class PolygonShape(
    private val sides: Int,
    private val cornerRatio: Float = 0.38f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = createPolygonPath(size, sides, cornerRatio.coerceIn(0.0f, 0.5f))
        return Outline.Generic(path)
    }
}

fun createPolygonPath(size: Size, sides: Int, cornerRatio: Float = 0.38f): Path {
    val path = Path()
    if (sides < 3) return path

    val radius = minOf(size.width, size.height) / 2f
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val angle = 2.0 * PI / sides
    val startAngle = -PI / 2.0

    val vertices = List(sides) { i ->
        val a = startAngle + i * angle
        Offset(
            (centerX + radius * cos(a)).toFloat(),
            (centerY + radius * sin(a)).toFloat()
        )
    }

    fun getCornerPoints(i: Int): Pair<Offset, Offset> {
        val curr = vertices[i]
        val prev = vertices[(i - 1 + sides) % sides]
        val next = vertices[(i + 1) % sides]

        val p1 = curr + (prev - curr) * cornerRatio
        val p2 = curr + (next - curr) * cornerRatio
        return Pair(p1, p2)
    }

    val (firstP1, firstP2) = getCornerPoints(0)
    path.moveTo(firstP2.x, firstP2.y)

    for (i in 1 until sides) {
        val (p1, p2) = getCornerPoints(i)
        path.lineTo(p1.x, p1.y)
        path.quadraticTo(vertices[i].x, vertices[i].y, p2.x, p2.y)
    }

    val (lastP1, _) = getCornerPoints(0)
    path.lineTo(lastP1.x, lastP1.y)
    path.quadraticTo(vertices[0].x, vertices[0].y, firstP2.x, firstP2.y)
    path.close()

    return path
}

class MorphingPolygonShape(
    private val targetSides: Int,
    private val cornerRatio: Float = 0.38f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        if (targetSides < 3) return Outline.Generic(path)

        val radius = minOf(size.width, size.height) / 2f
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        val totalPoints = 24
        val safeRatio = cornerRatio.coerceIn(0.0f, 0.45f)

        fun getVertex(index: Int): Offset {
            val angle = -PI / 2.0 + (index % targetSides) * (2.0 * PI / targetSides)
            return Offset(
                (centerX + radius * cos(angle)).toFloat(),
                (centerY + radius * sin(angle)).toFloat()
            )
        }

        for (i in 0 until totalPoints) {
            val sideIndex = (i * targetSides) / totalPoints
            val nextSideIndex = (sideIndex + 1) % targetSides
            val prevSideIndex = (sideIndex - 1 + targetSides) % targetSides
            val nextNextSideIndex = (nextSideIndex + 1) % targetSides

            val sideProgress = (i.toDouble() * targetSides / totalPoints) - sideIndex

            val vCurr = getVertex(sideIndex)
            val vNext = getVertex(nextSideIndex)

            val x: Float
            val y: Float

            when {
                sideProgress < safeRatio -> {
                    val vPrev = getVertex(prevSideIndex)
                    val aCurr = vCurr + (vPrev - vCurr) * safeRatio
                    val bCurr = vCurr + (vNext - vCurr) * safeRatio
                    val t = 0.5f + 0.5f * (sideProgress.toFloat() / safeRatio)

                    x = (1 - t) * (1 - t) * aCurr.x + 2 * (1 - t) * t * vCurr.x + t * t * bCurr.x
                    y = (1 - t) * (1 - t) * aCurr.y + 2 * (1 - t) * t * vCurr.y + t * t * bCurr.y
                }
                sideProgress > 1.0 - safeRatio -> {
                    val vNextNext = getVertex(nextNextSideIndex)
                    val aNext = vNext + (vCurr - vNext) * safeRatio
                    val bNext = vNext + (vNextNext - vNext) * safeRatio
                    val t = 0.5f * ((sideProgress.toFloat() - (1f - safeRatio)) / safeRatio)

                    x = (1 - t) * (1 - t) * aNext.x + 2 * (1 - t) * t * vNext.x + t * t * bNext.x
                    y = (1 - t) * (1 - t) * aNext.y + 2 * (1 - t) * t * vNext.y + t * t * bNext.y
                }
                else -> {
                    x = vCurr.x + (vNext.x - vCurr.x) * sideProgress.toFloat()
                    y = vCurr.y + (vNext.y - vCurr.y) * sideProgress.toFloat()
                }
            }

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}