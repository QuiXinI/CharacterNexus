package ru.quasaris.characternexus.backend.cropper

import kotlin.math.cos
import kotlin.math.sin

/**
 * A simple 3x3 matrix implementation mimicking android.graphics.Matrix behavior for 2D transformations.
 */
class Matrix3x3 {
    val values = FloatArray(9)

    init {
        reset()
    }

    constructor()

    constructor(src: Matrix3x3) {
        src.values.copyInto(values)
    }

    fun reset() {
        values[0] = 1f; values[1] = 0f; values[2] = 0f
        values[3] = 0f; values[4] = 1f; values[5] = 0f
        values[6] = 0f; values[7] = 0f; values[8] = 1f
    }

    fun set(src: Matrix3x3) {
        src.values.copyInto(values)
    }

    fun getValues(out: FloatArray) {
        values.copyInto(out)
    }

    fun setValues(src: FloatArray) {
        src.copyInto(values)
    }

    /**
     * Set this matrix to a * b
     */
    fun setConcat(a: Matrix3x3, b: Matrix3x3): Matrix3x3 {
        val a00 = a.values[0]; val a01 = a.values[1]; val a02 = a.values[2]
        val a10 = a.values[3]; val a11 = a.values[4]; val a12 = a.values[5]
        val a20 = a.values[6]; val a21 = a.values[7]; val a22 = a.values[8]

        val b00 = b.values[0]; val b01 = b.values[1]; val b02 = b.values[2]
        val b10 = b.values[3]; val b11 = b.values[4]; val b12 = b.values[5]
        val b20 = b.values[6]; val b21 = b.values[7]; val b22 = b.values[8]

        values[0] = a00 * b00 + a01 * b10 + a02 * b20
        values[1] = a00 * b01 + a01 * b11 + a02 * b21
        values[2] = a00 * b02 + a01 * b12 + a02 * b22

        values[3] = a10 * b00 + a11 * b10 + a12 * b20
        values[4] = a10 * b01 + a11 * b11 + a12 * b21
        values[5] = a10 * b02 + a11 * b12 + a12 * b22

        values[6] = a20 * b00 + a21 * b10 + a22 * b20
        values[7] = a20 * b01 + a21 * b11 + a22 * b21
        values[8] = a20 * b02 + a21 * b12 + a22 * b22

        return this
    }

    fun postTranslate(dx: Float, dy: Float) {
        val t = Matrix3x3()
        t.values[2] = dx
        t.values[5] = dy
        val current = Matrix3x3(this)
        setConcat(t, current)
    }

    fun postScale(sx: Float, sy: Float, px: Float, py: Float) {
        val t = Matrix3x3()
        t.postTranslate(px, py)
        val s = Matrix3x3()
        s.values[0] = sx
        s.values[4] = sy
        val t2 = Matrix3x3()
        t2.postTranslate(-px, -py)
        
        // Final = T(px,py) * S(sx,sy) * T(-px,-py) * Current
        val combined = Matrix3x3()
        combined.setConcat(t, s)
        combined.setConcat(combined, t2)
        
        val current = Matrix3x3(this)
        setConcat(combined, current)
    }

    fun postScale(sx: Float, sy: Float) {
        val s = Matrix3x3()
        s.values[0] = sx
        s.values[4] = sy
        val current = Matrix3x3(this)
        setConcat(s, current)
    }

    fun postRotate(degrees: Float, px: Float, py: Float) {
        val t = Matrix3x3()
        t.postTranslate(px, py)
        
        val r = Matrix3x3()
        val radians = degrees * (kotlin.math.PI.toFloat() / 180f)
        val c = cos(radians)
        val s = sin(radians)
        r.values[0] = c; r.values[1] = -s
        r.values[3] = s; r.values[4] = c
        
        val t2 = Matrix3x3()
        t2.postTranslate(-px, -py)

        val combined = Matrix3x3()
        combined.setConcat(t, r)
        combined.setConcat(combined, t2)
        
        val current = Matrix3x3(this)
        setConcat(combined, current)
    }

    fun mapPoints(pts: FloatArray) {
        for (i in 0 until pts.size step 2) {
            val x = pts[i]
            val y = pts[i + 1]
            val w = values[6] * x + values[7] * y + values[8]
            pts[i] = (values[0] * x + values[1] * y + values[2]) / w
            pts[i + 1] = (values[3] * x + values[4] * y + values[5]) / w
        }
    }
    
    // Helper to convert to Compose Matrix
    fun toComposeMatrix(): androidx.compose.ui.graphics.Matrix {
        val m = androidx.compose.ui.graphics.Matrix()
        val v = m.values
        v[0] = values[0]; v[4] = values[1]; v[8] = 0f; v[12] = values[2]
        v[1] = values[3]; v[5] = values[4]; v[9] = 0f; v[13] = values[5]
        v[2] = 0f;        v[6] = 0f;        v[10] = 1f; v[14] = 0f
        v[3] = values[6]; v[7] = values[7]; v[11] = 0f; v[15] = values[8]
        return m
    }
}
