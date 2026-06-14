package com.example.justdrawit.spell

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.example.justdrawit.MainScene
import com.example.justdrawit.R
import com.example.justdrawit.base.Player
import com.example.justdrawit.base.Speed
import com.example.justdrawit.enemy.Enemy
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.math.atan2
import kotlin.math.sqrt

class MagicArrow(
    private val gctx: GameContext,
    var x: Float,
    var y: Float,
    private val player: Player,
    private val world: World<MainScene.Layer>
) : IGameObject, Spell {
    private val bitmap: Bitmap = gctx.res.getBitmap(R.drawable.fireball)
    private val srcRect = Rect(0, 0, 173, 100)
    private val paint = Paint().apply { isAntiAlias = true }
    
    private var width = 173f
    private var height = 100f
    private var speed = 0f
    private var dx = 0f
    private var dy = 0f
    private var lifeTime = 3.0f

    // 잔상을 위한 위치 기록
    private val history = mutableListOf<Pair<Float, Float>>()
    private val maxHistory = 15
    private val trailCount = 5
    private val trailInterval = 3 

    private var targetEnemy: Enemy? = null
    private val trackingRange = 1000f 

    init {
        // 크기가 너무 크면 약간 축소 (0.6배 정도)
        width *= 0.6f
        height *= 0.6f
        
        speed = Speed.getSpellSpeed(gctx) * 1.5f 
        findNearestEnemy()
        
        // 초기 발사 방향 설정 (적이 없으면 플레이어 앞쪽)
        if (targetEnemy == null) {
            dx = 0f
            dy = -1f 
        }
        updateDirection()
    }

    private fun findNearestEnemy() {
        val enemies = world.objectsAt(MainScene.Layer.ENEMY).filterIsInstance<Enemy>()
        var minDist = trackingRange
        
        targetEnemy = null
        for (enemy in enemies) {
            val dist = sqrt(((enemy.x - x) * (enemy.x - x) + (enemy.y - y) * (enemy.y - y)).toDouble()).toFloat()
            if (dist < minDist) {
                minDist = dist
                targetEnemy = enemy
            }
        }
    }

    private fun updateDirection() {
        val target = targetEnemy
        
        // 타겟이 유효한지 확인
        if (target != null) {
            val stillAlive = world.objectsAt(MainScene.Layer.ENEMY).contains(target)
            if (stillAlive) {
                val diffX = target.x - x
                val diffY = target.y - y
                val dist = sqrt((diffX * diffX + diffY * diffY).toDouble()).toFloat()
                
                if (dist > 0) {
                    // 유도 성능: 현재 방향에서 타겟 방향으로 부드럽게 회전하는 대신 즉시 보정
                    dx = diffX / dist
                    dy = diffY / dist
                }
                return
            } else {
                // 타겟이 죽었으면 새로운 타겟 찾기
                targetEnemy = null
                findNearestEnemy()
                // 새로 찾은 타겟이 있다면 다음 프레임에서 updateDirection이 처리함
            }
        }
        
        // 타겟이 없으면 기존 dx, dy 방향을 유지 (아무것도 안 함)
    }

    override fun update(gctx: GameContext) {
        // 매 프레임 타겟 추적
        updateDirection()

        // 이전 위치 기록 (잔상용)
        history.add(0, Pair(x, y))
        if (history.size > maxHistory) {
            history.removeAt(history.size - 1)
        }

        x += dx * speed * gctx.frameTime
        y += dy * speed * gctx.frameTime

        lifeTime -= gctx.frameTime
        if (lifeTime <= 0) {
            world.remove(this, MainScene.Layer.ARROW_MAGIC)
        }
    }

    override fun draw(canvas: Canvas) {
        val offsetX = getCameraOffsetX()
        val offsetY = getCameraOffsetY()

        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 180f

        // 1. 잔상 그리기 (점점 투명해짐)
        val alphas = intArrayOf(180, 140, 100, 60, 30) 
        
        for (i in 0 until trailCount) {
            val historyIdx = (i + 1) * trailInterval
            if (history.size > historyIdx) {
                val (hx, hy) = history[historyIdx]
                drawAt(canvas, hx - offsetX, hy - offsetY, alphas[i], angle)
            }
        }

        // 2. 본체 그리기 (100% 투명도)
        drawAt(canvas, x - offsetX, y - offsetY, 255, angle)
    }

    private fun drawAt(canvas: Canvas, screenX: Float, screenY: Float, alpha: Int, angle: Float) {
        val halfW = width / 2f
        val halfH = height / 2f
        
        val saveCount = canvas.save()
        canvas.translate(screenX, screenY)
        canvas.rotate(angle)
        
        paint.alpha = alpha
        
        val dstRect = RectF(-halfW, -halfH, halfW, halfH)
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
        
        canvas.restoreToCount(saveCount)
    }

    private fun getCameraOffsetX(): Float {
        val screenWidth = gctx.metrics.width
        val mapSize = 200f * 20f
        return (player.x - screenWidth / 2).coerceIn(0f, (mapSize - screenWidth).coerceAtLeast(0f))
    }

    private fun getCameraOffsetY(): Float {
        val screenHeight = gctx.metrics.height
        val mapSize = 200f * 20f
        return (player.y - screenHeight / 2).coerceIn(0f, (mapSize - screenHeight).coerceAtLeast(0f))
    }

    fun getBoundingRect(): RectF {
        val halfW = width / 2f
        val halfH = height / 2f
        return RectF(x - halfW, y - halfH, x + halfW, y + halfH)
    }

    override fun getScreenRect(): RectF {
        val drawX = x - getCameraOffsetX()
        val drawY = y - getCameraOffsetY()
        val halfW = width / 2f
        val halfH = height / 2f
        return RectF(drawX - halfW, drawY - halfH, drawX + halfW, drawY + halfH)
    }
}
