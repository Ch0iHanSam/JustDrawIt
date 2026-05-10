package com.example.justdrawit

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class Background(private val gctx: GameContext, private val player: Player) : IGameObject {
    private val tileDrawable: Drawable = gctx.res.getDrawable(R.drawable.bg_tile_dirt)
    private val tileSize = 200f // 타일 한 개의 가상 좌표 크기
    private val mapTileCount = 20 // 전체 맵의 타일 개수 (20x20)

    override fun update(gctx: GameContext) {
    }

    override fun draw(canvas: Canvas) {
        val screenWidth = gctx.metrics.width
        val screenHeight = gctx.metrics.height
        val totalMapSize = tileSize * mapTileCount

        // 맵의 스크롤 오프셋(카메라의 왼쪽 상단 월드 좌표) 계산
        var offsetX = player.x - screenWidth / 2
        var offsetY = player.y - screenHeight / 2

        // 맵 경계에 도달하면 스크롤 고정
        offsetX = offsetX.coerceIn(0f, (totalMapSize - screenWidth).coerceAtLeast(0f))
        offsetY = offsetY.coerceIn(0f, (totalMapSize - screenHeight).coerceAtLeast(0f))

        // 화면에 보일 타일 범위 계산
        val startCol = (offsetX / tileSize).toInt().coerceAtLeast(0)
        val endCol = ((offsetX + screenWidth) / tileSize).toInt().coerceAtMost(mapTileCount - 1)
        val startRow = (offsetY / tileSize).toInt().coerceAtLeast(0)
        val endRow = ((offsetY + screenHeight) / tileSize).toInt().coerceAtMost(mapTileCount - 1)

        for (row in startRow..endRow) {
            for (col in startCol..endCol) {
                val left = (col * tileSize - offsetX).toInt()
                val top = (row * tileSize - offsetY).toInt()
                val right = left + tileSize.toInt()
                val bottom = top + tileSize.toInt()
                
                tileDrawable.setBounds(left, top, right, bottom)
                tileDrawable.draw(canvas)
            }
        }
    }
}
