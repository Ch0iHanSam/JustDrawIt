package com.example.justdrawit.spell

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import com.example.justdrawit.MainScene
import com.example.justdrawit.R
import com.example.justdrawit.base.Player
import com.example.justdrawit.base.Speed
import kotlin.math.atan2

class MagicSprinkle(
    private val gctx: GameContext,
    var x: Float,
    var y: Float,
    private val dx: Float,
    private val dy: Float,
    private val player: Player,
    private val world: World<MainScene.Layer>
) : IGameObject, Spell {
    private val bitmap: Bitmap = gctx.res.getBitmap(R.drawable.lightning)
    private val srcRect = Rect(0, 0, 173, 100)
    private val paint = Paint().apply { isAntiAlias = true }
    
    private var width = 173f
    private var height = 100f
    private var speed = 0f
    private var lifeTime = 3.0f

    // 애니메이션 관련
    private var animTimer = 0f
    private val animFps = 12f
    private val frameCount = 8

    init {
        speed = Speed.getSpellSpeed(gctx)
        
        // 크기 약간 축소 (0.5배 정도)
        width *= 0.5f
        height *= 0.5f
    }

    override fun update(gctx: GameContext) {
        x += dx * speed * gctx.frameTime
        y += dy * speed * gctx.frameTime

        // 애니메이션 프레임 업데이트
        animTimer += gctx.frameTime
        val frameIndex = ((animTimer * animFps).toInt() % frameCount)
        srcRect.left = frameIndex * 173
        srcRect.right = srcRect.left + 173

        lifeTime -= gctx.frameTime
        if (lifeTime <= 0) {
            world.remove(this, MainScene.Layer.ARROW_MAGIC)
        }
    }

    override fun draw(canvas: Canvas) {
        val offsetX = getCameraOffsetX()
        val offsetY = getCameraOffsetY()

        val drawX = x - offsetX
        val drawY = y - offsetY

        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 180f

        val halfW = width / 2f
        val halfH = height / 2f
        
        val saveCount = canvas.save()
        canvas.translate(drawX, drawY)
        canvas.rotate(angle)
        
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
