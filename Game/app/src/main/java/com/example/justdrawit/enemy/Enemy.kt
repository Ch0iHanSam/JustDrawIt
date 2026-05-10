package com.example.justdrawit.enemy

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import com.example.justdrawit.R
import com.example.justdrawit.base.Player
import com.example.justdrawit.base.Speed
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

        // 속도 설정
        speed = Speed.getEnemySpeed(gctx)
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

    fun getScreenRect(): RectF {
        val screenWidth = gctx.metrics.width
        val screenHeight = gctx.metrics.height
        val mapSize = 200f * 20f

        var offsetX = player.x - screenWidth / 2
        var offsetY = player.y - screenHeight / 2
        offsetX = offsetX.coerceIn(0f, (mapSize - screenWidth).coerceAtLeast(0f))
        offsetY = offsetY.coerceIn(0f, (mapSize - screenHeight).coerceAtLeast(0f))

        val drawX = x - offsetX
        val drawY = y - offsetY

        val halfW = width / 2f
        val halfH = height / 2f
        
        return RectF(
            drawX - halfW, drawY - halfH,
            drawX + halfW, drawY + halfH
        )
    }

    fun getBoundingRect(): RectF {
        val halfW = width / 2f
        val halfH = height / 2f
        return RectF(x - halfW, y - halfH, x + halfW, y + halfH)
    }

    companion object {
        fun randomSpawn(gctx: GameContext, player: Player): Enemy {
            val random = java.util.Random()
            // 최소 거리: 화면 세로 길이를 지름으로 하는 원의 반지름 (즉, 세로 길이의 절반)
            val minDistance = gctx.metrics.height / 2f
            val maxDistance = minDistance + 500f // 최대 거리는 최소 거리 + 500f 정도로 설정
            
            // 랜덤한 각도(0~360도) 선택
            val spawnAngle = random.nextDouble() * 2.0 * Math.PI
            // 최소~최대 사이의 랜덤한 거리 선택
            val distance = minDistance + random.nextFloat() * (maxDistance - minDistance)
            
            val enemyX = player.x + (Math.cos(spawnAngle) * distance).toFloat()
            val enemyY = player.y + (Math.sin(spawnAngle) * distance).toFloat()
            
            // 8개 가상 지점(0~7) 중 하나를 랜덤하게 목표로 설정
            val targetIndex = random.nextInt(8)
            
            return Enemy(gctx, enemyX, enemyY, player, targetIndex)
        }
    }
}
