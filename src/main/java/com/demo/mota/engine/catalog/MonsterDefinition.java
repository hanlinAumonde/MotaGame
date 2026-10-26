package com.demo.mota.engine.catalog;

import com.demo.mota.engine.GameNumber;

import java.util.List;

/**
 * 怪物表（{@code monsters.json}）的一条。
 *
 * @param skills 技能 id 列表（技能表里的 id），缺省 / 空为「普通」怪物
 * @param tags   角色标签，供技能效果按类别检索（如「亡灵协同」数地图上的 {@code undead}）
 */
public record MonsterDefinition(String id, String name,
                                GameNumber health, GameNumber attack, GameNumber defense,
                                long gold, GameNumber experience,
                                String sprite, List<String> skills, List<String> tags) {
}
