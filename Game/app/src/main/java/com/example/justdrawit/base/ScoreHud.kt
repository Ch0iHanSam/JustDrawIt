package com.example.justdrawit.base

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class ScoreHud(private val gctx: GameContext, private val player: Player, private val getPhase: () -> Int) : IGameObject {
    private val boxW = 200f
    private val boxH = 60f
    private val padding = 40f
    private val gap = 10f

    private val bgPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
    private val borderPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f }
    private val textPaint = Paint().apply { color = Color.BLACK; textSize = 30f; textAlign = Paint.Align.CENTER }

    override fun update(gctx: GameContext) {}

    override fun draw(canvas: Canvas) {
        val screenWidth = gctx.metrics.width
        val top = 300f // Minimap 아래 위치 (Minimap 200 + padding 50 + extra)
        val right = screenWidth - padding
        
        val score = (player.normalKills + player.eliteKills * 10 + player.playTimeSeconds.toInt() * 5)
        
        // 칸 1: 점수
        val rect1 = RectF(right - boxW, top, right, top + boxH)
        canvas.drawRect(rect1, bgPaint)
        canvas.drawRect(rect1, borderPaint)
        canvas.drawText("Score: $score", rect1.centerX(), rect1.centerY() + 10f, textPaint)

        // 칸 2: 페이즈
        val rect2 = RectF(right - boxW, top + boxH + gap, right, top + boxH * 2 + gap)
        canvas.drawRect(rect2, bgPaint)
        canvas.drawRect(rect2, borderPaint)
        canvas.drawText("Phase: ${getPhase()}", rect2.centerX(), rect2.centerY() + 10f, textPaint)
    }
}
