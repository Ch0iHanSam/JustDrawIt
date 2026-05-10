package com.example.justdrawit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class Enemy(
    private val gctx: GameContext, 
    startX: Float, 
    startY: Float, 
    private val player: Player,
    private val targetIndex: Int // 0~7 사이의 가상 지점 인덱스
) : Sprite(gctx, R.drawable.densis_illustration) {
    private var speed = 0f
    private val targetOffsetDist = 80f // 캐릭터 중심으로부터의 거리 (캐릭터와 겹치도록 축소)
    
    private val paint = Paint().apply {
        // 검정색을 덧씌워서 임시로 표현
        colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
    }

    init {
        x = startX
        y = startY
        width = 180f
        height = 200f
        syncDstRect()

        // 캐릭터 속도(화면 너비 / 5초)의 70%
        speed = (gctx.metrics.width / 5.0f) * 0.7f
    }

    override fun update(gctx: GameContext) {
        // 8개 지점 중 선택된 목표 지점 계산
        // 0: 상, 1: 상우, 2: 우, 3: 하우, 4: 하, 5: 하좌, 6: 좌, 7: 상좌
        val angle = Math.toRadians(targetIndex * 45.0 - 90.0) // 0도가 위쪽이 되도록 조정
        val targetWorldX = player.x + (Math.cos(angle) * targetOffsetDist).toFloat()
        val targetWorldY = player.y + (Math.sin(angle) * targetOffsetDist).toFloat()

        // 목표 지점 방향 벡터 계산
        var dx = targetWorldX - x
        var dy = targetWorldY - y
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)

        if (dist > 5f) { // 목표 지점에 거의 도달하면 부드럽게 유지
            dx /= dist
            dy /= dist
            x += dx * speed * gctx.frameTime
            y += dy * speed * gctx.frameTime
        }

        syncDstRect()
    }

    override fun draw(canvas: Canvas) {
        val screenWidth = gctx.metrics.width
        val screenHeight = gctx.metrics.height
        val mapSize = 200f * 20f

        // 배경 스크롤과 동일한 카메라 오프셋 계산
        var offsetX = player.x - screenWidth / 2
        var offsetY = player.y - screenHeight / 2

        // 맵 경계에서의 카메라 고정 로직 (Background와 동일하게)
        offsetX = offsetX.coerceIn(0f, (mapSize - screenWidth).coerceAtLeast(0f))
        offsetY = offsetY.coerceIn(0f, (mapSize - screenHeight).coerceAtLeast(0f))

        // 화면 좌표 계산
        val drawX = x - offsetX
        val drawY = y - offsetY

        val halfW = width / 2f
        val halfH = height / 2f
        
        val drawRect = android.graphics.RectF(
            drawX - halfW, drawY - halfH,
            drawX + halfW, drawY + halfH
        )
        canvas.drawBitmap(bitmap, srcRect, drawRect, paint)
    }
}
