package com.demo.mota.engine.battle;

import com.demo.mota.engine.state.GameNumber;

public record BattleSnapshot(GameNumber hp, GameNumber atk, GameNumber def) {
}
