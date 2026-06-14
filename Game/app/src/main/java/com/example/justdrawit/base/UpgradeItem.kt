package com.example.justdrawit.base

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.Drawable
import com.example.justdrawit.R
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class UpgradeItem(
    private val gctx: GameContext,
    var x: Float,
    var y: Float,
    private val player: Player
) : IGameObject {
    
    private val drawable: Drawable = gctx.res.getDrawable(R.drawable.ic_launcher_foreground).apply {
        colorFilter = PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN)
    }
    
    private val width = 80f
    private val height = 80f

    override fun update(gctx: GameContext) {}

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
