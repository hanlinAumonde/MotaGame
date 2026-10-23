package com.demo.mota.world;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.demo.mota.app.event.MotaEvent;
import com.demo.mota.app.nav.Navigator;
import com.demo.mota.app.services.AssetService;
import com.demo.mota.app.services.SessionService;
import com.demo.mota.engine.event.GameEvent;
import com.demo.mota.engine.item.Item;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.MapManager;
import com.demo.mota.engine.map.Position;
import com.demo.mota.engine.map.region.RegionInfo;
import com.demo.mota.engine.map.tile.BackGroundTile;
import com.demo.mota.engine.map.tile.Tile;
import com.demo.mota.engine.session.GameSession;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.state.monster.Monster;
import com.demo.mota.ui.screen.transition.FloorTransitionSubScene;
import com.demo.mota.world.RemovalHints.Exit;
import com.demo.mota.world.RemovalHints.Layer;
import com.demo.mota.world.component.MonsterComponent;
import com.demo.mota.world.component.PlayerComponent;
import com.demo.mota.world.fx.MapEffects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * 地图显示（取代 {@code EntityMapView}）：把当前这一局的地图同步成 FXGL 世界里的实体（{@link MotaEntityFactory}）。
 *
 * <p><b>同步规则</b>（{@link #sync()}，调用方在每次行动后调用）：
 * <ul>
 *   <li>换了一张 {@link GameMap}（换层、换区域、撤销 / 读档重建一局）→ 三层全部销毁重建、主角直接落位；
 *       若这一步有 {@code FloorChanged} 事件，先叠上换层过渡，黑幕落定时才重建（{@link FloorTransitionSubScene}）</li>
 *   <li>同一张地图里，各层由 {@link LayerSync} 逐格对照，只动变了的格子。一格地块分成地砖与地物两层
 *       （见 {@link MotaEntityFactory}）：开门时只有地物（门）退场，地砖原样不动；
 *       旧实体怎么退场由领域事件决定（{@link RemovalHints}）：开门 / 击败 / 拾取播一段动画（{@link MapEffects}），
 *       没有事件描述的变化（撤销、读档、将来的脚本）立即移除——差异同步始终是兜底</li>
 *   <li>伤害数字每次逐只对照，文字或颜色变了才更新</li>
 *   <li>主角走到相邻格时平滑走过去（{@link PlayerComponent#step}），其余位置变化直接落位；位置没变只更新朝向</li>
 * </ul>
 * 领域事件经会话服务订阅（换局时自动注销）：在 {@code SessionService.move} 返回之前就已记下，随后的同步即可用上。
 *
 * <p>世界坐标 = 格坐标 × 64，地图在窗口里的位置由视口决定（{@link WorldGrid}）。每局新建一个：
 * {@code startNewGame()} 会清空世界里的实体与工厂。
 */
public final class MapPresenter {

    private final SessionService sessions;
    private final FloorTransitionSubScene transition;

    /** 地砖层：按底图的精灵 id 比较——开门换上的背景格沿用原来的底图，地砖不动 */
    private final LayerSync<String, Entity> floors =
            LayerSync.byValue(this::spawnFloor, (pos, e) -> e.removeFromWorld());
    /** 地物层：非背景格的 Tile，按对象身份比较——开门 / 揭墙时整块被替换，地物退场 */
    private final LayerSync<Tile, Entity> features =
            new LayerSync<>(this::spawnFeature, (pos, e) -> despawn(Layer.FEATURE, pos, e));
    private final LayerSync<Item, Entity> items = new LayerSync<>(this::spawnItem, (pos, e) -> despawn(Layer.ITEM, pos, e));
    private final LayerSync<Monster, Entity> monsters =
            new LayerSync<>(this::spawnMonster, (pos, e) -> despawn(Layer.MONSTER, pos, e));
    private final RemovalHints hints = new RemovalHints();
    /** 正在播消失动画的实体：换图时一并移除，免得在新楼层上播完 */
    private final Set<Entity> leaving = new HashSet<>();

    /** 当前显示的是哪一张地图：换了就整张重建 */
    private GameMap shownMap;
    private Entity player;
    private PlayerComponent playerView;
    /** 主角实体所在（或正在走向）的格子 */
    private Position playerAt;

    /** 这一步换了层：下一次换图时播过渡 */
    private boolean floorChanged;
    /** 过渡已叠上、黑幕还没落定：旧楼层要留在画面上，期间的同步一律推迟 */
    private boolean waitingForBlack;

    private Runnable onStepFinished = () -> { };

    /** 在 {@code initUI} 里创建（加载场景的后台线程，游戏场景尚未显示） */
    public MapPresenter(SessionService sessions, AssetService assets, Navigator navigator) {
        this.sessions = sessions;
        this.transition = new FloorTransitionSubScene(navigator);
        WorldGrid.applyViewport(FXGL.getGameScene().getViewport());
        FXGL.getGameWorld().addEntityFactory(new MotaEntityFactory(assets));
        sessions.subscribe(MotaEvent.MAP, hints::record);
        sessions.subscribe(MotaEvent.FLOOR, event -> {
            if (event instanceof GameEvent.FloorChanged) {
                floorChanged = true;
            }
        });
    }

    /** 主角走完一步时回调（每一步一次），供调用方接着处理缓下来的输入 */
    public void setOnStepFinished(Runnable listener) {
        this.onStepFinished = listener == null ? () -> { } : listener;
    }

    /** 主角是否正在走（走完之前不执行下一步） */
    public boolean isAnimating() {
        return playerView != null && playerView.isWalking();
    }

    // ==================== 同步 ====================

    public void sync() {
        if (waitingForBlack) {
            return;
        }
        GameSession session = sessions.session();
        GameMap map = session.maps().getCurrentMap();
        boolean mapChanged = map != shownMap;
        if (mapChanged && floorChanged && shownMap != null) {
            floorChanged = false;
            waitingForBlack = true;
            transition.play(caption(session.maps()), () -> {
                waitingForBlack = false;
                sync();
            });
            return;
        }
        floorChanged = false;
        if (mapChanged) {
            clear();
            shownMap = map;
        }
        floors.sync(floorsOf(map));
        features.sync(featuresOf(map));
        items.sync(map.getItems());
        monsters.sync(map.getMonsters());
        hints.clear();

        PlayerStateManager state = session.player();
        monsters.views().forEach(e -> e.getComponent(MonsterComponent.class).refresh(state));
        syncPlayer(session, mapChanged);
    }

    /** 黑屏上写出到了哪：区域名常带「·」（如「试炼之塔·下层」），与楼层分两行写 */
    private static String caption(MapManager maps) {
        RegionInfo region = maps.getCurrentRegion();
        String floor = "第 " + maps.getCurrentMap().getFloorNumber() + " 层";
        return region == null ? floor : region.name() + "\n" + floor;
    }

    private void clear() {
        hints.clear();
        floors.clear();
        features.clear();
        items.clear();
        monsters.clear();
        leaving.forEach(Entity::removeFromWorld);
        leaving.clear();
    }

    /** 每格的底图精灵 id */
    private static Map<Position, String> floorsOf(GameMap map) {
        Map<Position, String> result = new HashMap<>();
        forEachCell(map, (pos, tile) -> result.put(pos, tile.getBgResourceId()));
        return result;
    }

    /** 非背景格的 Tile（墙 / 门 / 楼梯 / 机关墙 / 检测格） */
    private static Map<Position, Tile> featuresOf(GameMap map) {
        Map<Position, Tile> result = new HashMap<>();
        forEachCell(map, (pos, tile) -> {
            if (!(tile instanceof BackGroundTile)) {
                result.put(pos, tile);
            }
        });
        return result;
    }

    private static void forEachCell(GameMap map, BiConsumer<Position, Tile> action) {
        for (int y = 0; y < GameMap.SIDE; y++) {
            for (int x = 0; x < GameMap.SIDE; x++) {
                Position pos = new Position(x, y);
                Tile tile = map.getTileAt(pos);
                if (tile != null) {
                    action.accept(pos, tile);
                }
            }
        }
    }

    // ==================== 生成与退场 ====================

    private Entity spawnFloor(Position pos, String sprite) {
        return spawn("floor", pos, sprite);
    }

    private Entity spawnFeature(Position pos, Tile tile) {
        return spawn("feature", pos, tile);
    }

    private Entity spawnItem(Position pos, Item item) {
        return spawn("item", pos, item);
    }

    private Entity spawnMonster(Position pos, Monster monster) {
        return spawn("monster", pos, monster);
    }

    private static Entity spawn(String name, Position pos, Object model) {
        return FXGL.getGameWorld().spawn(name,
                new SpawnData(WorldGrid.x(pos), WorldGrid.y(pos)).put(MotaEntityFactory.MODEL, model));
    }

    /** 有领域事件描述的格子播消失动画再移除，否则立即移除 */
    private void despawn(Layer layer, Position pos, Entity entity) {
        Exit exit = hints.take(layer, pos);
        if (exit == null) {
            entity.removeFromWorld();
            return;
        }
        leaving.add(entity);
        MapEffects.play(exit, entity, () -> {
            if (leaving.remove(entity)) {
                entity.removeFromWorld();
            }
        });
    }

    // ==================== 主角 ====================

    private void syncPlayer(GameSession session, boolean mapChanged) {
        Position pos = session.maps().getPlayerPosition();
        if (pos == null) {
            return;
        }
        var facing = session.player().getCurrentDirection();
        if (player == null) {
            player = FXGL.getGameWorld().spawn("player", new SpawnData(WorldGrid.x(pos), WorldGrid.y(pos)));
            playerView = player.getComponent(PlayerComponent.class);
            playerView.setOnStepFinished(() -> onStepFinished.run());
            playerView.place(pos, facing);
        } else if (mapChanged || playerAt == null || !(pos.equals(playerAt) || pos.isAdjacentTo(playerAt))) {
            playerView.place(pos, facing);
        } else if (pos.equals(playerAt)) {
            playerView.face(facing);
        } else {
            playerView.step(pos, facing);
        }
        playerAt = pos;
    }
}
