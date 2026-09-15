package com.demo.mota.engine.skill;

import java.util.List;

/**
 * 技能（不可变值对象），<b>不区分持有者</b>：怪物与玩家共用同一套模型与同一份配置表，
 * 因此同一个技能 id 既可以挂在怪物身上，也可以在角色技能系统落地后被玩家习得。
 * 「谁持有技能」由 {@code AbstractCharacterState} 统一承载。
 *
 * <p>技能本身<b>不包含任何战斗逻辑</b>，只声明「用哪种效果机制（{@code effectId}）、
 * 配什么参数（{@code params}）」。战斗开始时由
 * {@code engine.skill.effect.SkillEffectResolver} 查
 * {@code SkillEffectRegistry} 把它翻译成 {@code BattleEffect} 交给
 * {@code BattleSimulator}。这层间接的意义在于：新技能通常只是
 * 「已有 effectId + 一组新参数」，改 JSON 即可；真正的新机制才需要注册新的 provider，
 * 而注册接口是开放的，未来的外部脚本可以沿同一路径接入。
 *
 * @param skillId     技能唯一标识
 * @param skillName   技能名
 * @param skillType   主动 / 被动
 * @param description 技能说明，允许手动换行，渲染时还会按可用宽度再次折行
 * @param resourceId  技能图标文件名（位于 {@code /Graphics/skills/} 下），缺省时渲染占位图标
 * @param effectId    战斗效果机制标识，对应 {@code SkillEffectRegistry} 中注册的 provider；
 *                    留空表示纯展示技能（不参与战斗计算）
 * @param params      效果参数，永不为 null（无参数时为 {@link SkillParams#EMPTY}）
 */
public record Skill(String skillId, String skillName, SkillType skillType,
                    String description, String resourceId,
                    String effectId, SkillParams params) {

    public Skill {
        params = params == null ? SkillParams.EMPTY : params;
    }

    /** 是否声明了战斗效果；未声明的技能只在怪物手册里展示 */
    public boolean hasEffect() {
        return effectId != null && !effectId.isBlank();
    }

    /** 按手动换行符拆出的说明行，供渲染层继续做宽度折行 */
    public List<String> descriptionLines() {
        if (description == null || description.isBlank()) {
            return List.of();
        }
        return description.lines().toList();
    }
}
