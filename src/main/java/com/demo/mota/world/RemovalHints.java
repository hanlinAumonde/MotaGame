package com.demo.mota.world;

import com.demo.mota.engine.event.GameEvent;
import com.demo.mota.engine.map.Position;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 一步行动里「哪一格的什么东西是怎么消失的」：由领域事件记下，供随后的同步决定旧实体怎么退场。
 *
 * <p>同步（{@code LayerSync}）只知道「这一格的对象换了 / 没了」，不知道原因；事件知道原因却不管显示。
 * 两者在这里碰头：有记录的格子播一段消失动画再移除，没有记录的（撤销、读档、将来的脚本改动）立即移除。
 * 每次同步之后清空，免得旧记录误伤以后的变化。不依赖 JavaFX / FXGL，单元测试见 {@code RemovalHintsTest}。
 */
public final class RemovalHints {

    /** 会有东西「消失」的三层（地砖开门后不变，不在其中） */
    public enum Layer { FEATURE, ITEM, MONSTER }

    /** 消失的方式 */
    public enum Exit {
        /** 门被打开：门收拢淡出 */
        DOOR_OPENED,
        /** 暗墙 / 机关墙 / 区域检测格打开：闪白后淡出 */
        WALL_OPENED,
        /** 怪物被击败：缩小淡出 */
        MONSTER_DEFEATED,
        /** 道具被拾取：上浮淡出 */
        ITEM_PICKED
    }

    private final Map<Layer, Map<Position, Exit>> hints = new EnumMap<>(Layer.class);

    /** 记下一个领域事件（与消失无关的事件忽略） */
    public void record(GameEvent event) {
        switch (event) {
            case GameEvent.TileOpened opened -> put(Layer.FEATURE, opened.at(),
                    opened.kind() == GameEvent.OpenKind.DOOR ? Exit.DOOR_OPENED : Exit.WALL_OPENED);
            case GameEvent.MonsterRemoved removed -> put(Layer.MONSTER, removed.at(), Exit.MONSTER_DEFEATED);
            case GameEvent.ItemPicked picked -> put(Layer.ITEM, picked.at(), Exit.ITEM_PICKED);
            default -> { }
        }
    }

    private void put(Layer layer, Position at, Exit exit) {
        hints.computeIfAbsent(layer, l -> new HashMap<>()).put(at, exit);
    }

    /** 这一格这一层的旧实体该怎么退场；没有记录时为 {@code null}（立即移除）。取出即删除 */
    public Exit take(Layer layer, Position at) {
        Map<Position, Exit> byPosition = hints.get(layer);
        return byPosition == null ? null : byPosition.remove(at);
    }

    public void clear() {
        hints.clear();
    }
}
