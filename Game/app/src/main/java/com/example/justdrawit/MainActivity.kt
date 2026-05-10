package com.example.justdrawit

import android.graphics.Canvas
import android.graphics.Color
import android.view.KeyEvent
import android.view.MotionEvent
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
    private val test = Test(gctx, player)
    private val background = Background(gctx, player)
    private val minimap = Minimap(gctx, player)

    init {
        world.add(background, Layer.BACKGROUND)
        world.add(player, Layer.PLAYER)
        world.add(test, Layer.HUD)
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val tx = event.x
            val ty = event.y

            // 테스트용 클릭 효과 추가
            test.addClickEffect(tx, ty)

            val screenWidth = gctx.metrics.width
            val screenHeight = gctx.metrics.height
            val mapSize = 200f * 20f

            // 캐릭터가 화면에 그려지는 실제 위치(Screen Position)를 역산
            val halfWinW = screenWidth / 2f
            val halfWinH = screenHeight / 2f

            val camMinX = halfWinW
            val camMaxX = mapSize - halfWinW
            val camMinY = halfWinH
            val camMaxY = mapSize - halfWinH

            val playerScreenX = when {
                mapSize <= screenWidth -> player.x
                player.x < camMinX -> player.x
                player.x > camMaxX -> player.x - (mapSize - screenWidth)
                else -> halfWinW
            }

            val playerScreenY = when {
                mapSize <= screenHeight -> player.y
                player.y < camMinY -> player.y
                player.y > camMaxY -> player.y - (mapSize - screenHeight)
                else -> halfWinH
            }

            // 클릭 지점(tx, ty)에서 캐릭터의 화면 위치(playerScreenX, playerScreenY)를 뺀 벡터가 발사 방향
            val diffX = tx - playerScreenX
            val diffY = ty - playerScreenY

            // 이 벡터를 현재 캐릭터의 월드 좌표에 더하면 월드 기준 목표 지점이 됨
            val targetWorldX = player.x + diffX
            val targetWorldY = player.y + diffY

            // 캐릭터의 현재 위치에서 마법 발사
            val spell = Spell(gctx, player.x, player.y, targetWorldX, targetWorldY, player, world)
            world.add(spell, Layer.PLAYER)
            return true
        }
        return super.onTouchEvent(event)
    }
}
