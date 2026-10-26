package com.demo.mota.world;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.pathfinding.CellMoveComponent;
import com.almasb.fxgl.texture.Texture;
import com.demo.mota.app.services.AssetService;
import com.demo.mota.app.services.CharacterSprite;
import com.demo.mota.app.services.SpriteCatalog.CharacterDef;
import com.demo.mota.engine.item.Item;
import com.demo.mota.engine.map.tile.BackGroundTile;
import com.demo.mota.engine.map.tile.DoorTile;
import com.demo.mota.engine.map.tile.FloorSwitcherTile;
import com.demo.mota.engine.map.tile.RegionGateTile;
import com.demo.mota.engine.map.tile.Tile;
import com.demo.mota.engine.map.tile.TrickyTile;
import com.demo.mota.engine.map.tile.WallTile;
import com.demo.mota.engine.state.monster.Monster;
import com.demo.mota.world.component.MonsterComponent;
import com.demo.mota.world.component.PlayerComponent;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.image.Image;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;
import static com.demo.mota.world.WorldGrid.CELL;

/**
 * 地图实体的工厂（FXGL {@link EntityFactory}）：地砖、地物、道具、怪物、主角都经 {@code getGameWorld().spawn(名字, SpawnData)} 生成。
 * 生成位置是世界坐标（格号 × {@link WorldGrid#CELL}），模型对象放在 {@link #MODEL} 里。
 *
 * <p>一格地块拆成<b>两个实体</b>，因为两部分的生命周期不同：
 * <ul>
 *   <li><b>地砖</b>（{@code floor}，模型是底图的精灵 id）：每格一个，开门、揭暗墙都不换——新换上的背景格沿用原来的底图；</li>
 *   <li><b>地物</b>（{@code feature}，模型是非背景格的 {@code Tile}）：墙 / 门 / 楼梯 / 机关墙 / 检测格的前景，
 *       打开时整个实体退场（播消失动画），底下的地砖原样不动。背景格没有地物。</li>
 * </ul>
 *
 * <p>视图：图片一律经 {@link AssetService} 取（已最近邻放大到一格），缺图时用 {@link Placeholders} 的语义占位形状。
 * FXGL 每次 {@code startNewGame()} 都会清空世界里的工厂，所以每局由地图显示重新登记一次。
 */
public final class MotaEntityFactory implements EntityFactory {

    /** SpawnData 里的模型对象（地砖的精灵 id / Tile / Item / Monster） */
    public static final String MODEL = "model";

    /** z 序：地砖 < 地物 < 道具 < 怪物 < 主角 < 盖在一切之上的特效 */
    public static final int Z_FLOOR = 0;
    public static final int Z_FEATURE = 1;
    public static final int Z_ITEM = 2;
    public static final int Z_MONSTER = 3;
    public static final int Z_PLAYER = 4;
    public static final int Z_EFFECT = 5;

    private final AssetService assets;

    public MotaEntityFactory(AssetService assets) {
        this.assets = assets;
    }

    /** 地砖：静态，不参与每帧更新（将来会动的地砖，如岩浆，去掉 neverUpdated） */
    @Spawns("floor")
    public Entity floor(SpawnData data) {
        String sprite = data.get(MODEL);
        return entityBuilder(data)
                .type(EntityType.FLOOR)
                .zIndex(Z_FLOOR)
                .view(imageOr(assets.sprite(sprite), Placeholders.background()))
                .neverUpdated()
                .build();
    }

    /** 地物：墙 / 门 / 楼梯 / 机关墙 / 检测格各取前景图，缺图时用纯色占位 */
    @Spawns("feature")
    public Entity feature(SpawnData data) {
        Tile tile = data.get(MODEL);
        String sprite = switch (tile) {
            case BackGroundTile _ -> throw new IllegalArgumentException("背景格没有地物：" + tile.getPosition());
            case WallTile wall -> wall.getWallResourceId();
            case DoorTile door -> door.getDoorResourceId();
            case FloorSwitcherTile stairs -> stairs.getSwitcherResourceId();
            case TrickyTile tricky -> tricky.getTrickyResourceId();
            case RegionGateTile gate -> gate.getGateResourceId();
        };
        return entityBuilder(data)
                .type(EntityType.FEATURE)
                .zIndex(Z_FEATURE)
                .view(imageOr(assets.sprite(sprite), Placeholders.foreground(tile)))
                .neverUpdated()
                .build();
    }

    @Spawns("item")
    public Entity item(SpawnData data) {
        Item item = data.get(MODEL);
        return entityBuilder(data)
                .type(EntityType.ITEM)
                .zIndex(Z_ITEM)
                .view(imageOr(assets.itemImage(item.getItemId()), Placeholders.item(item)))
                .build();
    }

    @Spawns("monster")
    public Entity monster(SpawnData data) {
        Monster monster = data.get(MODEL);
        MonsterComponent component = new MonsterComponent(monster);
        return entityBuilder(data)
                .type(EntityType.MONSTER)
                .zIndex(Z_MONSTER)
                .view(imageOr(assets.monsterImage(monster.getCharacterId()), Placeholders.monster()))
                .view(component.damageText())
                .with(component)
                .build();
    }

    /**
     * 主角：锚点在格心（{@code CellMoveComponent} 以锚点定位），{@code SpawnData} 的坐标是所在格的左上角。
     * 走一格的速度 = 格宽 / 精灵目录的 {@code stepSeconds}。
     */
    @Spawns("player")
    public Entity player(SpawnData data) {
        CharacterSprite sprite = assets.character(AssetService.PLAYER);
        double stepSeconds = sprite != null ? sprite.stepSeconds() : CharacterDef.DEFAULT_STEP_SECONDS;
        Point2D anchor = new Point2D(CELL / 2.0, CELL / 2.0);
        return entityBuilder()
                .type(EntityType.PLAYER)
                .zIndex(Z_PLAYER)
                .atAnchored(anchor, new Point2D(data.getX(), data.getY()).add(anchor))
                .with(new CellMoveComponent(CELL, CELL, CELL / stepSeconds))
                .with(new PlayerComponent(sprite, Placeholders::player))
                .build();
    }

    /**
     * 有图用图，缺图用占位形状。图片已按原尺寸放大到一格，关掉平滑：窗口放大时像素图仍是锐利的方块，
     * 而不是被插值抹糊。
     */
    private static Node imageOr(Image image, Node placeholder) {
        if (image == null) {
            return placeholder;
        }
        Texture texture = new Texture(image);
        texture.setSmooth(false);
        return texture;
    }
}
