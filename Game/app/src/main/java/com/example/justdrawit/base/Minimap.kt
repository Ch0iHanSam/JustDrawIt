package com.example.justdrawit.base

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.justdrawit.MainScene
import com.example.justdrawit.enemy.Enemy
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class Minimap(private val gctx: GameContext, private val player: Player) : IGameObject {
    private val size = 200f // 미니맵 정사각형 크기
    private val padding = 50f // 화면 끝에서의 여백
    private val mapTileCount = 20f // 전체 맵의 타일 개수 (20x20)
    private val tileSize = 200f // Background.kt에서 정의한 타일 크기

    private val bgPaint = Paint().apply {
        color = Color.parseColor("#5D4037") // 진한 갈색
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val playerPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }
    private val enemyPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }

    override fun update(gctx: GameContext) {
    }

    override fun draw(canvas: Canvas) {
        val screenWidth = gctx.metrics.width

        // 미니맵 박스 위치 (오른쪽 상단)
        val left = screenWidth - size - padding
        val top = padding
        val right = screenWidth - padding
        val bottom = padding + size

        // 배경 및 테두리 그리기
        canvas.drawRect(left, top, right, bottom, bgPaint)
        canvas.drawRect(left, top, right, bottom, borderPaint)

        // 전체 맵 너비 = tileSize * mapTileCount
        val totalMapSize = tileSize * mapTileCount
        
        // 캐릭터의 좌표가 전체 맵에서 차지하는 비율 계산 (0.0 ~ 1.0)
        val relX = player.x / totalMapSize
        val relY = player.y / totalMapSize

        // 미니맵 내의 실제 좌표로 변환
        val dotX = left + (relX * size)
        val dotY = top + (relY * size)

        // 캐릭터 표시 (빨간 점, 미니맵 내에서 실제 위치에 따라 움직임)
        canvas.drawCircle(dotX, dotY, 8f, playerPaint)

        // 적 표시 (파란 점)
        val scene = gctx.scene as? MainScene ?: return
        val enemies = scene.world.objectsAt(MainScene.Layer.ENEMY).filterIsInstance<Enemy>()
        for (enemy in enemies) {
            val relEnemyX = enemy.x / totalMapSize
            val relEnemyY = enemy.y / totalMapSize
            val enemyDotX = left + (relEnemyX * size)
            val enemyDotY = top + (relEnemyY * size)
            
            // 엘리트는 보라색, 일반은 파란색
            enemyPaint.color = if (enemy.isElite) Color.parseColor("#9C27B0") else Color.BLUE
            canvas.drawCircle(enemyDotX, enemyDotY, if (enemy.isElite) 7f else 5f, enemyPaint)
        }

        // 아이템 표시 (초록색 점)
        val items = scene.world.objectsAt(MainScene.Layer.BACKGROUND).filterIsInstance<UpgradeItem>()
        val itemPaint = Paint().apply { color = Color.GREEN; style = Paint.Style.FILL }
        for (item in items) {
            val relItemX = item.x / totalMapSize
            val relItemY = item.y / totalMapSize
            val itemDotX = left + (relItemX * size)
            val itemDotY = top + (relItemY * size)
            canvas.drawCircle(itemDotX, itemDotY, 6f, itemPaint)
        }
    }
}
