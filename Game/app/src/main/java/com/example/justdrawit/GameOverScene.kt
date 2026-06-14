package com.example.justdrawit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class GameOverScene(gctx: GameContext) : Scene(gctx) {
    override val isTransparent = true // 뒷배경(게임 화면)이 보이도록 설정

    private val overlayPaint = Paint().apply {
        color = Color.WHITE
        alpha = 180 // 반투명한 흰색
        style = Paint.Style.FILL
    }

    private val titlePaint = Paint().apply {
        color = Color.RED
        textSize = 150f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val buttonBgPaint = Paint().apply {
        color = Color.DKGRAY
        style = Paint.Style.FILL
    }

    private val buttonTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val restartButtonRect = RectF(250f, 1000f, 650f, 1120f)
    private val mainMenuButtonRect = RectF(250f, 1150f, 650f, 1270f)

    override fun update(gctx: GameContext) {
        // 게임 오버 상태에서는 아무것도 업데이트하지 않음 (정지 효과)
    }

    override fun draw(canvas: Canvas) {
        // 1. 반투명 레이어 덮기
        canvas.drawRect(gctx.metrics.borderRect, overlayPaint)

        // 2. GameOver 텍스트
        canvas.drawText("GameOver", gctx.metrics.width / 2, 600f, titlePaint)

        // 3. Restart 버튼
        drawButton(canvas, restartButtonRect, "Restart")

        // 4. Main Menu 버튼
        drawButton(canvas, mainMenuButtonRect, "Main Menu")
    }

    private fun drawButton(canvas: Canvas, rect: RectF, text: String) {
        canvas.drawRect(rect, buttonBgPaint)
        canvas.drawText(text, rect.centerX(), rect.centerY() + 18f, buttonTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val pt = gctx.metrics.fromScreen(event.x, event.y)
            
            if (restartButtonRect.contains(pt.x, pt.y)) {
                // 게임 재시작: 현재 씬들을 모두 제거하고 MainScene으로 교환
                MainScene(gctx).change()
                return true
            }
            
            if (mainMenuButtonRect.contains(pt.x, pt.y)) {
                // 메인 메뉴로: TitleScene으로 교환
                TitleScene(gctx).change()
                return true
            }
        }
        return true // 게임 오버 화면이 모든 터치를 소비함
    }
}
