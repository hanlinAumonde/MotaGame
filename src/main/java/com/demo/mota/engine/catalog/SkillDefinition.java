package com.demo.mota.engine.catalog;

import java.util.Map;

/**
 * 技能表（{@code skills.json}）的一条。技能表不区分持有者，怪物与玩家共用。
 *
 * @param effectId 效果机制，对应 {@code SkillEffectRegistry} 里注册的 provider；留空 = 纯展示技能
 * @param params   该机制的参数，字段随 effectId 而定
 * @param cost     可选的释放消耗；缺省为不消耗
 * @param maxCasts 每场战斗最多释放几次（只对主动技能有意义）；缺省 1，负数为不限
 */
public record SkillDefinition(String id, String name, String skillType, String description, String sprite,
                              String effectId, Map<String, Object> params, CostDefinition cost, Integer maxCasts) {

    /** 可选的消耗声明 */
    public record CostDefinition(String costId, Map<String, Object> params) {
    }
}
