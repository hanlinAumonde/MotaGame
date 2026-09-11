package com.demo.mota.engine.skill;

import java.util.List;

/**
 * 技能（不可变值对象），<b>不区分持有者</b>：怪物与玩家共用同一套模型与同一份配置表，
 * 因此同一个技能 id 既可以挂在怪物身上，也可以在角色技能系统落地后被玩家习得。
 * 「谁持有技能」由 {@code AbstractCharacterState} 统一承载。
 *
 * <p><b>当前仅承载展示信息</b>：怪物手册在选中怪物后列出其技能名、类型与说明文本，
 * 战斗流程尚未消费这些数据。待技能系统落地时，可在此记录上补充效果标识（如
 * {@code effectId}）并由战斗侧据其构造 {@code BattleEffect} 加入
 * {@code BattleSimulator.simulate} 的效果列表，展示层无需改动。
 *
 * @param skillId     技能唯一标识
 * @param skillName   技能名
 * @param skillType   主动 / 被动
 * @param description 技能说明，允许手动换行，渲染时还会按可用宽度再次折行
 * @param resourceId  技能图标文件名（位于 {@code /images/} 下），缺省时渲染占位图标
 */
public record Skill(String skillId, String skillName, SkillType skillType,
                    String description, String resourceId) {

    /** 按手动换行符拆出的说明行，供渲染层继续做宽度折行 */
    public List<String> descriptionLines() {
        if (description == null || description.isBlank()) {
            return List.of();
        }
        return description.lines().toList();
    }
}
