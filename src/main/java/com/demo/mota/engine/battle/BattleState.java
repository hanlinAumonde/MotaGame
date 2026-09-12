package com.demo.mota.engine.battle;

import com.demo.mota.engine.GameNumber;

/**
 * 战斗模拟过程中的可变状态，供BattleEffect在各回合钩子中读写。
 *
 * <p>其中的双方属性是 {@link BattleEffect#adjustPlayerStats} 等钩子<b>调整之后</b>的快照，
 * 基础伤害也据此算出，因此效果在回合钩子里读到的一切都已包含战前增减益。
 */
public class BattleState {
    private final BattleSnapshot playerStats;
    private final BattleSnapshot monsterStats;

    /** 战前算定的每回合基础伤害（攻击-防御，已 clamp 到非负），不随回合变化 */
    private final GameNumber basePlayerDamage;
    private final GameNumber baseMonsterDamage;

    private GameNumber playerHp;
    private GameNumber monsterHp;
    private int round;
    private GameNumber playerDamagePerRound;
    private GameNumber monsterDamagePerRound;

    public BattleState(BattleSnapshot playerStats, BattleSnapshot monsterStats,
                       GameNumber basePlayerDamage, GameNumber baseMonsterDamage) {
        this.playerStats = playerStats;
        this.monsterStats = monsterStats;
        this.basePlayerDamage = basePlayerDamage;
        this.baseMonsterDamage = baseMonsterDamage;
        this.playerHp = playerStats.hp();
        this.monsterHp = monsterStats.hp();
        this.round = 0;
        this.playerDamagePerRound = GameNumber.ZERO;
        this.monsterDamagePerRound = GameNumber.ZERO;
    }

    public BattleSnapshot getPlayerStats() {
        return playerStats;
    }

    public BattleSnapshot getMonsterStats() {
        return monsterStats;
    }

    public GameNumber getBasePlayerDamage() {
        return basePlayerDamage;
    }

    public GameNumber getBaseMonsterDamage() {
        return baseMonsterDamage;
    }

    public GameNumber getInitialPlayerHp() {
        return playerStats.hp();
    }

    public GameNumber getInitialMonsterHp() {
        return monsterStats.hp();
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
