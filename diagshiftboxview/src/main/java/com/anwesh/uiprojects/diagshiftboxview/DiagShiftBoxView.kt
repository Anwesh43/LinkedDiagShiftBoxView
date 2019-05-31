package com.anwesh.uiprojects.diagshiftboxview

/**
 * Created by anweshmishra on 31/05/19.
 */

import android.view.View
import android.view.MotionEvent
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.Color
import android.app.Activity
import android.content.Context

val nodes : Int = 5
val lines : Int = 4
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#283593")
val backColor : Int = Color.parseColor("#BDBDBD")
val rotDeg : Float = 90f
val boxHFactor : Float = 4f

fun Int.inverse() : Float = 1f / this
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.mirrorValue(a : Int, b : Int) : Float {
    val k : Float = scaleFactor()
    return (1 - k) * a.inverse() + k * b.inverse()
}
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap

fun Canvas.drawRotLine(i : Int, sc : Float, x : Float, size : Float, paint : Paint) {
    save()
    translate(x, 0f)
    rotate(rotDeg * sc.divideScale(i, lines))
    drawLine(0f, -size / 2, 0f, size / 2, paint)
    restore()
}

fun Canvas.drawDSBNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    paint.color = foreColor
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    var x : Float = 0f
    val xGap : Float = 2 * size / (lines)
    save()
    translate(w / 2, gap * (i + 1))
    for (j in 0..(lines - 1)) {
        x += xGap * sc1.divideScale(j, lines)
        drawRotLine(j, sc2, x, size, paint)
    }
    drawRect(RectF(-size, -size / boxHFactor, -size + 2 * size * sc1, size / boxHFactor), paint)
    restore()
}


class DiagShiftBoxView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas : Canvas) {

    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

            }
        }
        return true
    }
}