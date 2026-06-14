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
import com.example.justdrawit.base.ScoreHud
import com.example.justdrawit.base.SpellHud
import com.example.justdrawit.base.StatusHud
import com.example.justdrawit.base.UpgradeItem
import com.example.justdrawit.enemy.Enemy
import com.example.justdrawit.enemy.EnemyBullet
import com.example.justdrawit.spell.FloorMagic
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
    private val statusHud = StatusHud(player)
    private val scoreHud = ScoreHud(gctx, player) { currentPhase }

    private var sprinkleTimer = 0f
    private var sprinkleCount = 30 // 초기에는 발사하지 않음
    private var sprinkleBaseAngle = 0.0

    private var phaseTimer = 0f
    private var currentPhase = 1
    private var eliteSpawnCountInPhase = 0
    private var isElitePresent = false
    private var floorMagicDamageTimer = 0f
    private var playerCollisionDamageTimer = 0f

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
        world.add(statusHud, Layer.HUD)
        world.add(scoreHud, Layer.HUD)

        for (i in 1..10) {
            val enemy = Enemy.randomSpawn(gctx, player, currentPhase, forceElite = false)
            world.add(enemy, Layer.ENEMY)
        }
        
        spawnUpgradeItems()
    }

    private fun spawnUpgradeItems() {
        val random = java.util.Random()
        val mapSize = 200f * 20f
        for (i in 1..3) {
            val itemX = random.nextFloat() * mapSize
            val itemY = random.nextFloat() * mapSize
            world.add(UpgradeItem(gctx, itemX, itemY, player), Layer.BACKGROUND)
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        super.draw(canvas)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = player.handleKeyDown(keyCode)
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean = player.handleKeyUp(keyCode)

    override fun update(gctx: GameContext) {
        if (player.hp <= 0) {
            val score = (player.normalKills + player.eliteKills * 10 + player.playTimeSeconds.toInt() * 5)
            GameOverScene(gctx, score).push()
            return
        }

        phaseTimer += gctx.frameTime
        if (phaseTimer >= 30f) {
            phaseTimer = 0f
            currentPhase++
            eliteSpawnCountInPhase = 0 // 페이즈 변경 시 카운트 리셋
            spawnUpgradeItems()
        }

        // 엘리트 몬스터 스폰 로직 (3페이즈 이상, 페이즈당 2마리 제한, 현재 부재 시)
        if (currentPhase >= 3 && eliteSpawnCountInPhase < 2 && !isElitePresent) {
            val elite = Enemy.randomSpawn(gctx, player, currentPhase, forceElite = true)
            world.add(elite, Layer.ENEMY)
            eliteSpawnCountInPhase++
            isElitePresent = true
        }

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
        val enemyObjects = world.objectsAt(Layer.ENEMY)
        val enemies = enemyObjects.filterIsInstance<Enemy>()
        val enemyBullets = enemyObjects.filterIsInstance<EnemyBullet>()
        
        val backgroundObjects = world.objectsAt(Layer.BACKGROUND)
        val upgradeItems = backgroundObjects.filterIsInstance<UpgradeItem>()
        
        val floorMagicObjects = world.objectsAt(Layer.FLOOR_MAGIC)
        val arrowMagicObjects = world.objectsAt(Layer.ARROW_MAGIC)
        
        val deadEnemies = mutableSetOf<Enemy>()
        val spentSpells = mutableMapOf<kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject, Layer>()
        val collectedItems = mutableSetOf<UpgradeItem>()
        val spentBullets = mutableSetOf<EnemyBullet>()

        // 0. 플레이어와 적 충돌 (HP 감소 - 0.2초마다)
        playerCollisionDamageTimer += gctx.frameTime
        val applyPlayerDamage = if (playerCollisionDamageTimer >= 0.2f) {
            playerCollisionDamageTimer = 0f
            true
        } else false

        val playerRect = player.getBoundingRect()
        val collisionDamage = 5f + (currentPhase - 1) // 기본 5, 페이즈당 +1
        
        for (enemy in enemies) {
            if (android.graphics.RectF.intersects(playerRect, enemy.getBoundingRect())) {
                if (applyPlayerDamage) {
                    player.hp -= collisionDamage
                }
            }
        }
        
        // 0.1 플레이어와 적 총알 충돌
        for (bullet in enemyBullets) {
            if (android.graphics.RectF.intersects(playerRect, bullet.getBoundingRect())) {
                player.hp -= (player.maxHp / 5f) // 1/5 데미지
                spentBullets.add(bullet)
            }
        }

        // 0.2 플레이어와 아이템 충돌
        for (item in upgradeItems) {
            if (android.graphics.RectF.intersects(playerRect, item.getBoundingRect())) {
                player.upgradeCount++
                player.damage += 1f
                player.updateMaxMp() // 최대 마나량 업데이트 및 회복
                collectedItems.add(item)
            }
        }

        // 1. FloorMagic (장판) 충돌 체크 (0.2초마다 데미지)
        floorMagicDamageTimer += gctx.frameTime
        val applyFloorDamage = if (floorMagicDamageTimer >= 0.2f) {
            floorMagicDamageTimer = 0f
            true
        } else false

        for (floor in floorMagicObjects.filterIsInstance<FloorMagic>()) {
            val floorRect = floor.getBoundingRect()
            for (enemy in enemies) {
                if (enemy in deadEnemies) continue
                if (android.graphics.RectF.intersects(floorRect, enemy.getBoundingRect())) {
                    if (applyFloorDamage) {
                        enemy.hp -= player.damage * 0.5f // 1/2 데미지
                        if (enemy.hp <= 0) deadEnemies.add(enemy)
                    }
                }
            }
        }

        // 2. MagicArrow 와 적 충돌
        val magicArrows = arrowMagicObjects.filterIsInstance<MagicArrow>()
        for (spell in magicArrows) {
            val spellRect = spell.getBoundingRect()
            for (enemy in enemies) {
                if (enemy in deadEnemies) continue
                if (android.graphics.RectF.intersects(spellRect, enemy.getBoundingRect())) {
                    enemy.hp -= player.damage
                    if (enemy.hp <= 0) deadEnemies.add(enemy)
                    spentSpells[spell] = Layer.ARROW_MAGIC
                    gctx.res.sound.playEffect(R.raw.boom)
                    break 
                }
            }
        }

        // 3. MagicSprinkle 과 적 충돌
        val magicSprinkles = arrowMagicObjects.filterIsInstance<MagicSprinkle>()
        for (spell in magicSprinkles) {
            val spellRect = spell.getBoundingRect()
            for (enemy in enemies) {
                if (enemy in deadEnemies) continue
                if (android.graphics.RectF.intersects(spellRect, enemy.getBoundingRect())) {
                    enemy.hp -= player.damage
                    if (enemy.hp <= 0) deadEnemies.add(enemy)
                    spentSpells[spell] = Layer.ARROW_MAGIC
                    gctx.res.sound.playEffect(R.raw.boom)
                    break 
                }
            }
        }

        // 소멸된 객체 처리 및 새 적 생성
        for (enemy in deadEnemies) {
            if (enemy.isElite) {
                player.eliteKills++
                isElitePresent = false // 엘리트 처치됨 -> 다음 업데이트에서 스폰 가능
            } else {
                player.normalKills++
            }
            world.remove(enemy, Layer.ENEMY)
            
            // 일반 몬스터만 즉시 리스폰
            if (!enemy.isElite) {
                world.add(Enemy.randomSpawn(gctx, player, currentPhase), Layer.ENEMY)
            }
        }
        for ((spell, layer) in spentSpells) {
            world.remove(spell, layer)
        }
        for (item in collectedItems) {
            world.remove(item, Layer.BACKGROUND)
        }
        for (bullet in spentBullets) {
            world.remove(bullet, Layer.ENEMY)
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
                        // 화면 터치 공격 기능 제거
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
                if (!player.consumeMp(30f)) return
                
                // 원을 그리면 캐릭터 위치에서 MagicSprinkle 360도 발사
                gctx.res.sound.playEffect(R.raw.lasergun)
                for (i in 0 until 36) {
                    val angle = Math.toRadians(i * 10.0)
                    val dx = Math.cos(angle).toFloat()
                    val dy = Math.sin(angle).toFloat()
                    val sprinkle = MagicSprinkle(gctx, player.x, player.y, dx, dy, player, world)
                    world.add(sprinkle, Layer.ARROW_MAGIC)
                }
                spellHud.startSprinkleCooldown()
            }
            "arrowLeft", "arrowRight", "arrowUp", "arrowDown" -> {
                if (!spellHud.canCastArrow()) return
                if (!player.consumeMp(10f)) return
                
                // 플레이어 중심, 왼쪽, 오른쪽 3개 지점에서 발사
                val positions = listOf(
                    Pair(player.x, player.y),
                    Pair(player.x - 50f, player.y),
                    Pair(player.x + 50f, player.y)
                )
                
                for (pos in positions) {
                    val arrow = MagicArrow(gctx, pos.first, pos.second, player, world)
                    world.add(arrow, Layer.ARROW_MAGIC)
                }
                gctx.res.sound.playEffect(R.raw.fireballsfx)

                spellHud.startArrowCooldown()
            }
            "triangle" -> {
                if (!spellHud.canCastFloor()) return
                if (!player.consumeMp(50f)) return
                
                val floor = FloorMagic(gctx, player.x, player.y, player, world)
                world.add(floor, Layer.FLOOR_MAGIC)
                
                spellHud.startFloorCooldown()
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
