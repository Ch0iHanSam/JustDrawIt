package com.example.justdrawit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.justdrawit.base.Player
import com.example.justdrawit.enemy.Enemy
import com.example.justdrawit.spell.Spell
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class Test(private val gctx: GameContext, private val player: Player) : IGameObject {
    // 클릭 효과 활성화 여부
    var isClickEffectEnabled = true
    
    // DirectionHud 활성화 여부
    var isDirectionHudEnabled = true
    
    // 히트박스 출력 여부
    var drawHitboxEnemy = true
    var drawHitboxSpell = true

    // DirectionHud 관련 변수
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

    // 클릭 효과 관련 클래스
    private class ClickEffect(val x: Float, val y: Float) {
        var lifeTime = 0.5f
    }
    private val clickEffects = mutableListOf<ClickEffect>()
    private val yellowPaint = Paint().apply {
        color = Color.YELLOW
        alpha = 128 // 반투명 (0~255)
        style = Paint.Style.FILL
    }
    private val hitboxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    fun addClickEffect(x: Float, y: Float) {
        if (!isClickEffectEnabled) return
        clickEffects.add(ClickEffect(x, y))
    }

    override fun update(gctx: GameContext) {
        // 클릭 효과 시간 업데이트 및 만료된 항목 제거
        val it = clickEffects.iterator()
        while (it.hasNext()) {
            val effect = it.next()
            effect.lifeTime -= gctx.frameTime
            if (effect.lifeTime <= 0) {
                it.remove()
            }
        }
    }

    override fun draw(canvas: Canvas) {
        // 1. 클릭 효과 그리기 (노란색 반투명 원)
        if (isClickEffectEnabled) {
            for (effect in clickEffects) {
                canvas.drawCircle(effect.x, effect.y, 50f, yellowPaint)
            }
        }

        // 2. 히트박스 그리기
        drawHitboxes(canvas)

        // 3. DirectionHud 내용 그리기
        if (isDirectionHudEnabled) {
            drawDirectionHud(canvas)
        }
    }

    private fun drawHitboxes(canvas: Canvas) {
        val scene = gctx.scene as? MainScene ?: return
        val world = scene.world

        if (drawHitboxEnemy) {
            hitboxPaint.color = Color.RED
            world.objectsAt(MainScene.Layer.PLAYER).forEach { obj ->
                if (obj is Enemy) {
                    obj.getScreenRect().let { canvas.drawRect(it, hitboxPaint) }
                }
            }
        }

        if (drawHitboxSpell) {
            hitboxPaint.color = Color.BLUE
            world.objectsAt(MainScene.Layer.PLAYER).forEach { obj ->
                if (obj is Spell) {
                    obj.getScreenRect()?.let { canvas.drawRect(it, hitboxPaint) }
                }
            }
        }
    }

    private fun drawDirectionHud(canvas: Canvas) {
        val screenHeight = gctx.metrics.height
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
