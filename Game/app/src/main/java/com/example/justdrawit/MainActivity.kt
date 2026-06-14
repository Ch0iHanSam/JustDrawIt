package com.example.justdrawit

import android.graphics.Canvas
import android.graphics.Color
import android.view.KeyEvent
import android.view.MotionEvent
import android.gesture.Gesture
import android.gesture.GesturePoint
import android.gesture.GestureStroke
import com.example.justdrawit.base.Background
import com.example.justdrawit.base.GestureManager
import com.example.justdrawit.base.Joystick
import com.example.justdrawit.base.MagicInput
import com.example.justdrawit.base.Minimap
import com.example.justdrawit.base.Player
import com.example.justdrawit.base.SpellHud
import com.example.justdrawit.enemy.Enemy
import com.example.justdrawit.spell.MagicArrow
import com.example.justdrawit.spell.MagicSprinkle
import kr.ac.tukorea.ge.spgp2026.a2dg.activity.BaseGameActivity
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import java.util.ArrayList

class MainActivity : BaseGameActivity() {
    override val drawsDebugInfo = false // FPS 출력을 끕니다 (게임 화면을 위해)

    override fun createRootScene(gctx: GameContext): Scene {
        return TitleScene(gctx)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 시스템에 의해 반복되는 이벤트는 무시합니다.
        if ((event?.repeatCount ?: 0) > 0) return true
        if (gameView.onKeyDown(keyCode, event)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (gameView.onKeyUp(keyCode, event)) return true
        return super.onKeyUp(keyCode, event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameView.onTouchEvent(event)) return true
        return super.onTouchEvent(event)
    }
}

class MainScene(gctx: GameContext) : Scene(gctx) {
    enum class Layer { BACKGROUND, FLOOR_MAGIC, ENEMY, PLAYER, ARROW_MAGIC, HUD }
    override val world = World(Layer.values())
    private val player = Player(gctx)
    private val test = Test(gctx, player)
    private val background = Background(gctx, player)
    private val minimap = Minimap(gctx, player)
    private val joystick = Joystick(gctx)
    private val gestureManager = GestureManager(gctx.view.context)
    private val magicInput = MagicInput(gctx, gestureManager, test) { handleGestureMagic(it) }
    private val spellHud = SpellHud(gctx)

    private var sprinkleTimer = 0f
    private var sprinkleCount = 30 // 초기에는 발사하지 않음
    private var sprinkleBaseAngle = 0.0

    // 터치 시간 및 제스처용 변수
    private var touchDownTime = 0L
    private var isTouchHolding = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private val currentPoints = ArrayList<GesturePoint>()

    init {
        world.add(background, Layer.BACKGROUND)
        world.add(player, Layer.PLAYER)
        world.add(test, Layer.HUD)
        world.add(minimap, Layer.HUD)
        world.add(joystick, Layer.HUD)
        world.add(magicInput, Layer.HUD)
        world.add(spellHud, Layer.HUD)

        // 캐릭터 주위에 랜덤하게 10마리의 적 생성
        for (i in 1..10) {
            val enemy = Enemy.randomSpawn(gctx, player)
            world.add(enemy, Layer.ENEMY)
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        super.draw(canvas)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = player.handleKeyDown(keyCode)
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean = player.handleKeyUp(keyCode)

    override fun update(gctx: GameContext) {
        player.setJoystickDirection(joystick.getDirection())
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

    private fun getPlayerScreenPos(): android.util.Pair<Float, Float> {
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
        return android.util.Pair(playerScreenX, playerScreenY)
    }

    private fun checkCollisions() {
        val objects = world.objectsAt(Layer.ENEMY)
        val enemies = objects.filterIsInstance<Enemy>()
        
        val floorMagicObjects = world.objectsAt(Layer.FLOOR_MAGIC)
        val arrowMagicObjects = world.objectsAt(Layer.ARROW_MAGIC)
        
        val allSpells = floorMagicObjects + arrowMagicObjects
        
        val magicArrows = allSpells.filterIsInstance<MagicArrow>()
        val magicSprinkles = allSpells.filterIsInstance<MagicSprinkle>()

        val deadEnemies = mutableSetOf<Enemy>()
        val spentSpells = mutableMapOf<kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject, Layer>()

        // MagicArrow 와 적 충돌
        for (spell in magicArrows) {
            val spellRect = spell.getBoundingRect()
            for (enemy in enemies) {
                if (enemy in deadEnemies) continue
                if (android.graphics.RectF.intersects(spellRect, enemy.getBoundingRect())) {
                    deadEnemies.add(enemy)
                    spentSpells[spell] = Layer.ARROW_MAGIC
                    break 
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
                    spentSpells[spell] = Layer.ARROW_MAGIC
                    break 
                }
            }
        }

        // 소멸된 객체 처리 및 새 적 생성
        for (enemy in deadEnemies) {
            world.remove(enemy, Layer.ENEMY)
            world.add(Enemy.randomSpawn(gctx, player), Layer.ENEMY)
        }
        for ((spell, layer) in spentSpells) {
            world.remove(spell, layer)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val joyHandled = joystick.onTouchEvent(event)
        val magicHandled = magicInput.onTouchEvent(event)
        if (joyHandled || magicHandled) return true

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDownTime = System.currentTimeMillis()
                isTouchHolding = true
                lastTouchX = event.x
                lastTouchY = event.y
                currentPoints.clear()
                currentPoints.add(GesturePoint(event.x, event.y, touchDownTime))
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                lastTouchX = event.x
                lastTouchY = event.y
                currentPoints.add(GesturePoint(event.x, event.y, System.currentTimeMillis()))
            }
            MotionEvent.ACTION_UP -> {
                if (isTouchHolding) {
                    val holdDuration = System.currentTimeMillis() - touchDownTime
                    
                    // 제스처 인식 시도
                    if (currentPoints.size > 10) { // 어느 정도 선이 그려졌을 때만
                        val gesture = Gesture()
                        gesture.addStroke(GestureStroke(currentPoints))
                        val gestureName = gestureManager.recognize(gesture)
                        
                        if (gestureName != null) {
                            handleGestureMagic(gestureName)
                            isTouchHolding = false
                            return true
                        }
                    }

                    if (holdDuration < 1000) {
                        // 1초 미만 터치 시 MagicArrow 발사 (제스처로 인식되지 않은 경우)
                        val pt = gctx.metrics.fromScreen(event.x, event.y)
                        val tx = pt.x
                        val ty = pt.y

                        test.addClickEffect(tx, ty)

                        val playerScreenPos = getPlayerScreenPos()
                        val diffX = tx - playerScreenPos.first
                        val diffY = ty - playerScreenPos.second

                        val arrow = MagicArrow(gctx, player.x, player.y, player.x + diffX, player.y + diffY, player, world)
                        world.add(arrow, Layer.ARROW_MAGIC)
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

    private fun handleGestureMagic(name: String) {
        // 제스처 이름에 따른 특수 효과 처리
        when (name) {
            "circle", "circleRight", "circleLeft", "circleRightSmall", "circleLeftSmall" -> {
                if (!spellHud.canCastSprinkle()) return
                
                // 원을 그리면 캐릭터 위치에서 MagicSprinkle 360도 발사
                for (i in 0 until 36) {
                    val angle = Math.toRadians(i * 10.0)
                    val dx = Math.cos(angle).toFloat()
                    val dy = Math.sin(angle).toFloat()
                    val sprinkle = MagicSprinkle(gctx, player.x, player.y, dx, dy, player, world)
                    world.add(sprinkle, Layer.ARROW_MAGIC)
                }
                spellHud.startSprinkleCooldown()
            }
            "arrowLeft" -> {
                if (!spellHud.canCastArrow()) return
                val arrow = MagicArrow(gctx, player.x, player.y, player.x - 100f, player.y, player, world)
                world.add(arrow, Layer.ARROW_MAGIC)
                spellHud.startArrowCooldown()
            }
            "arrowRight" -> {
                if (!spellHud.canCastArrow()) return
                val arrow = MagicArrow(gctx, player.x, player.y, player.x + 100f, player.y, player, world)
                world.add(arrow, Layer.ARROW_MAGIC)
                spellHud.startArrowCooldown()
            }
            "arrowUp" -> {
                if (!spellHud.canCastArrow()) return
                val arrow = MagicArrow(gctx, player.x, player.y, player.x, player.y - 100f, player, world)
                world.add(arrow, Layer.ARROW_MAGIC)
                spellHud.startArrowCooldown()
            }
            "arrowDown" -> {
                if (!spellHud.canCastArrow()) return
                val arrow = MagicArrow(gctx, player.x, player.y, player.x, player.y + 100f, player, world)
                world.add(arrow, Layer.ARROW_MAGIC)
                spellHud.startArrowCooldown()
            }
        }
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
                world.add(sprinkle, Layer.ARROW_MAGIC)
            }
            
            sprinkleCount++
            sprinkleTimer = 0f
        }
    }
}
