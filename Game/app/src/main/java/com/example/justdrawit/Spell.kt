package com.example.justdrawit

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class Spell(
    private val gctx: GameContext,
    var x: Float,
    var y: Float,
    targetWorldX: Float,
    targetWorldY: Float,
    private val player: Player,
    private val world: World<MainScene.Layer>
) : IGameObject {
    private val drawable: Drawable = gctx.res.getDrawable(R.drawable.ic_launcher_foreground)
    private var width = 80f
    private var height = 80f
    private var speed = 0f
    private var dx = 0f
    private var dy = 0f
    private var lifeTime = 3.0f

    init {
        // 속도 설정
        speed = Speed.getSpellSpeed(gctx)

        // 방향 벡터 계산 (월드 좌표 기준)
        val diffX = targetWorldX - x
        val diffY = targetWorldY - y
        val dist = kotlin.math.sqrt(diffX * diffX + diffY * diffY)
        if (dist > 0) {
            dx = diffX / dist
            dy = diffY / dist
        }
    }

    override fun update(gctx: GameContext) {
        x += dx * speed * gctx.frameTime
        y += dy * speed * gctx.frameTime

        lifeTime -= gctx.frameTime
        if (lifeTime <= 0) {
            world.remove(this, MainScene.Layer.PLAYER)
        }
    }

    override fun draw(canvas: Canvas) {
        val screenWidth = gctx.metrics.width
        val screenHeight = gctx.metrics.height
        val mapSize = 200f * 20f

        // 카메라 오프셋 계산 (Player/Enemy 와 동일 로직)
        var offsetX = player.x - screenWidth / 2
        var offsetY = player.y - screenHeight / 2
        offsetX = offsetX.coerceIn(0f, (mapSize - screenWidth).coerceAtLeast(0f))
        offsetY = offsetY.coerceIn(0f, (mapSize - screenHeight).coerceAtLeast(0f))

        val drawX = x - offsetX
        val drawY = y - offsetY

        val halfW = width / 2f
        val halfH = height / 2f
        
        drawable.setBounds(
            (drawX - halfW).toInt(),
            (drawY - halfH).toInt(),
            (drawX + halfW).toInt(),
            (drawY + halfH).toInt()
        )
        drawable.draw(canvas)
    }
}
