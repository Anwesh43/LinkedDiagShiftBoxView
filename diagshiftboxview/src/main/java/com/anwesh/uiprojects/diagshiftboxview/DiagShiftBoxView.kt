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
val arcFactor : Float = 3f
val offsetFactor : Float = 3f
val sweepDeg : Float = 360f

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

fun Canvas.drawProgressArc(size : Float, scale : Float, h : Float, paint : Paint) {
    save()
    translate(0f, -h / offsetFactor)
    drawArc(RectF(-size / arcFactor, -size / arcFactor, size / arcFactor, size / arcFactor),
            0f, sweepDeg * scale, true, paint)
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
    drawProgressArc(size, scale, h, paint)
    for (j in 0..(lines - 1)) {
        x += xGap * sc1.divideScale(j, lines)
        drawRotLine(j, sc2, x, size, paint)
    }
    drawRect(RectF(-size, -size / boxHFactor, -size + 2 * size * sc1, size / boxHFactor), paint)
    restore()
}


class DiagShiftBoxView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, lines, 1)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class DSBNode(var i : Int, val state : State = State()) {

        private var next : DSBNode? = null
        private var prev : DSBNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = DSBNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawDSBNode(i, state.scale, paint)
            prev?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : DSBNode {
            var curr : DSBNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class DiagShiftBox(var i : Int) {

        private val root : DSBNode = DSBNode(0)
        private var curr : DSBNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : DiagShiftBoxView) {

        private val animator : Animator = Animator(view)
        private val dsb : DiagShiftBox = DiagShiftBox(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            dsb.draw(canvas, paint)
            animator.animate {
                dsb.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            dsb.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : DiagShiftBoxView {
            val view : DiagShiftBoxView = DiagShiftBoxView(activity)
            activity.setContentView(view)
            return view
        }
    }
}