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
     * 적 속도: 캐릭터 속도의 70% * 일반몬스터 배율(0.8)
     */
    fun getEnemySpeed(gctx: GameContext): Float {
        return getPlayerSpeed(gctx) * 0.7f * 0.8f
    }

    /**
     * 엘리트 적 속도: 일반 적 속도의 60% (0.6)
     */
    fun getEliteEnemySpeed(gctx: GameContext): Float {
        return getEnemySpeed(gctx) * 0.6f
    }

    /**
     * 스펠 속도: 캐릭터 속도의 2배
     */
    fun getSpellSpeed(gctx: GameContext): Float {
        return getPlayerSpeed(gctx) * 2.0f
    }
}
