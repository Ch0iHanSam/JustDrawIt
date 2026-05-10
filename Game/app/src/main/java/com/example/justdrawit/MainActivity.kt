package com.example.justdrawit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.KeyEvent
import kr.ac.tukorea.ge.spgp2026.a2dg.activity.BaseGameActivity
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class MainActivity : BaseGameActivity() {
    override val drawsDebugInfo = true // FPS 출력을 활성화합니다.
    private var mainScene: MainScene? = null

    override fun createRootScene(gctx: GameContext): Scene {
        return MainScene(gctx).also { mainScene = it }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 시스템에 의해 반복되는 이벤트는 무시합니다.
        if ((event?.repeatCount ?: 0) > 0) return true
        if (mainScene?.onKeyDown(keyCode) == true) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (mainScene?.onKeyUp(keyCode) == true) return true
        return super.onKeyUp(keyCode, event)
    }
}

class MainScene(gctx: GameContext) : Scene(gctx) {
    enum class Layer { PLAYER }
    override val world = World(Layer.entries.toTypedArray())
    private val player = Player(gctx)

    init {
        world.add(player, Layer.PLAYER)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        super.draw(canvas)
    }

    fun onKeyDown(keyCode: Int): Boolean = player.handleKeyDown(keyCode)
    fun onKeyUp(keyCode: Int): Boolean = player.handleKeyUp(keyCode)
}

class Player(gctx: GameContext) : IGameObject {
    private val drawable: Drawable = gctx.res.getDrawable(R.drawable.ic_launcher_foreground).apply {
        colorFilter = PorterDuffColorFilter(Color.parseColor("#FFC0CB"), PorterDuff.Mode.SRC_IN)
    }
    private var x = gctx.metrics.width / 2
    private var y = gctx.metrics.height / 2
    private val size = 300f
    private val speed = 800f

    private var dx = 0f
    private var dy = 0f

    override fun update(gctx: GameContext) {
        var moveX = dx
        var moveY = dy
        
        if (dx != 0f && dy != 0f) {
            val mag = kotlin.math.sqrt(dx * dx + dy * dy)
            moveX /= mag
            moveY /= mag
        }

        x += moveX * speed * gctx.frameTime
        y += moveY * speed * gctx.frameTime

        if (moveX != 0f || moveY != 0f) {
            Log.d("JDI_Move", "Player Position: (%.2f, %.2f)".format(x, y))
        }

        x = x.coerceIn(0f, gctx.metrics.width)
        y = y.coerceIn(0f, gctx.metrics.height)
    }

    override fun draw(canvas: Canvas) {
        val left = (x - size / 2).toInt()
        val top = (y - size / 2).toInt()
        val right = (x + size / 2).toInt()
        val bottom = (y + size / 2).toInt()
        drawable.setBounds(left, top, right, bottom)
        drawable.draw(canvas)
    }

    fun handleKeyDown(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> dx = -1f
            KeyEvent.KEYCODE_DPAD_RIGHT -> dx = 1f
            KeyEvent.KEYCODE_DPAD_UP -> dy = -1f
            KeyEvent.KEYCODE_DPAD_DOWN -> dy = 1f
            else -> return false
        }
        return true
    }

    fun handleKeyUp(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> if (dx < 0) dx = 0f
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (dx > 0) dx = 0f
            KeyEvent.KEYCODE_DPAD_UP -> if (dy < 0) dy = 0f
            KeyEvent.KEYCODE_DPAD_DOWN -> if (dy > 0) dy = 0f
            else -> return false
        }
        return true
    }
}
