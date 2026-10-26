package com.demo.mota.engine.event;

import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.map.Position;
import com.demo.mota.engine.map.region.RegionClearReport;
import com.demo.mota.engine.map.region.RegionInfo;

import java.util.List;

/**
 * 领域事件：一次命令（走一步、转身）具体改变了什么，按发生顺序记录在 {@link MoveResult#events()} 里。
 *
 * <p>{@link MoveResult} 的类型与说明给状态栏用；事件则告诉表现层「哪一格的门开了、打的是谁、掉了多少血」，
 * 动画、音效、飘字都挂在这上面。规则本身不依赖事件——即使没人订阅，游戏状态也已经是最终结果。
 * 坐标都是事件发生那一层的格坐标。
 */
public sealed interface GameEvent {

    /** 玩家换了朝向 */
    record PlayerTurned(Direction facing) implements GameEvent {
    }

    /** 玩家走到了相邻的一格（同一层） */
    record PlayerStepped(Position from, Position to) implements GameEvent {
    }

    /** 玩家被直接放到某层某格（换层后的落点） */
    record PlayerPlaced(int floor, Position at) implements GameEvent {
    }

    /**
     * 打了一场。
     *
     * @param damage 玩家实际掉的血（打不过时为倒下前剩下的全部生命）
     * @param won    是否击败了怪物；false 表示玩家在这一战里倒下了
     */
    record BattleFought(Position at, String monsterId, GameNumber damage, boolean won) implements GameEvent {
    }

    /** 怪物被击败、从地图上移除 */
    record MonsterRemoved(Position at, String monsterId) implements GameEvent {
    }

    /** 拾取了道具 */
    record ItemPicked(Position at, String itemId) implements GameEvent {
    }

    /** 一格被打开、换成了背景（门、暗墙、机关墙、区域检测格） */
    record TileOpened(Position at, OpenKind kind) implements GameEvent {
    }

    /** 区域检测格的检查没通过 */
    record RegionGateRejected(RegionClearReport report) implements GameEvent {
    }

    /** 换了楼层 */
    record FloorChanged(int from, int to) implements GameEvent {
    }

    /** 进入了下一个区域（随后会有 {@link FloorChanged} 与 {@link PlayerPlaced}） */
    record RegionEntered(RegionInfo from, RegionInfo to) implements GameEvent {
    }

    /** 升级了（可能一次跨多级） */
    record LeveledUp(int from, int to, List<String> learnedSkills) implements GameEvent {
        public LeveledUp {
            learnedSkills = List.copyOf(learnedSkills);
        }
    }

    /** 玩家生命归零 */
    record PlayerDied(Position at) implements GameEvent {
    }

    /** 被打开的是哪种格子 */
    enum OpenKind {
        DOOR, DARK_WALL, TRICKY_WALL, REGION_GATE
    }
}
