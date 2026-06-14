package com.example.justdrawit.base

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class StatusHud(private val player: Player) : IGameObject {
    private val padding = 40f
    private val barWidth = 400f
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
        // HP Bar
        drawBar(canvas, padding, padding, player.hp, player.maxHp, Color.parseColor("#FFCDD2"), Color.RED)
        
        // MP Bar
        drawBar(canvas, padding, padding + barHeight + gap, player.mp, player.maxMp, Color.parseColor("#BBDEFB"), Color.BLUE)
    }

    private fun drawBar(canvas: Canvas, x: Float, y: Float, current: Float, max: Float, bgColor: Int, fgColor: Int) {
        val rect = RectF(x, y, x + barWidth, y + barHeight)
        
        // 배경 (연한 색)
        bgPaint.color = bgColor
        canvas.drawRect(rect, bgPaint)
        
        // 전경 (진한 색)
        val ratio = (current / max).coerceIn(0f, 1f)
        val fgRect = RectF(x, y, x + (barWidth * ratio), y + barHeight)
        fgPaint.color = fgColor
        canvas.drawRect(fgRect, fgPaint)
        
        // 테두리
        canvas.drawRect(rect, borderPaint)
    }
}
