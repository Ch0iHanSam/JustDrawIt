package com.example.justdrawit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class DirectionHud(private val gctx: GameContext, private val player: Player) : IGameObject {
    private val boxSize = 80f
    private val gap = 15f
    
    private val redPaint = Paint().apply { 
        color = Color.parseColor("#FF6B6B")
        style = Paint.Style.FILL 
    }
    private val greenPaint = Paint().apply { 
        color = Color.parseColor("#51CF66")
        style = Paint.Style.FILL 
    }

    override fun update(gctx: GameContext) {
    }

    override fun draw(canvas: Canvas) {
        val screenWidth = gctx.metrics.width
        val screenHeight = gctx.metrics.height
        
        // 왼쪽 하단 배치를 위한 기준점 계산
        // 십자 모양의 전체 폭을 고려하여 여백(padding)을 80f로 설정
        val padding = 80f
        val centerX = padding + boxSize + gap + (boxSize / 2f)
        val centerY = screenHeight - padding - boxSize - gap - (boxSize / 2f)

        // 상 (Up)
        drawBox(canvas, centerX, centerY - (boxSize + gap), player.isUpPressed())
        // 하 (Down)
        drawBox(canvas, centerX, centerY + (boxSize + gap), player.isDownPressed())
        // 좌 (Left)
        drawBox(canvas, centerX - (boxSize + gap), centerY, player.isLeftPressed())
        // 우 (Right)
        drawBox(canvas, centerX + (boxSize + gap), centerY, player.isRightPressed())
    }

    private fun drawBox(canvas: Canvas, x: Float, y: Float, isActive: Boolean) {
        val paint = if (isActive) greenPaint else redPaint
        canvas.drawRect(
            x - boxSize / 2,
            y - boxSize / 2,
            x + boxSize / 2,
            y + boxSize / 2,
            paint
        )
    }
}
