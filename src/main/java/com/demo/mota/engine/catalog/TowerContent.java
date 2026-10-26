package com.demo.mota.engine.catalog;

import com.demo.mota.engine.map.floor.FloorSource;
import com.demo.mota.engine.map.floor.GameMapFactory;
import com.demo.mota.engine.map.region.RegionCatalog;
import com.demo.mota.engine.rules.GameRules;

/**
 * 一座塔的全部内容（不可变）：规则、区域、技能 / 道具 / 怪物目录、等级表、玩家开局状态与楼层来源。
 *
 * <p>启动时由内容层（{@code com.demo.mota.content.ContentLoader}）一次装好，之后每一局
 * （{@code GameSession}）都从它出发，不再有「谁先用到谁读盘」的单例。
 * 测试可以直接用内存数据构造。
 */
public record TowerContent(GameRules rules, RegionCatalog regions, SkillCatalog skills, ItemCatalog items,
                           MonsterCatalog monsters, LevelTable levels, PlayerDefinition player, FloorSource floors) {

    /** 开局楼层：第一个区域的第一层 */
    public int startFloor() {
        return regions.all().get(0).floors().get(0);
    }

    /** 按本塔的道具与怪物目录造地图 */
    public GameMapFactory mapFactory() {
        return new GameMapFactory(items, monsters);
    }
}
