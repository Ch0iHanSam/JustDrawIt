package com.example.justdrawit

import android.graphics.Canvas
import android.graphics.Color
import android.view.KeyEvent
import kr.ac.tukorea.ge.spgp2026.a2dg.activity.BaseGameActivity
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
    enum class Layer { BACKGROUND, PLAYER, HUD }
    override val world = World(Layer.entries.toTypedArray())
    private val player = Player(gctx)
    private val hud = DirectionHud(gctx, player)
    private val background = Background(gctx, player)
    private val minimap = Minimap(gctx, player)

    init {
        world.add(background, Layer.BACKGROUND)
        world.add(player, Layer.PLAYER)
        world.add(hud, Layer.HUD)
        world.add(minimap, Layer.HUD)

        // 캐릭터 주위에 랜덤하게 5마리의 적 생성
        val random = java.util.Random()
        val minDistance = 800f  // 최소 거리
        val maxDistance = 1500f // 최대 거리
        
        for (i in 1..5) {
            // 랜덤한 각도(0~360도) 선택
            val spawnAngle = random.nextDouble() * 2.0 * Math.PI
            // 최소~최대 사이의 랜덤한 거리 선택
            val distance = minDistance + random.nextFloat() * (maxDistance - minDistance)
            
            val enemyX = player.x + (Math.cos(spawnAngle) * distance).toFloat()
            val enemyY = player.y + (Math.sin(spawnAngle) * distance).toFloat()
            
            // 8개 가상 지점(0~7) 중 하나를 랜덤하게 목표로 설정
            val targetIndex = random.nextInt(8)
            
            val enemy = Enemy(gctx, enemyX, enemyY, player, targetIndex)
            world.add(enemy, Layer.PLAYER)
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        super.draw(canvas)
    }

    fun onKeyDown(keyCode: Int): Boolean = player.handleKeyDown(keyCode)
    fun onKeyUp(keyCode: Int): Boolean = player.handleKeyUp(keyCode)
}
