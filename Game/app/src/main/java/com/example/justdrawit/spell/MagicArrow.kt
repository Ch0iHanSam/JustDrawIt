package com.example.justdrawit.spell

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import android.graphics.RectF
import com.example.justdrawit.MainScene
import com.example.justdrawit.R
import com.example.justdrawit.base.Player
import com.example.justdrawit.base.Speed
import com.example.justdrawit.enemy.Enemy
import kotlin.math.sqrt

class MagicArrow(
    private val gctx: GameContext,
    var x: Float,
    var y: Float,
    private val player: Player,
    private val world: World<MainScene.Layer>
) : IGameObject, Spell {
    private val drawable: Drawable = gctx.res.getDrawable(R.drawable.ic_launcher_foreground).apply {
        // 하늘색 (Light Blue) 필터 적용
        colorFilter = PorterDuffColorFilter(Color.parseColor("#87CEEB"), PorterDuff.Mode.SRC_IN)
    }
    private var width = 80f
    private var height = 80f
    private var speed = 0f
    private var dx = 0f
    private var dy = 0f
    private var lifeTime = 3.0f

    // 잔상을 위한 위치 기록
    private val history = mutableListOf<Pair<Float, Float>>()
    private val maxHistory = 15 // 잔상 개수 5개 * 간격 3 = 15프레임 기록
    private val trailCount = 5
    private val trailInterval = 3 // 잔상 간의 프레임 간격 (거리를 늘리기 위함)

    // 랜덤 목표 색상 (그라데이션용)
    private val targetColor = Color.rgb(
        (0..255).random(),
        (0..255).random(),
        (0..255).random()
    )
    private val baseColor = Color.parseColor("#87CEEB") // 기본 하늘색
    private var targetEnemy: Enemy? = null
    private val trackingRange = 1000f // 새로운 타겟을 찾을 최대 거리

    init {
        speed = Speed.getSpellSpeed(gctx) * 1.5f // 유도탄이므로 속도를 약간 상향
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

        // 1. 잔상 그리기 (그라데이션 + 투명도)
        val alphas = intArrayOf(204, 153, 102, 77, 51) // 점점 투명해짐 (0.8, 0.6, 0.4, 0.3, 0.2)
        
        for (i in 0 until trailCount) {
            val historyIdx = (i + 1) * trailInterval
            if (history.size > historyIdx) {
                val (hx, hy) = history[historyIdx]
                
                // 색상 보간 (Linear Interpolation)
                // i=0(첫 잔상)일 때 baseColor에 가깝고, i=4(마지막 잔상)일 때 targetColor에 가깝게
                val ratio = (i + 1).toFloat() / trailCount.toFloat()
                val interpolatedColor = interpolateColor(baseColor, targetColor, ratio)
                
                drawAt(canvas, hx - offsetX, hy - offsetY, alphas[i], interpolatedColor)
            }
        }

        // 2. 본체 그리기 (기본 하늘색, 100% 투명도)
        drawAt(canvas, x - offsetX, y - offsetY, 255, baseColor)
    }

    private fun interpolateColor(start: Int, end: Int, ratio: Float): Int {
        val r = (Color.red(start) + (Color.red(end) - Color.red(start)) * ratio).toInt()
        val g = (Color.green(start) + (Color.green(end) - Color.green(start)) * ratio).toInt()
        val b = (Color.blue(start) + (Color.blue(end) - Color.blue(start)) * ratio).toInt()
        return Color.rgb(r, g, b)
    }

    private fun drawAt(canvas: Canvas, screenX: Float, screenY: Float, alpha: Int, color: Int) {
        val halfW = width / 2f
        val halfH = height / 2f
        drawable.alpha = alpha
        drawable.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        drawable.setBounds(
            (screenX - halfW).toInt(),
            (screenY - halfH).toInt(),
            (screenX + halfW).toInt(),
            (screenY + halfH).toInt()
        )
        drawable.draw(canvas)
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
