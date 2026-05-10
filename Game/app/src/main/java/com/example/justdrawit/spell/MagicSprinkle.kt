package com.example.justdrawit.spell

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.Drawable
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import com.example.justdrawit.MainScene
import com.example.justdrawit.R
import com.example.justdrawit.base.Player
import com.example.justdrawit.base.Speed

class MagicSprinkle(
    private val gctx: GameContext,
    var x: Float,
    var y: Float,
    private val dx: Float,
    private val dy: Float,
    private val player: Player,
    private val world: World<MainScene.Layer>
) : IGameObject, Spell {
    private val drawable: Drawable = gctx.res.getDrawable(R.drawable.ic_launcher_foreground).apply {
        // 형광 초록색 (Neon Green) 필터 적용
        colorFilter = PorterDuffColorFilter(Color.parseColor("#39FF14"), PorterDuff.Mode.SRC_IN)
    }
    private var width = 80f
    private var height = 80f
    private var speed = 0f
    private var lifeTime = 3.0f

    init {
        speed = Speed.getSpellSpeed(gctx)
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

    fun getBoundingRect(): RectF {
        val halfW = width / 2f
        val halfH = height / 2f
        return RectF(x - halfW, y - halfH, x + halfW, y + halfH)
    }

    override fun getScreenRect(): RectF {
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
        return RectF(drawX - halfW, drawY - halfH, drawX + halfW, drawY + halfH)
    }
}
