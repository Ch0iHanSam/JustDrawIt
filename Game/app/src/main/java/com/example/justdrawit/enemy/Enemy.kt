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
    private val targetIndex: Int, // 0~7 사이의 가상 지점 인덱스
    val isElite: Boolean = false,
    val phase: Int = 1
) : Sprite(gctx, R.drawable.densis_illustration) {
    private var speed = 0f
    private val targetOffsetDist = 80f // 캐릭터 중심으로부터의 거리
    
    var hp = 0f
    var maxHp = 0f
    private var canAttackPlayer = false
    private var shootTimer = 0f
    private val shootInterval = 2.0f

    private val paint = Paint().apply {
        val color = if (isElite) Color.parseColor("#9C27B0") else Color.BLACK
        colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    private val hpBgPaint = Paint().apply { color = Color.GRAY; style = Paint.Style.FILL }
    private val hpFgPaint = Paint().apply { color = Color.RED; style = Paint.Style.FILL }

    init {
        x = startX
        y = startY
        width = 180f
        height = 200f
        syncDstRect()

        // HP 설정: 기본 2, 페이즈마다 2배. 엘리트는 일반의 10배
        val baseHp = 2f * Math.pow(2.0, (phase - 1).toDouble()).toFloat()
        maxHp = if (isElite) baseHp * 10f else baseHp
        hp = maxHp

        // 속도 설정
        speed = if (isElite) Speed.getEliteEnemySpeed(gctx) else Speed.getEnemySpeed(gctx)
    }

    override fun update(gctx: GameContext) {
        val angle = Math.toRadians(targetIndex * 45.0 - 90.0)
        val targetWorldX = player.x + (Math.cos(angle) * targetOffsetDist).toFloat()
        val targetWorldY = player.y + (Math.sin(angle) * targetOffsetDist).toFloat()

        val diffX = targetWorldX - x
        val diffY = targetWorldY - y
        val distToTarget = kotlin.math.sqrt(diffX * diffX + diffY * diffY)

        if (distToTarget > 5f) { // 목표 지점으로 이동
            x += (diffX / distToTarget).toFloat() * speed * gctx.frameTime
            y += (diffY / distToTarget).toFloat() * speed * gctx.frameTime
        }

        // 플레이어와의 실제 거리 계산
        val pdx = player.x - x
        val pdy = player.y - y
        val distToPlayer = kotlin.math.sqrt((pdx * pdx + pdy * pdy).toDouble()).toFloat()

        // 플레이어와 충분히 가까워지면(거리 150 이내) 공격 가능 상태로 전환
        canAttackPlayer = (distToPlayer < 150f)

        // 엘리트 공격 로직
        if (isElite) {
            shootTimer += gctx.frameTime
            if (shootTimer >= shootInterval) {
                shootTimer = 0f
                shootAtPlayer()
            }
        }

        syncDstRect()
    }

    private fun shootAtPlayer() {
        val diffX = player.x - x
        val diffY = player.y - y
        val dist = kotlin.math.sqrt(diffX * diffX + diffY * diffY)
        if (dist > 0) {
            val bullet = EnemyBullet(gctx, x, y, (diffX / dist).toFloat(), (diffY / dist).toFloat(), player)
            (gctx.scene as? com.example.justdrawit.MainScene)?.world?.add(bullet, com.example.justdrawit.MainScene.Layer.ENEMY)
        }
    }

    override fun draw(canvas: Canvas) {
        val screenWidth = gctx.metrics.width
        val screenHeight = gctx.metrics.height
        val mapSize = 200f * 20f

        var offsetX = (player.x - screenWidth / 2).coerceIn(0f, (mapSize - screenWidth).coerceAtLeast(0f))
        var offsetY = (player.y - screenHeight / 2).coerceIn(0f, (mapSize - screenHeight).coerceAtLeast(0f))

        val drawX = x - offsetX
        val drawY = y - offsetY

        val halfW = width / 2f
        val halfH = height / 2f
        
        val drawRect = android.graphics.RectF(drawX - halfW, drawY - halfH, drawX + halfW, drawY + halfH)
        canvas.drawBitmap(bitmap, srcRect, drawRect, paint)

        // HP 바 그리기
        val barW = 100f
        val barH = 10f
        val barTop = drawY + halfH + 10f
        val barLeft = drawX - barW / 2
        val hpRatio = (hp / maxHp).coerceIn(0f, 1f)

        canvas.drawRect(barLeft, barTop, barLeft + barW, barTop + barH, hpBgPaint)
        canvas.drawRect(barLeft, barTop, barLeft + barW * hpRatio, barTop + barH, hpFgPaint)
    }

    fun canAttack(): Boolean = canAttackPlayer

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
        fun randomSpawn(gctx: GameContext, player: Player, phase: Int = 1, forceElite: Boolean = false): Enemy {
            val random = java.util.Random()
            val minDistance = gctx.metrics.height / 2f
            val maxDistance = minDistance + 500f
            
            val spawnAngle = random.nextDouble() * 2.0 * Math.PI
            val distance = minDistance + random.nextFloat() * (maxDistance - minDistance)
            
            val enemyX = player.x + (Math.cos(spawnAngle) * distance).toFloat()
            val enemyY = player.y + (Math.sin(spawnAngle) * distance).toFloat()
            
            val targetIndex = random.nextInt(8)
            val isElite = if (forceElite) true else false // 자동 엘리트 생성 로직 제거 (MainActivity에서 제어)
            
            return Enemy(gctx, enemyX, enemyY, player, targetIndex, isElite, phase)
        }
    }
}
