package com.example.justdrawit

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class Background(private val gctx: GameContext) : IGameObject {
    private val tileDrawable: Drawable = gctx.res.getDrawable(R.drawable.bg_tile_dirt)
    private val tileSize = 200f // 타일 한 개의 가상 좌표 크기

    override fun update(gctx: GameContext) {
    }

    override fun draw(canvas: Canvas) {
        val screenWidth = gctx.metrics.width
        val screenHeight = gctx.metrics.height

        val cols = (screenWidth / tileSize).toInt() + 1
        val rows = (screenHeight / tileSize).toInt() + 1

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val left = (col * tileSize).toInt()
                val top = (row * tileSize).toInt()
                val right = left + tileSize.toInt()
                val bottom = top + tileSize.toInt()
                
                tileDrawable.setBounds(left, top, right, bottom)
                tileDrawable.draw(canvas)
            }
        }
    }
}
