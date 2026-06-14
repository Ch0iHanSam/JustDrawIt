package com.example.justdrawit.enemy

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.Drawable
import com.example.justdrawit.MainScene
import com.example.justdrawit.R
import com.example.justdrawit.base.Player
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class EnemyBullet(
    private val gctx: GameContext,
    var x: Float,
    var y: Float,
    private val dx: Float,
    private val dy: Float,
    private val player: Player
) : IGameObject {

    private val speed = 500f
    private val width = 40f
    private val height = 40f
    private val drawable: Drawable = gctx.res.getDrawable(R.drawable.ic_launcher_foreground).apply {
        colorFilter = PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
    }

    override fun update(gctx: GameContext) {
        x += dx * speed * gctx.frameTime
        y += dy * speed * gctx.frameTime
        
        // 맵 밖으로 나가면 제거 (간단히)
        val mapSize = 200f * 20f
        if (x < 0 || x > mapSize || y < 0 || y > mapSize) {
            (gctx.scene as? MainScene)?.world?.remove(this, MainScene.Layer.ENEMY)
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

        drawable.setBounds(
            (drawX - halfW).toInt(),
            (drawY - halfH).toInt(),
            (drawX + halfW).toInt(),
            (drawY + halfH).toInt()
        )
        drawable.draw(canvas)
    }

    fun getBoundingRect(): RectF {
        val halfW = width / 2f
        val halfH = height / 2f
        return RectF(x - halfW, y - halfH, x + halfW, y + halfH)
    }
}
