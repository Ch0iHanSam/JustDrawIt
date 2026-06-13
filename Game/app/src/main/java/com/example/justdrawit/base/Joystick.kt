package com.example.justdrawit.base

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class Joystick(private val gctx: GameContext) : IGameObject {
    private val outerRadius = 150f
    private val innerRadius = 70f
    
    private var centerX = 0f
    private var centerY = 0f
    
    private var knobX = 0f
    private var knobY = 0f

    private var touchId = -1
    private val touchRect = RectF()

    private val outerPaint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.STROKE
        strokeWidth = 10f
        alpha = 150
    }

    private val innerPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
        alpha = 180
    }

    private val directionVector = PointF(0f, 0f)

    init {
        val padding = 100f
        centerX = padding + outerRadius
        centerY = gctx.metrics.height - padding - outerRadius
        
        knobX = centerX
        knobY = centerY

        touchRect.set(
            centerX - outerRadius,
            centerY - outerRadius,
            centerX + outerRadius,
            centerY + outerRadius
        )
    }

    fun getDirection(): PointF {
        return directionVector
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val actionIndex = event.actionIndex
        val pointerId = event.getPointerId(actionIndex)

        // 화면 좌표를 게임 가상 좌표로 변환
        val pt = gctx.metrics.fromScreen(event.getX(actionIndex), event.getY(actionIndex))
        val tx = pt.x
        val ty = pt.y

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (touchId == -1 && touchRect.contains(tx, ty)) {
                    touchId = pointerId
                    updateKnob(tx, ty)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchId != -1) {
                    val index = event.findPointerIndex(touchId)
                    if (index != -1) {
                        val movePt = gctx.metrics.fromScreen(event.getX(index), event.getY(index))
                        updateKnob(movePt.x, movePt.y)
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (pointerId == touchId) {
                    touchId = -1
                    knobX = centerX
                    knobY = centerY
                    directionVector.set(0f, 0f)
                    return true
                }
            }
        }
        return false
    }

    private fun updateKnob(tx: Float, ty: Float) {
        val dx = tx - centerX
        val dy = ty - centerY
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        if (distance <= outerRadius) {
            knobX = tx
            knobY = ty
        } else {
            val ratio = outerRadius / distance
            knobX = centerX + dx * ratio
            knobY = centerY + dy * ratio
        }

        // 방향 벡터 업데이트 (단위 벡터)
        if (distance > 0) {
            val currentDx = knobX - centerX
            val currentDy = knobY - centerY
            val currentDist = Math.sqrt((currentDx * currentDx + currentDy * currentDy).toDouble()).toFloat()
            if (currentDist > 0) {
                directionVector.set(currentDx / currentDist, currentDy / currentDist)
            } else {
                directionVector.set(0f, 0f)
            }
        } else {
            directionVector.set(0f, 0f)
        }
    }

    override fun update(gctx: GameContext) {
        // 나중에 터치 입력을 처리하여 knobX, knobY를 업데이트할 예정입니다.
    }

    override fun draw(canvas: Canvas) {
        // 큰 테두리 원
        canvas.drawCircle(centerX, centerY, outerRadius, outerPaint)
        
        // 파란색 내부 원 (노브)
        canvas.drawCircle(knobX, knobY, innerRadius, innerPaint)
    }
}
