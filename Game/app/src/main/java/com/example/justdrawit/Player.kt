package com.example.justdrawit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.KeyEvent
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.Sprite
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class Player(private val gctx: GameContext) : Sprite(gctx, R.drawable.densis_illustration) {
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
        // 위치 초기화: 전체 맵(20x20 타일, 각 200f)의 중앙
        val mapSize = 200f * 20f
        x = mapSize / 2
        y = mapSize / 2
        
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

        // 월드 경계 제한 (20x20 타일, 캐릭터 크기 고려)
        val mapSize = 200f * 20f
        val halfW = width / 2f
        val halfH = height / 2f
        x = x.coerceIn(halfW, mapSize - halfW)
        y = y.coerceIn(halfH, mapSize - halfH)
        
        // syncDstRect() 는 이제 실제 그리기에 사용되지 않지만 
        // 충돌 체크 등을 위해 로직 좌표를 업데이트 함.
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
        // 실제 화면에 그려질 좌표 계산
        val screenWidth = gctx.metrics.width
        val screenHeight = gctx.metrics.height
        val mapSize = 200f * 20f

        // 캐릭터가 화면 중앙에 있을 때의 월드 좌표 기준 스크롤 범위
        val halfWinW = screenWidth / 2f
        val halfWinH = screenHeight / 2f

        // 카메라(스크롤)가 고정되는 경계값
        val camMinX = halfWinW
        val camMaxX = mapSize - halfWinW
        val camMinY = halfWinH
        val camMaxY = mapSize - halfWinH

        val drawX = when {
            mapSize <= screenWidth -> x // 맵이 화면보다 작으면 그냥 x
            x < camMinX -> x
            x > camMaxX -> x - (mapSize - screenWidth)
            else -> halfWinW
        }

        val drawY = when {
            mapSize <= screenHeight -> y
            y < camMinY -> y
            y > camMaxY -> y - (mapSize - screenHeight)
            else -> halfWinH
        }

        val halfW = width / 2f
        val halfH = height / 2f
        
        val drawRect = android.graphics.RectF(
            drawX - halfW, drawY - halfH,
            drawX + halfW, drawY + halfH
        )
        canvas.drawBitmap(bitmap, srcRect, drawRect, paint)
    }

    fun handleKeyDown(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_A -> {
                leftPressed = true
                rightPressed = false
            }
            KeyEvent.KEYCODE_D -> {
                rightPressed = true
                leftPressed = false
            }
            KeyEvent.KEYCODE_W -> {
                upPressed = true
                downPressed = false
            }
            KeyEvent.KEYCODE_S -> {
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
            KeyEvent.KEYCODE_A,
            KeyEvent.KEYCODE_D,
            KeyEvent.KEYCODE_W,
            KeyEvent.KEYCODE_S -> true
            else -> false
        }
    }
}
