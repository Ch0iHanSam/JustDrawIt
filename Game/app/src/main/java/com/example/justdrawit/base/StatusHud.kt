package com.example.justdrawit.base

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class StatusHud(private val player: Player) : IGameObject {
    private val padding = 40f
    private val barHeight = 30f
    private val gap = 15f

    private val bgPaint = Paint().apply { style = Paint.Style.FILL }
    private val fgPaint = Paint().apply { style = Paint.Style.FILL }
    private val borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    override fun update(gctx: GameContext) {}

    override fun draw(canvas: Canvas) {
        // HP Bar (고정 길이 400f)
        drawBar(canvas, padding, padding, player.hp, player.maxHp, 400f, Color.parseColor("#FFCDD2"), Color.RED)
        
        // MP Bar (최대 마나량에 비례하여 길이 변화: 초기 1/3 -> 12개 획득 시 1/1)
        // 최대치 300일 때 400f가 되도록 계산
        val mpBarWidth = (player.maxMp / 300f) * 400f
        drawBar(canvas, padding, padding + barHeight + gap, player.mp, player.maxMp, mpBarWidth, Color.parseColor("#BBDEFB"), Color.BLUE)
    }

    private fun drawBar(canvas: Canvas, x: Float, y: Float, current: Float, max: Float, currentBarWidth: Float, bgColor: Int, fgColor: Int) {
        val rect = RectF(x, y, x + currentBarWidth, y + barHeight)
        
        // 배경 (연한 색)
        bgPaint.color = bgColor
        canvas.drawRect(rect, bgPaint)
        
        // 전경 (진한 색)
        val ratio = if (max > 0) (current / max).coerceIn(0f, 1f) else 0f
        val fgRect = RectF(x, y, x + (currentBarWidth * ratio), y + barHeight)
        fgPaint.color = fgColor
        canvas.drawRect(fgRect, fgPaint)
        
        // 테두리
        canvas.drawRect(rect, borderPaint)
    }
}
