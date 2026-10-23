package com.demo.mota.app.event;

import com.demo.mota.engine.event.GameEvent;
import javafx.event.Event;
import javafx.event.EventType;

/**
 * 领域事件（{@link GameEvent}）在 FXGL 事件总线上的包装。订阅方按大类挑自己关心的：
 *
 * <ul>
 *   <li>{@link #MAP}：转身、走动、落位、拾取、格子打开、怪物移除</li>
 *   <li>{@link #BATTLE}：战斗</li>
 *   <li>{@link #PLAYER}：升级、死亡</li>
 *   <li>{@link #FLOOR}：换层、进入区域、区域检测格拒绝通过</li>
 * </ul>
 * 订阅 {@link #ANY} 即收到全部。大类由 {@link #typeOf} 决定，新增一种领域事件时编译器会提醒在那里归类。
 */
public final class MotaEvent extends Event {

    public static final EventType<MotaEvent> ANY = new EventType<>(Event.ANY, "MOTA");
    public static final EventType<MotaEvent> MAP = new EventType<>(ANY, "MOTA_MAP");
    public static final EventType<MotaEvent> BATTLE = new EventType<>(ANY, "MOTA_BATTLE");
    public static final EventType<MotaEvent> PLAYER = new EventType<>(ANY, "MOTA_PLAYER");
    public static final EventType<MotaEvent> FLOOR = new EventType<>(ANY, "MOTA_FLOOR");

    private final GameEvent payload;

    public MotaEvent(GameEvent payload) {
        super(typeOf(payload));
        this.payload = payload;
    }

    public GameEvent payload() {
        return payload;
    }

    static EventType<MotaEvent> typeOf(GameEvent event) {
        return switch (event) {
            case GameEvent.PlayerTurned _, GameEvent.PlayerStepped _, GameEvent.PlayerPlaced _,
                 GameEvent.ItemPicked _, GameEvent.TileOpened _, GameEvent.MonsterRemoved _ -> MAP;
            case GameEvent.BattleFought _ -> BATTLE;
            case GameEvent.LeveledUp _, GameEvent.PlayerDied _ -> PLAYER;
            case GameEvent.FloorChanged _, GameEvent.RegionEntered _, GameEvent.RegionGateRejected _ -> FLOOR;
        };
    }

    @Override
    public String toString() {
        return "MotaEvent[" + payload + "]";
    }
}
