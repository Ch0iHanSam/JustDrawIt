package com.example.justdrawit

import android.graphics.Canvas
import android.graphics.Color
import android.view.KeyEvent
import android.view.MotionEvent
import com.example.justdrawit.base.Background
import com.example.justdrawit.base.Minimap
import com.example.justdrawit.base.Player
import com.example.justdrawit.enemy.Enemy
import com.example.justdrawit.spell.MagicArrow
import com.example.justdrawit.spell.MagicSprinkle
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

    private var sprinkleTimer = 0f
    private var sprinkleCount = 30 // 초기에는 발사하지 않음
    private var sprinkleBaseAngle = 0.0

    // 터치 시간 측정용 변수
    private var touchDownTime = 0L
    private var isTouchHolding = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    init {
        world.add(background, Layer.BACKGROUND)
        world.add(player, Layer.PLAYER)
        world.add(test, Layer.HUD)
        world.add(minimap, Layer.HUD)

        // 캐릭터 주위에 랜덤하게 10마리의 적 생성
        for (i in 1..10) {
            val enemy = Enemy.randomSpawn(gctx, player)
            world.add(enemy, Layer.PLAYER)
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        super.draw(canvas)
    }

    fun onKeyDown(keyCode: Int): Boolean = player.handleKeyDown(keyCode)
    fun onKeyUp(keyCode: Int): Boolean = player.handleKeyUp(keyCode)

    override fun update(gctx: GameContext) {
        super.update(gctx)
        checkCollisions()
        updateSprinkleSequence(gctx)
        checkTouchHold(gctx)
    }

    private fun checkTouchHold(gctx: GameContext) {
        if (isTouchHolding) {
            val holdDuration = System.currentTimeMillis() - touchDownTime
            if (holdDuration >= 1000) { // 1초 이상 누르고 있으면
                val pt = gctx.metrics.fromScreen(lastTouchX, lastTouchY)
                val tx = pt.x
                val ty = pt.y

                val playerScreenPos = getPlayerScreenPos()
                val diffX = tx - playerScreenPos.first
                val diffY = ty - playerScreenPos.second
                val angleRad = Math.atan2(diffY.toDouble(), diffX.toDouble())

                // MagicSprinkle 발사 시작
                startSprinkleSequence(angleRad)
                isTouchHolding = false // 한 번 발사 후 홀딩 해제
            }
        }
    }

    private fun getPlayerScreenPos(): Pair<Float, Float> {
        val screenWidth = gctx.metrics.width
        val screenHeight = gctx.metrics.height
        val mapSize = 200f * 20f

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
        return Pair(playerScreenX, playerScreenY)
    }

    private fun checkCollisions() {
        val objects = world.objectsAt(Layer.PLAYER)
        val magicArrows = objects.filterIsInstance<MagicArrow>()
        val magicSprinkles = objects.filterIsInstance<MagicSprinkle>()
        val enemies = objects.filterIsInstance<Enemy>()

        val deadEnemies = mutableSetOf<Enemy>()
        val spentSpells = mutableSetOf<kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject>()

        // MagicArrow 와 적 충돌
        for (spell in magicArrows) {
            val spellRect = spell.getBoundingRect()
            for (enemy in enemies) {
                if (enemy in deadEnemies) continue
                if (android.graphics.RectF.intersects(spellRect, enemy.getBoundingRect())) {
                    deadEnemies.add(enemy)
                    spentSpells.add(spell)
                    break // 이 화살은 소멸
                }
            }
        }

        // MagicSprinkle 과 적 충돌
        for (spell in magicSprinkles) {
            val spellRect = spell.getBoundingRect()
            for (enemy in enemies) {
                if (enemy in deadEnemies) continue
                if (android.graphics.RectF.intersects(spellRect, enemy.getBoundingRect())) {
                    deadEnemies.add(enemy)
                    spentSpells.add(spell)
                    break // 이 스프링클은 소멸
                }
            }
        }

        // 소멸된 객체 처리 및 새 적 생성
        for (enemy in deadEnemies) {
            world.remove(enemy, Layer.PLAYER)
            world.add(Enemy.randomSpawn(gctx, player), Layer.PLAYER)
        }
        for (spell in spentSpells) {
            world.remove(spell, Layer.PLAYER)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDownTime = System.currentTimeMillis()
                isTouchHolding = true
                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_UP -> {
                if (isTouchHolding) {
                    val holdDuration = System.currentTimeMillis() - touchDownTime
                    if (holdDuration < 1000) {
                        // 1초 미만 터치 시 MagicArrow 발사
                        val pt = gctx.metrics.fromScreen(event.x, event.y)
                        val tx = pt.x
                        val ty = pt.y

                        test.addClickEffect(tx, ty)

                        val playerScreenPos = getPlayerScreenPos()
                        val diffX = tx - playerScreenPos.first
                        val diffY = ty - playerScreenPos.second

                        val arrow = MagicArrow(gctx, player.x, player.y, player.x + diffX, player.y + diffY, player, world)
                        world.add(arrow, Layer.PLAYER)
                    }
                    isTouchHolding = false
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isTouchHolding = false
            }
        }
        return super.onTouchEvent(event)
    }

    private fun startSprinkleSequence(baseAngle: Double) {
        sprinkleTimer = 0f
        sprinkleCount = 0
        sprinkleBaseAngle = baseAngle
    }

    private fun updateSprinkleSequence(gctx: GameContext) {
        if (sprinkleCount >= 5) return // 0.2초마다 5번 = 1초 동안 총 30발

        sprinkleTimer += gctx.frameTime
        // 0.2초마다 발사
        if (sprinkleTimer >= 0.2f) {
            // 한 번에 6발 발사 (위아래 3개씩, 5도 간격, 총 30도 범위)
            for (i in -3..2) { // -3, -2, -1, 0, 1, 2 (총 6개)
                val degOffset = (i + 0.5) * 5.0 // 중심 기준 대칭을 위해 0.5 조정
                val currentAngle = sprinkleBaseAngle + Math.toRadians(degOffset)
                
                val dx = Math.cos(currentAngle).toFloat()
                val dy = Math.sin(currentAngle).toFloat()
                
                val sprinkle = MagicSprinkle(gctx, player.x, player.y, dx, dy, player, world)
                world.add(sprinkle, Layer.PLAYER)
            }
            
            sprinkleCount++
            sprinkleTimer = 0f
        }
    }
}
