package com.example.justdrawit.base

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class SpellHud(private val gctx: GameContext) : IGameObject {
    private val boxSize = 120f
    private val padding = 40f
    private val gap = 20f

    // 쿨타임 정보 (초 단위)
    var arrowCooldown = 0.5f
    var sprinkleCooldown = 1.0f

    // 현재 남은 쿨타임
    var arrowTimer = 0f
    var sprinkleTimer = 0f

    private val bgPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    
    private val coolPaint = Paint().apply {
        color = Color.WHITE
        alpha = 100 // 반투명
        style = Paint.Style.FILL
    }

    private val iconPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    private val borderPaint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    override fun update(gctx: GameContext) {
        if (arrowTimer > 0) arrowTimer -= gctx.frameTime
        if (sprinkleTimer > 0) sprinkleTimer -= gctx.frameTime
    }

    override fun draw(canvas: Canvas) {
        // 1번 칸: MagicArrow
        drawSpellBox(canvas, padding, padding, arrowTimer, arrowCooldown, IconType.ARROW)

        // 2번 칸: MagicSprinkle
        drawSpellBox(canvas, padding, padding + boxSize + gap, sprinkleTimer, sprinkleCooldown, IconType.CIRCLE)
    }

    private enum class IconType { ARROW, CIRCLE }

    private fun drawSpellBox(canvas: Canvas, x: Float, y: Float, current: Float, max: Float, type: IconType) {
        val rect = RectF(x, y, x + boxSize, y + boxSize)
        
        // 검정 배경
        canvas.drawRect(rect, bgPaint)

        // 아이콘 그리기
        val iconPadding = 20f
        val iconRect = RectF(rect.left + iconPadding, rect.top + iconPadding, rect.right - iconPadding, rect.bottom - iconPadding)
        
        when (type) {
            IconType.ARROW -> drawArrowIcon(canvas, iconRect)
            IconType.CIRCLE -> canvas.drawCircle(iconRect.centerX(), iconRect.centerY(), iconRect.width() / 2f, iconPaint)
        }

        // 쿨타임 오버레이 (아래에서 위로 차오름)
        if (current > 0) {
            val ratio = current / max
            val coolHeight = boxSize * ratio
            val coolRect = RectF(rect.left, rect.bottom - coolHeight, rect.right, rect.bottom)
            canvas.drawRect(coolRect, coolPaint)
        }

        // 테두리
        canvas.drawRect(rect, borderPaint)
    }

    private fun drawArrowIcon(canvas: Canvas, rect: RectF) {
        val path = Path()
        // 화살표 모양 (∧)
        path.moveTo(rect.centerX(), rect.top)
        path.lineTo(rect.left, rect.centerY())
        path.moveTo(rect.centerX(), rect.top)
        path.lineTo(rect.right, rect.centerY())
        path.moveTo(rect.centerX(), rect.top)
        path.lineTo(rect.centerX(), rect.bottom)
        canvas.drawPath(path, iconPaint)
    }
    
    fun canCastArrow(): Boolean = arrowTimer <= 0f
    fun canCastSprinkle(): Boolean = sprinkleTimer <= 0f
    
    fun startArrowCooldown() { arrowTimer = arrowCooldown }
    fun startSprinkleCooldown() { sprinkleTimer = sprinkleCooldown }
}
