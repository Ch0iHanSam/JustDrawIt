package com.example.justdrawit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.KeyEvent
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class Player(gctx: GameContext) : Sprite(gctx, R.drawable.densis_illustration) {
    private var speed = 0f

    private var leftPressed = false
    private var rightPressed = false
    private var upPressed = false
    private var downPressed = false

    private var isColorChanging = false
    private var frameCount = 0
    private val paint = Paint() // 기본적으로 필터 없음

    fun isLeftPressed() = leftPressed
    fun isRightPressed() = rightPressed
    fun isUpPressed() = upPressed
    fun isDownPressed() = downPressed

    init {
        // 위치 초기화
        x = gctx.metrics.width / 2
        y = gctx.metrics.height / 2
        
        // 크기 설정 (가상 좌표계 기준 180x200)
        width = 180f
        height = 200f
        syncDstRect()

        // 화면 가로를 5초에 횡단하는 속도 계산 (화면 너비 / 5초)
        speed = gctx.metrics.width / 5.0f
    }

    override fun update(gctx: GameContext) {
        var dx = (if (rightPressed) 1f else 0f) - (if (leftPressed) 1f else 0f)
        var dy = (if (downPressed) 1f else 0f) - (if (upPressed) 1f else 0f)
        
        if (dx != 0f && dy != 0f) {
            val mag = kotlin.math.sqrt(dx * dx + dy * dy)
            dx /= mag
            dy /= mag
        }

        x += dx * speed * gctx.frameTime
        y += dy * speed * gctx.frameTime

        x = x.coerceIn(0f, gctx.metrics.width)
        y = y.coerceIn(0f, gctx.metrics.height)
        
        // 위치가 변했으므로 그리기 영역 업데이트
        syncDstRect()

        if (isColorChanging) {
            frameCount++
            if (frameCount >= 3) {
                val r = (0..255).random()
                val g = (0..255).random()
                val b = (0..255).random()
                paint.colorFilter = PorterDuffColorFilter(Color.rgb(r, g, b), PorterDuff.Mode.SRC_IN)
                frameCount = 0
            }
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
    }

    fun handleKeyDown(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                leftPressed = true
                rightPressed = false
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                rightPressed = true
                leftPressed = false
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                upPressed = true
                downPressed = false
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                downPressed = true
                upPressed = false
            }
            KeyEvent.KEYCODE_C -> {
                isColorChanging = !isColorChanging
                if (!isColorChanging) {
                    paint.colorFilter = null // 원래 이미지 색상으로 복구
                }
            }
            KeyEvent.KEYCODE_X -> {
                leftPressed = false
                rightPressed = false
                upPressed = false
                downPressed = false
            }
            else -> return false
        }
        return true
    }

    fun handleKeyUp(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> true
            else -> false
        }
    }
}
