package com.demo.mota.engine.factory.skill;

import com.demo.mota.engine.factory.AbstractFactory;
import com.demo.mota.engine.resource.ResourceManager;
import com.demo.mota.engine.skill.Skill;
import com.demo.mota.engine.skill.SkillType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

import static com.demo.mota.engine.configs.SkillConfigConstants.SKILL_LIST_FILE;

/**
 * 技能工厂：读取 {@code /data/skill/skillList.json}，按 id 产出 {@link Skill}。
 * 解析时顺带把技能图标注册进 {@link ResourceManager}。
 *
 * <p>技能表不区分持有者，怪物与（将来的）玩家技能共用这一份配置。
 */
public class SkillFactory extends AbstractFactory<Skill, SkillFactory.SkillData, SkillCreator> {
    private static class Holder {
        private static final SkillFactory INSTANCE = new SkillFactory();
    }

    public static SkillFactory getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    protected String getConfigFileName() {
        return SKILL_LIST_FILE;
    }

    @Override
    protected void parseData(ObjectMapper mapper, InputStream inputStream) throws IOException {
        mapper.readValue(inputStream, new TypeReference<List<SkillData>>(){})
                .forEach(skillData -> {
                    dataRegistry.put(skillData.id, skillData);
                    ResourceManager.getInstance().registerSkillImage(skillData.id, skillData.resourceId);
                });
    }

    @Override
    protected SkillCreator generateCreator(String id) {
        return Skill::new;
    }

    @Override
    protected Skill createProduct(SkillCreator creator, SkillData data) {
        return creator.createSkill(data.id, data.name, SkillType.fromString(data.skillType),
                data.description, data.resourceId);
    }

    /**
     * 按 id 列表批量解析技能；列表为空 / 为 null 时返回空列表。
     * 供怪物工厂（以及将来的玩家技能加载）复用。
     */
    public List<Skill> createByIds(List<String> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return List.of();
        }
        return skillIds.stream().map(this::createById).toList();
    }

    public record SkillData(String id, String name, String skillType,
                            String description, String resourceId) implements Serializable {
    }
}
