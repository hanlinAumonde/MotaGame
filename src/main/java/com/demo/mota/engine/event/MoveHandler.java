package com.demo.mota.engine.event;

import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.enums.WallType;
import com.demo.mota.engine.event.GameEvent.OpenKind;
import com.demo.mota.engine.item.AbilityGem;
import com.demo.mota.engine.item.Item;
import com.demo.mota.engine.item.Portion;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.MapManager;
import com.demo.mota.engine.map.Position;
import com.demo.mota.engine.map.region.RegionClearReport;
import com.demo.mota.engine.map.region.RegionInfo;
import com.demo.mota.engine.map.tile.BackGroundTile;
import com.demo.mota.engine.map.tile.DoorTile;
import com.demo.mota.engine.map.tile.FloorSwitcherTile;
import com.demo.mota.engine.map.tile.RegionGateTile;
import com.demo.mota.engine.map.tile.Tile;
import com.demo.mota.engine.map.tile.TrickyTile;
import com.demo.mota.engine.map.tile.WallTile;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.state.level.LevelUpResult;
import com.demo.mota.engine.state.monster.Monster;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家移动一步的分发：按目标格的类型结算（走动 / 战斗 / 拾取 / 开门 / 换层 / 区域检查），
 * 并按发生顺序记下领域事件（{@link GameEvent}），一起放进 {@link MoveResult}。
 */
public class MoveHandler {
    private final MapManager mapManager;
    private final PlayerStateManager player;
    private final BattleHandler battleHandler;

    /** 当前这一步记下的事件；每一步开始时清空 */
    private final List<GameEvent> events = new ArrayList<>();

    public MoveHandler(MapManager mapManager, PlayerStateManager player) {
        this.mapManager = mapManager;
        this.player = player;
        this.battleHandler = new BattleHandler();
    }

    public MoveResult handleMove(Direction direction) {
        events.clear();
        if (player.getCurrentDirection() != direction) {
            player.setCurrentDirection(direction);
            events.add(new GameEvent.PlayerTurned(direction));
        }

        Position currentPos = mapManager.getPlayerPosition();
        Position targetPos = currentPos.adjacent(direction);
        if (targetPos == null) {
            return result(MoveResult.Type.NO_MOVE);
        }

        GameMap map = mapManager.getCurrentMap();
        Tile targetTile = map.getTileAt(targetPos);

        // 穷尽匹配：新增一种 Tile 却没在这里处理，编译期直接报错，不会静默落到兜底分支
        return switch (targetTile) {
            case BackGroundTile _ -> handleBackgroundTile(map, currentPos, targetPos);
            case WallTile wallTile -> handleWallTile(wallTile, targetPos);
            case DoorTile doorTile -> handleDoorTile(doorTile, targetPos);
            case FloorSwitcherTile switcherTile -> handleFloorSwitcher(switcherTile);
            case TrickyTile _ -> result(MoveResult.Type.BLOCKED);
            case RegionGateTile _ -> handleRegionGate(targetPos);
            // 地图数据有空洞时 getTileAt 会返回 null，与改造前一样按「走不过去」处理
            case null -> result(MoveResult.Type.BLOCKED);
        };
    }

    private MoveResult handleBackgroundTile(GameMap map, Position currentPos, Position targetPos) {
        Monster monster = map.getMonsterAt(targetPos);
        if (monster != null) {
            String monsterName = monster.getCharacterName();
            String monsterId = monster.getCharacterId();
            BattleHandler.BattleReport report = battleHandler.executeBattle(player, map, targetPos);
            events.add(new GameEvent.BattleFought(targetPos, monsterId, report.damage(), report.won()));
            if (!report.won()) {
                return result(MoveResult.Type.BATTLE_LOST, monsterName);
            }
            checkTrickyTiles(map, targetPos);
            map.removeMonsterAt(targetPos);
            events.add(new GameEvent.MonsterRemoved(targetPos, monsterId));
            LevelUpResult levelUp = report.levelUp();
            if (levelUp != null && levelUp.didLevelUp()) {
                events.add(new GameEvent.LeveledUp(levelUp.previousLevel(), levelUp.newLevel(),
                        levelUp.learnedSkillIds()));
            }
            battleHandler.recalculateAllDamage(player, map);
            return result(MoveResult.Type.BATTLE_WON, monsterName);
        }

        Item item = map.getItemAt(targetPos);
        if (item != null) {
            String itemName = item.getItemName();
            player.gainItem(item);
            map.removeItemAt(targetPos);
            events.add(new GameEvent.ItemPicked(targetPos, item.getItemId()));
            if (item instanceof AbilityGem || item instanceof Portion)
                battleHandler.recalculateAllDamage(player, map);
            stepTo(currentPos, targetPos);
            return result(MoveResult.Type.ITEM_PICKED, itemName);
        }

        stepTo(currentPos, targetPos);
        return result(MoveResult.Type.MOVED);
    }

    private void stepTo(Position from, Position to) {
        mapManager.setPlayerPosition(to);
        events.add(new GameEvent.PlayerStepped(from, to));
    }

    private MoveResult handleWallTile(WallTile wallTile, Position targetPos) {
        if (wallTile.getWallType() == WallType.DARK) {
            open(targetPos, OpenKind.DARK_WALL);
            return result(MoveResult.Type.DARK_WALL_REVEALED);
        }
        return result(MoveResult.Type.BLOCKED);
    }

    private MoveResult handleDoorTile(DoorTile doorTile, Position targetPos) {
        KeyColor color = doorTile.getKeyColor();
        if (player.consumeKey(color)) {
            open(targetPos, OpenKind.DOOR);
            String colorName = switch (color) {
                case YELLOW -> "黄";
                case BLUE -> "蓝";
                case RED -> "红";
                case GREEN -> "绿";
            };
            return result(MoveResult.Type.DOOR_OPENED, colorName + "门");
        }
        return result(MoveResult.Type.BLOCKED);
    }

    /**
     * 楼梯：同区域照常切层；通往下一区域时先做出口兜底（检测格都已开启才放行），
     * 放行则卸载当前区域、载入新区域；通往已通过的区域一律挡住（区域单向推进）。
     */
    private MoveResult handleFloorSwitcher(FloorSwitcherTile switcherTile) {
        int targetFloor = switcherTile.getTargetFloor();
        String floorText = "第" + targetFloor + "层";
        return switch (mapManager.relationTo(targetFloor)) {
            case SAME -> {
                arriveAt(targetFloor, switcherTile);
                yield result(MoveResult.Type.FLOOR_SWITCHED, floorText);
            }
            case FORWARD -> {
                if (!mapManager.isRegionExitUnlocked()) {
                    yield result(MoveResult.Type.REGION_EXIT_LOCKED);
                }
                RegionInfo from = mapManager.getCurrentRegion();
                int fromFloor = mapManager.getCurrentMap().getFloorNumber();
                mapManager.enterRegion(mapManager.regionOf(targetFloor).regionId());
                events.add(new GameEvent.RegionEntered(from, mapManager.getCurrentRegion()));
                arriveAt(fromFloor, targetFloor, switcherTile);
                yield result(MoveResult.Type.REGION_ENTERED,
                        mapManager.getCurrentRegion().name() + " · " + floorText);
            }
            case BACKWARD -> result(MoveResult.Type.REGION_BACKWARD_BLOCKED);
        };
    }

    private void arriveAt(int targetFloor, FloorSwitcherTile switcherTile) {
        arriveAt(mapManager.getCurrentMap().getFloorNumber(), targetFloor, switcherTile);
    }

    private void arriveAt(int fromFloor, int targetFloor, FloorSwitcherTile switcherTile) {
        mapManager.switchFloor(targetFloor, switcherTile.getTargetSpawn());
        battleHandler.recalculateAllDamage(player, mapManager.getCurrentMap());
        events.add(new GameEvent.FloorChanged(fromFloor, targetFloor));
        events.add(new GameEvent.PlayerPlaced(targetFloor, switcherTile.getTargetSpawn()));
    }

    /** 区域检测格：做区域结束检查，通过则开启（换成背景），否则挡住并带回剩余数量 */
    private MoveResult handleRegionGate(Position targetPos) {
        RegionClearReport report = mapManager.checkRegionClear();
        if (report.isClear()) {
            open(targetPos, OpenKind.REGION_GATE);
            return result(MoveResult.Type.REGION_GATE_OPENED);
        }
        events.add(new GameEvent.RegionGateRejected(report));
        return result(MoveResult.Type.REGION_GATE_LOCKED,
                "剩余怪物 " + report.totalMonsters() + " 只、能力宝石 " + report.totalGems() + " 颗");
    }

    private void checkTrickyTiles(GameMap map, Position defeatedMonsterPos) {
        for (TrickyTile tricky : map.getAllTrickyTiles()) {
            if (!tricky.isPassable()) {
                boolean shouldOpen = tricky.removeBoundMonster(map.getMonsterAt(defeatedMonsterPos).getCharacterId());
                if (shouldOpen)
                    open(tricky.getPosition(), OpenKind.TRICKY_WALL);
            }
        }
    }

    private void open(Position position, OpenKind kind) {
        mapManager.replaceTileWithBackground(position);
        events.add(new GameEvent.TileOpened(position, kind));
    }

    private MoveResult result(MoveResult.Type type) {
        return result(type, null);
    }

    private MoveResult result(MoveResult.Type type, String detail) {
        return new MoveResult(type, detail, events);
    }

    public BattleHandler getBattleHandler() {
        return battleHandler;
    }
}
