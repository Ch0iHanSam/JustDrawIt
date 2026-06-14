package com.example.justdrawit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class TitleScene(gctx: GameContext) : Scene(gctx) {
    private val titlePaint = Paint().apply {
        color = Color.BLACK
        textSize = 100f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    
    private val buttonBgPaint = Paint().apply {
        color = Color.LTGRAY
        style = Paint.Style.FILL
    }
    
    private val buttonBorderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    
    private val buttonTextPaint = Paint().apply {
        color = Color.BLACK
        textSize = 60f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val startButtonRect = RectF(250f, 1000f, 650f, 1150f)
    private val exitButtonRect = RectF(250f, 1200f, 650f, 1350f)

    override fun update(gctx: GameContext) {
    }

    override fun draw(canvas: Canvas) {
        // 흰색 배경
        canvas.drawColor(Color.WHITE)

        // 제목 출력
        canvas.drawText("Just Draw It!", gctx.metrics.width / 2, 400f, titlePaint)

        // Start 버튼
        drawButton(canvas, startButtonRect, "Start")

        // Exit 버튼
        drawButton(canvas, exitButtonRect, "Exit")
    }

    private fun drawButton(canvas: Canvas, rect: RectF, text: String) {
        canvas.drawRect(rect, buttonBgPaint)
        canvas.drawRect(rect, buttonBorderPaint)
        canvas.drawText(text, rect.centerX(), rect.centerY() + 20f, buttonTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val pt = gctx.metrics.fromScreen(event.x, event.y)
            if (startButtonRect.contains(pt.x, pt.y)) {
                // 게임 시작 (MainScene으로 변경)
                MainScene(gctx).change()
                return true
            }
            if (exitButtonRect.contains(pt.x, pt.y)) {
                // 종료 (Activity 종료 요청)
                gctx.view.context.let {
                    if (it is android.app.Activity) {
                        it.finish()
                    }
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
