package com.demo.mota.engine.battle;

import com.demo.mota.engine.state.GameNumber;

/**
 * 战斗模拟过程中的可变状态，供BattleEffect在各回合钩子中读写。
 */
public class BattleState {
    private final GameNumber initialPlayerHp;
    private final GameNumber initialMonsterHp;

    private GameNumber playerHp;
    private GameNumber monsterHp;
    private int round;
    private GameNumber playerDamagePerRound;
    private GameNumber monsterDamagePerRound;

    public BattleState(GameNumber initialPlayerHp, GameNumber initialMonsterHp) {
        this.initialPlayerHp = initialPlayerHp;
        this.initialMonsterHp = initialMonsterHp;
        this.playerHp = initialPlayerHp;
        this.monsterHp = initialMonsterHp;
        this.round = 0;
        this.playerDamagePerRound = GameNumber.ZERO;
        this.monsterDamagePerRound = GameNumber.ZERO;
    }

    public GameNumber getInitialPlayerHp() {
        return initialPlayerHp;
    }

    public GameNumber getInitialMonsterHp() {
        return initialMonsterHp;
    }

    public GameNumber getPlayerHp() {
        return playerHp;
    }

    public void setPlayerHp(GameNumber playerHp) {
        this.playerHp = playerHp;
    }

    public GameNumber getMonsterHp() {
        return monsterHp;
    }

    public void setMonsterHp(GameNumber monsterHp) {
        this.monsterHp = monsterHp;
    }

    public int getRound() {
        return round;
    }

    public void incrementRound() {
        this.round++;
    }

    public GameNumber getPlayerDamagePerRound() {
        return playerDamagePerRound;
    }

    public void setPlayerDamagePerRound(GameNumber playerDamagePerRound) {
        this.playerDamagePerRound = playerDamagePerRound;
    }

    public GameNumber getMonsterDamagePerRound() {
        return monsterDamagePerRound;
    }

    public void setMonsterDamagePerRound(GameNumber monsterDamagePerRound) {
        this.monsterDamagePerRound = monsterDamagePerRound;
    }
}
