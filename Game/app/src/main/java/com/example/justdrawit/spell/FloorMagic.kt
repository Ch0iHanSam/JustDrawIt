package com.example.justdrawit.spell

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
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class FloorMagic(
    private val gctx: GameContext,
    private val x: Float,
    private val y: Float,
    private val player: Player,
    private val world: World<MainScene.Layer>
) : IGameObject, Spell {
    private val drawable: Drawable = gctx.res.getDrawable(R.drawable.ic_launcher_foreground).apply {
        // 보라색 필터 적용
        colorFilter = PorterDuffColorFilter(Color.parseColor("#9C27B0"), PorterDuff.Mode.SRC_IN)
    }
    
    // 캐릭터 크기(180x200)의 약 4배 (가로 기준 720f)
    private val width = 720f
    private val height = 720f
    private var lifeTime = 3.0f // 3초간 유지

    override fun update(gctx: GameContext) {
        lifeTime -= gctx.frameTime
        if (lifeTime <= 0) {
            world.remove(this, MainScene.Layer.FLOOR_MAGIC)
        }
    }

    override fun draw(canvas: Canvas) {
        val offsetX = getCameraOffsetX()
        val offsetY = getCameraOffsetY()
        
        val halfW = width / 2f
        val halfH = height / 2f
        
        // 깜빡이는 효과 (생존 시간에 따라 투명도 조절)
        drawable.alpha = (minOf(1.0f, lifeTime) * 200).toInt()
        
        drawable.setBounds(
            (x - halfW - offsetX).toInt(),
            (y - halfH - offsetY).toInt(),
            (x + halfW - offsetX).toInt(),
            (y + halfH - offsetY).toInt()
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
        // 히트박스를 비주얼 크기(720f)의 1/4인 180f로 설정
        val hitboxSize = width / 4f
        val halfW = hitboxSize / 2f
        val halfH = hitboxSize / 2f
        return RectF(x - halfW, y - halfH, x + halfW, y + halfH)
    }

    override fun getScreenRect(): RectF {
        val drawX = x - getCameraOffsetX()
        val drawY = y - getCameraOffsetY()
        // 히트박스 시각화를 위해 getBoundingRect와 동일한 비율(1/4) 적용
        val hitboxSize = width / 4f
        val halfW = hitboxSize / 2f
        val halfH = hitboxSize / 2f
        return RectF(drawX - halfW, drawY - halfH, drawX + halfW, drawY + halfH)
    }
}
