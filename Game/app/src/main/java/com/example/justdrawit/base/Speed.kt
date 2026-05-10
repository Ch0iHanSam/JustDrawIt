package com.example.justdrawit.base

import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

object Speed {
    /**
     * 기준 속도: 화면 가로를 5초에 횡단하는 속도
     */
    fun getBaseSpeed(gctx: GameContext): Float {
        return gctx.metrics.width / 5.0f
    }

    /**
     * 캐릭터 속도: 기준 속도와 동일
     */
    fun getPlayerSpeed(gctx: GameContext): Float {
        return getBaseSpeed(gctx)
    }

    /**
     * 적 속도: 캐릭터 속도의 70%
     */
    fun getEnemySpeed(gctx: GameContext): Float {
        return getPlayerSpeed(gctx) * 0.7f
    }

    /**
     * 스펠 속도: 캐릭터 속도의 2배
     */
    fun getSpellSpeed(gctx: GameContext): Float {
        return getPlayerSpeed(gctx) * 2.0f
    }
}
