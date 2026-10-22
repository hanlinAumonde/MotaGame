package com.demo.mota.ui.screen.game.map;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.demo.mota.app.services.CharacterSprite;
import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.item.Item;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.Position;
import com.demo.mota.engine.map.tile.Tile;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.state.monster.Monster;
import com.demo.mota.ui.Fonts;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.HashMap;
import java.util.Map;


/**
 * 用 FXGL 实体显示地图：每个地块、道具、怪物各是一个实体，玩家一个实体，按 z 序叠放。
 *
 * <p>每个实体的视图是一块单格大小的 {@link Canvas}，由 {@link MapCellPainter} 画上去，
 * 因此画面与改造前逐格画整张画布完全一致；变化的只是「谁来重画」——
 * {@link #sync()} 用 {@link LayerSync} 逐格对照，只替换变了的格子：
 * <ul>
 *   <li>换了一张 {@link GameMap}（换层、换区域、新开一局、撤销 / 读档重建）→ 全部销毁重建；
 *       若事先 {@link #playFloorTransition 要求了过渡}，则先落幕、黑幕下重建、再升幕</li>
 *   <li>同一张地图里开门 / 揭暗墙（Tile 被替换）、击败怪物、拾取道具 → 只动那一格</li>
 *   <li>伤害数字每次都对照一遍，文字或颜色变了才重画该怪物</li>
 *   <li>玩家走到相邻格时<b>平滑滑过去</b>并播行走帧（{@link WalkCycle}），其余位置变化直接落位</li>
 * </ul>
 * 动画由挂在实体上的 FXGL {@link Component} 每帧推进；引擎状态在同步前就已是最终结果，动画只是追上它。
 * 实体放在 FXGL 游戏场景的世界层，位于界面层（各块画布）之下，地图区域由 {@code MotaController} 留空透出。
 */
public final class EntityMapView implements MapView {

    private static final int Z_TILE = 0;
    private static final int Z_ITEM = 1;
    private static final int Z_MONSTER = 2;
    private static final int Z_PLAYER = 3;
    /** 楼层过渡的黑幕盖在一切之上 */
    private static final int Z_COVER = 100;

    /** 走一格用多少秒 */
    private static final double WALK_STEP_SECONDS = 0.12;

    private final GameEngine engine;
    private final MapCellPainter painter;
    private final double originX;
    private final double originY;
    private final double cellSize;

    private final LayerSync<Tile, Entity> tiles = new LayerSync<>(this::spawnTile, Entity::removeFromWorld);
    private final LayerSync<Item, Entity> items = new LayerSync<>(this::spawnItem, Entity::removeFromWorld);
    private final LayerSync<Monster, MonsterSprite> monsters =
            new LayerSync<>(this::spawnMonster, sprite -> sprite.entity().removeFromWorld());

    /** 当前显示的是哪一张地图：换了就整张重建 */
    private GameMap shownMap;
    private PlayerSprite player;
    private FloorCover cover;
    private boolean visible = true;

    /** 下一次换图时要播的过渡说明（{@link #playFloorTransition}），{@code null} = 直接切换 */
    private String pendingCaption;
    private Runnable onAnimationsFinished = () -> { };

    public EntityMapView(GameEngine engine, MapCellPainter painter, double originX, double originY) {
        this.engine = engine;
        this.painter = painter;
        this.originX = originX;
        this.originY = originY;
        this.cellSize = painter.cellSize();
    }

    // ==================== MapView ====================

    @Override
    public void sync() {
        // 落幕期间旧楼层还要留在画面上：换图推迟到黑幕落定的那一刻（由过渡组件再调一次）
        if (cover != null && cover.isRunning() && !cover.isSwapped()) {
            return;
        }
        GameMap map = engine.getMapManager().getCurrentMap();
        boolean mapChanged = map != shownMap;
        String caption = pendingCaption;
        pendingCaption = null;

        if (mapChanged && caption != null && shownMap != null && visible) {
            cover().start(caption);
            return;
        }
        if (mapChanged) {
            clear();
            shownMap = map;
        }
        tiles.sync(tilesOf(map));
        items.sync(map.getItems());
        monsters.sync(map.getMonsters());

        PlayerStateManager state = engine.getPlayerStateManager();
        monsters.views().forEach(sprite -> sprite.refresh(state));
        syncPlayer(state, mapChanged);
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        tiles.views().forEach(this::applyVisibility);
        items.views().forEach(this::applyVisibility);
        monsters.views().forEach(sprite -> applyVisibility(sprite.entity()));
        if (player != null) {
            applyVisibility(player.entity());
        }
        if (cover != null) {
            applyVisibility(cover.entity());
        }
    }

    @Override
    public void playFloorTransition(String caption) {
        this.pendingCaption = caption == null ? "" : caption;
    }

    @Override
    public boolean isAnimating() {
        return (player != null && player.isWalking()) || (cover != null && cover.isRunning());
    }

    @Override
    public void setOnAnimationsFinished(Runnable listener) {
        this.onAnimationsFinished = listener == null ? () -> { } : listener;
    }

    /** 某个动画刚播完：若已没有别的动画在播，通知调用方 */
    private void animationEnded() {
        if (!isAnimating()) {
            onAnimationsFinished.run();
        }
    }

    // ==================== 图层 ====================

    private void clear() {
        tiles.clear();
        items.clear();
        monsters.clear();
    }

    private static Map<Position, Tile> tilesOf(GameMap map) {
        Map<Position, Tile> result = new HashMap<>();
        for (int y = 0; y < GameMap.SIDE; y++) {
            for (int x = 0; x < GameMap.SIDE; x++) {
                Position pos = new Position(x, y);
                result.put(pos, map.getTileAt(pos));
            }
        }
        return result;
    }

    private Entity spawnTile(Position pos, Tile tile) {
        Canvas canvas = newCellCanvas();
        painter.paintTile(canvas.getGraphicsContext2D(), tile);
        return spawn(xOf(pos), yOf(pos), canvas, Z_TILE);
    }

    private Entity spawnItem(Position pos, Item item) {
        Canvas canvas = newCellCanvas();
        painter.paintItem(canvas.getGraphicsContext2D(), item);
        return spawn(xOf(pos), yOf(pos), canvas, Z_ITEM);
    }

    private MonsterSprite spawnMonster(Position pos, Monster monster) {
        Canvas canvas = newCellCanvas();
        // 伤害数字留给随后的 refresh 画：新建与刷新走同一条路
        return new MonsterSprite(monster, canvas, spawn(xOf(pos), yOf(pos), canvas, Z_MONSTER));
    }

    private Entity spawn(double x, double y, Node view, int zIndex) {
        Entity entity = FXGL.entityBuilder()
                .at(x, y)
                .view(view)
                .zIndex(zIndex)
                .buildAndAttach();
        applyVisibility(entity);
        return entity;
    }

    private void applyVisibility(Entity entity) {
        entity.getViewComponent().setVisible(visible);
    }

    private Canvas newCellCanvas() {
        return new Canvas(cellSize, cellSize);
    }

    private double xOf(Position pos) {
        return originX + pos.x() * cellSize;
    }

    private double yOf(Position pos) {
        return originY + pos.y() * cellSize;
    }

    /** 只增删 / 刷新的组件：不需要 FXGL 反射注入字段 */
    private abstract static class FrameComponent extends Component {
        @Override
        public boolean isComponentInjectionRequired() {
            return false;
        }
    }

    // ==================== 玩家 ====================

    private void syncPlayer(PlayerStateManager state, boolean mapChanged) {
        Position pos = engine.getMapManager().getPlayerPosition();
        if (pos == null) {
            return;
        }
        if (player == null) {
            player = new PlayerSprite();
        }
        player.moveTo(pos, state.getCurrentDirection(), !mapChanged && visible);
    }

    /**
     * 玩家实体：位置变到相邻格时平滑滑过去并播行走帧，其余变化（换图、跳转）直接落位；
     * 显示的帧（朝向 + 帧下标）变了才重画。
     */
    private final class PlayerSprite {
        private final Canvas canvas = newCellCanvas();
        private final Entity entity;

        /** 引擎里的位置（动画的终点） */
        private Position logical;
        private Direction direction = Direction.DOWN;

        private boolean walking;
        private double fromX;
        private double fromY;
        private double toX;
        private double toY;
        private double elapsed;
        /** 连续行走的步数，决定迈哪只脚（见 {@link WalkCycle}） */
        private int stepIndex;

        private Direction shownDirection;
        private int shownFrame = Integer.MIN_VALUE;

        PlayerSprite() {
            entity = spawn(originX, originY, canvas, Z_PLAYER);
            entity.addComponent(new FrameComponent() {
                @Override
                public void onUpdate(double tpf) {
                    advance(tpf);
                }
            });
        }

        Entity entity() {
            return entity;
        }

        boolean isWalking() {
            return walking;
        }

        /**
         * @param animate 允许走一步的动画（同一张地图、地图可见）；即便允许，也只有相邻格才会走
         */
        void moveTo(Position pos, Direction facing, boolean animate) {
            direction = facing;
            if (pos.equals(logical)) {
                // 位置没变（转身、打怪、开门……）：走路动画照常播完，否则只更新朝向
                if (!walking) {
                    show(direction, WalkCycle.IDLE);
                }
                return;
            }
            boolean step = animate && logical != null && logical.isAdjacentTo(pos);
            logical = pos;
            toX = xOf(pos);
            toY = yOf(pos);
            if (step) {
                // 从当前所在（可能还在上一步的半路）出发，滑向新的一格
                fromX = entity.getX();
                fromY = entity.getY();
                elapsed = 0;
                walking = true;
            } else {
                walking = false;
                entity.setPosition(toX, toY);
                show(direction, WalkCycle.IDLE);
            }
        }

        private void advance(double tpf) {
            if (!walking) {
                return;
            }
            elapsed += tpf;
            double progress = Math.min(1, elapsed / WALK_STEP_SECONDS);
            entity.setPosition(fromX + (toX - fromX) * progress, fromY + (toY - fromY) * progress);
            if (progress < 1) {
                show(direction, WalkCycle.frameAt(stepIndex, progress, walkLength()));
                return;
            }
            walking = false;
            stepIndex++;
            show(direction, WalkCycle.IDLE);
            animationEnded();
        }

        private void show(Direction dir, int frame) {
            if (dir == shownDirection && frame == shownFrame) {
                return;
            }
            shownDirection = dir;
            shownFrame = frame;
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, cellSize, cellSize);
            painter.paintPlayer(gc, dir, frame);
        }

        private int walkLength() {
            CharacterSprite sprite = painter.playerSprite();
            return sprite == null ? 0 : sprite.walkLength(direction);
        }
    }

    // ==================== 楼层过渡 ====================

    private FloorCover cover() {
        if (cover == null) {
            cover = new FloorCover();
        }
        return cover;
    }

    /**
     * 楼层切换的黑幕：一张盖住整张地图的黑底 + 居中的楼层名，平时透明。
     * 按 {@link FadeTimeline} 落幕 → 黑屏停留 → 升幕；落定的那一刻补一次 {@link #sync()}，在黑幕下换成新楼层。
     */
    private final class FloorCover {
        private final FadeTimeline timeline = FadeTimeline.FLOOR_SWITCH;
        private final Text caption = new Text();
        private final Entity entity;

        private boolean running;
        private boolean swapped;
        private double elapsed;

        FloorCover() {
            double size = cellSize * GameMap.SIDE;
            caption.setFont(Fonts.bold(34));
            caption.setFill(Color.web("#f5f5f5"));
            caption.setTextAlignment(TextAlignment.CENTER);
            caption.setLineSpacing(14);
            StackPane view = new StackPane(new Rectangle(size, size, Color.BLACK), caption);
            entity = spawn(originX, originY, view, Z_COVER);
            entity.getViewComponent().setOpacity(0);
            entity.addComponent(new FrameComponent() {
                @Override
                public void onUpdate(double tpf) {
                    advance(tpf);
                }
            });
        }

        Entity entity() {
            return entity;
        }

        boolean isRunning() {
            return running;
        }

        boolean isSwapped() {
            return swapped;
        }

        void start(String text) {
            caption.setText(text);
            running = true;
            swapped = false;
            elapsed = 0;
        }

        private void advance(double tpf) {
            if (!running) {
                return;
            }
            elapsed += tpf;
            entity.getViewComponent().setOpacity(timeline.opacityAt(elapsed));
            if (!swapped && timeline.isSwapReached(elapsed)) {
                swapped = true;
                sync();
            }
            if (timeline.isDone(elapsed)) {
                running = false;
                entity.getViewComponent().setOpacity(0);
                animationEnded();
            }
        }
    }

    // ==================== 怪物 ====================

    /** 怪物实体：伤害数字的文字或颜色变了才重画 */
    private final class MonsterSprite {
        private final Monster monster;
        private final Canvas canvas;
        private final Entity entity;
        private MapCellPainter.DamageLabel shown;

        MonsterSprite(Monster monster, Canvas canvas, Entity entity) {
            this.monster = monster;
            this.canvas = canvas;
            this.entity = entity;
        }

        Entity entity() {
            return entity;
        }

        void refresh(PlayerStateManager state) {
            MapCellPainter.DamageLabel label = painter.damageLabel(monster, state);
            if (label.equals(shown)) {
                return;
            }
            shown = label;
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, cellSize, cellSize);
            painter.paintMonster(gc, monster, label);
        }
    }
}
