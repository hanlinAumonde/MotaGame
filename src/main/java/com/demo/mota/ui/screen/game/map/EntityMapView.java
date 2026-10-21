package com.demo.mota.ui.screen.game.map;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.demo.mota.engine.GameEngine;
import com.demo.mota.engine.Item.Item;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.Position;
import com.demo.mota.engine.map.tile.Tile;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.state.monster.Monster;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import java.util.HashMap;
import java.util.Map;

import static com.demo.mota.engine.configs.MapConfigConstants.MAP_SIDE_LENGTH;

/**
 * 用 FXGL 实体显示地图：每个地块、道具、怪物各是一个实体，玩家一个实体，按 z 序叠放。
 *
 * <p>每个实体的视图是一块单格大小的 {@link Canvas}，由 {@link MapCellPainter} 画上去，
 * 因此画面与改造前逐格画整张画布完全一致；变化的只是「谁来重画」——
 * {@link #sync()} 用 {@link LayerSync} 逐格对照，只替换变了的格子：
 * <ul>
 *   <li>换了一张 {@link GameMap}（换层、换区域、新开一局、撤销 / 读档重建）→ 全部销毁重建</li>
 *   <li>同一张地图里开门 / 揭暗墙（Tile 被替换）、击败怪物、拾取道具 → 只动那一格</li>
 *   <li>伤害数字每次都对照一遍，文字或颜色变了才重画该怪物</li>
 *   <li>玩家实体只挪位置，朝向变了才重画</li>
 * </ul>
 * 实体放在 FXGL 游戏场景的世界层，位于界面层（各块画布）之下，地图区域由 {@code MotaController} 留空透出。
 */
public final class EntityMapView implements MapView {

    private static final int Z_TILE = 0;
    private static final int Z_ITEM = 1;
    private static final int Z_MONSTER = 2;
    private static final int Z_PLAYER = 3;

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
    private boolean visible = true;

    /**
     * @param originX 地图左上角在窗口中的横坐标
     * @param originY 地图左上角在窗口中的纵坐标
     */
    public EntityMapView(GameEngine engine, MapCellPainter painter, double originX, double originY) {
        this.engine = engine;
        this.painter = painter;
        this.originX = originX;
        this.originY = originY;
        this.cellSize = painter.cellSize();
    }

    @Override
    public void sync() {
        GameMap map = engine.getMapManager().getCurrentMap();
        if (map != shownMap) {
            clear();
            shownMap = map;
        }
        tiles.sync(tilesOf(map));
        items.sync(map.getItems());
        monsters.sync(map.getMonsters());

        PlayerStateManager state = engine.getPlayerStateManager();
        monsters.views().forEach(sprite -> sprite.refresh(state));
        syncPlayer(state);
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
    }

    private void clear() {
        tiles.clear();
        items.clear();
        monsters.clear();
    }

    private static Map<Position, Tile> tilesOf(GameMap map) {
        Map<Position, Tile> result = new HashMap<>();
        for (short y = 0; y < MAP_SIDE_LENGTH; y++) {
            for (short x = 0; x < MAP_SIDE_LENGTH; x++) {
                Position pos = new Position(x, y);
                result.put(pos, map.getTileAt(pos));
            }
        }
        return result;
    }

    // ==================== 实体创建 ====================

    private Entity spawnTile(Position pos, Tile tile) {
        Canvas canvas = newCellCanvas();
        painter.paintTile(canvas.getGraphicsContext2D(), tile);
        return spawn(pos, canvas, Z_TILE);
    }

    private Entity spawnItem(Position pos, Item item) {
        Canvas canvas = newCellCanvas();
        painter.paintItem(canvas.getGraphicsContext2D(), item);
        return spawn(pos, canvas, Z_ITEM);
    }

    private MonsterSprite spawnMonster(Position pos, Monster monster) {
        Canvas canvas = newCellCanvas();
        // 伤害数字留给随后的 refresh 画：新建与刷新走同一条路
        return new MonsterSprite(monster, canvas, spawn(pos, canvas, Z_MONSTER));
    }

    private Entity spawn(Position pos, Canvas view, int zIndex) {
        Entity entity = FXGL.entityBuilder()
                .at(xOf(pos), yOf(pos))
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
        return originX + pos.getX_index() * cellSize;
    }

    private double yOf(Position pos) {
        return originY + pos.getY_index() * cellSize;
    }

    // ==================== 玩家 ====================

    private void syncPlayer(PlayerStateManager state) {
        Position pos = engine.getMapManager().getPlayerPosition();
        if (pos == null) {
            return;
        }
        if (player == null) {
            Canvas canvas = newCellCanvas();
            player = new PlayerSprite(canvas, spawn(pos, canvas, Z_PLAYER));
        }
        player.entity().setPosition(xOf(pos), yOf(pos));
        player.face(state.getCurrentDirection());
    }

    /** 玩家实体：朝向变了才重画 */
    private final class PlayerSprite {
        private final Canvas canvas;
        private final Entity entity;
        private Direction shown;

        PlayerSprite(Canvas canvas, Entity entity) {
            this.canvas = canvas;
            this.entity = entity;
        }

        Entity entity() {
            return entity;
        }

        void face(Direction direction) {
            if (direction == shown) {
                return;
            }
            shown = direction;
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, cellSize, cellSize);
            painter.paintPlayer(gc, direction);
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
