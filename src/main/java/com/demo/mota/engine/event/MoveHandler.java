package com.demo.mota.engine.event;

import com.demo.mota.engine.Item.AbilityGem;
import com.demo.mota.engine.Item.Item;
import com.demo.mota.engine.Item.Portion;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.enums.WallType;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.MapManager;
import com.demo.mota.engine.map.Position;
import com.demo.mota.engine.map.tile.*;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.state.monster.Monster;

public class MoveHandler {
    private final MapManager mapManager;
    private final PlayerStateManager player;
    private final BattleHandler battleHandler;

    public MoveHandler(MapManager mapManager, PlayerStateManager player) {
        this.mapManager = mapManager;
        this.player = player;
        this.battleHandler = new BattleHandler();
    }

    public MoveResult handleMove(Direction direction) {
        player.setCurrentDirection(direction);

        Position currentPos = mapManager.getPlayerPosition();
        Position targetPos = currentPos.getAdjacentPosition(direction);

        if (targetPos.equals(currentPos)) {
            return MoveResult.of(MoveResult.Type.NO_MOVE);
        }

        GameMap map = mapManager.getCurrentMap();
        Tile targetTile = map.getTileAt(targetPos);

        // 穷尽匹配：新增一种 Tile 却没在这里处理，编译期直接报错，不会静默落到兜底分支
        return switch (targetTile) {
            case BackGroundTile _ -> handleBackgroundTile(map, targetPos);
            case WallTile wallTile -> handleWallTile(wallTile, targetPos);
            case DoorTile doorTile -> handleDoorTile(doorTile, targetPos);
            case FloorSwitcherTile switcherTile -> handleFloorSwitcher(switcherTile);
            case TrickyTile _ -> MoveResult.of(MoveResult.Type.BLOCKED);
            // 地图数据有空洞时 getTileAt 会返回 null，与改造前一样按「走不过去」处理
            case null -> MoveResult.of(MoveResult.Type.BLOCKED);
        };
    }

    private MoveResult handleBackgroundTile(GameMap map, Position targetPos) {
        Monster monster = map.getMonsterAt(targetPos);
        if (monster != null) {
            String monsterName = monster.getCharacterName();
            boolean won = battleHandler.executeBattle(player, map, targetPos);
            if (won) {
                checkTrickyTiles(map, targetPos);
                map.removeMonsterAt(targetPos);
                battleHandler.recalculateAllDamage(player, map);
                return MoveResult.of(MoveResult.Type.BATTLE_WON, monsterName);
            } else {
                return MoveResult.of(MoveResult.Type.BATTLE_LOST, monsterName);
            }
        }

        Item item = map.getItemAt(targetPos);
        if (item != null) {
            String itemName = item.getItemName();
            player.gainItem(item);
            map.removeItemAt(targetPos);
            if(item instanceof AbilityGem || item instanceof Portion)
                battleHandler.recalculateAllDamage(player, map);
            mapManager.setPlayerPosition(targetPos);
            return MoveResult.of(MoveResult.Type.ITEM_PICKED, itemName);
        }

        mapManager.setPlayerPosition(targetPos);
        return MoveResult.of(MoveResult.Type.MOVED);
    }

    private MoveResult handleWallTile(WallTile wallTile, Position targetPos) {
        if (wallTile.getWallType() == WallType.DARK) {
            mapManager.replaceTileWithBackground(targetPos);
            return MoveResult.of(MoveResult.Type.DARK_WALL_REVEALED);
        }
        return MoveResult.of(MoveResult.Type.BLOCKED);
    }

    private MoveResult handleDoorTile(DoorTile doorTile, Position targetPos) {
        KeyColor color = doorTile.getKeyColor();
        if (player.consumeKey(color)) {
            mapManager.replaceTileWithBackground(targetPos);
            String colorName = switch (color) {
                case YELLOW -> "黄";
                case BLUE -> "蓝";
                case RED -> "红";
                case GREEN -> "绿";
            };
            return MoveResult.of(MoveResult.Type.DOOR_OPENED, colorName + "门");
        }
        return MoveResult.of(MoveResult.Type.BLOCKED);
    }

    private MoveResult handleFloorSwitcher(FloorSwitcherTile switcherTile) {
        int targetFloor = Integer.parseInt(switcherTile.getAimedFloorId());
        mapManager.loadFloor(targetFloor);
        mapManager.setPlayerPosition(switcherTile.getTargetSpawn());
        battleHandler.recalculateAllDamage(player, mapManager.getCurrentMap());
        return MoveResult.of(MoveResult.Type.FLOOR_SWITCHED, "第" + targetFloor + "层");
    }

    private void checkTrickyTiles(GameMap map, Position defeatedMonsterPos) {
        for (TrickyTile tricky : map.getAllTrickyTiles()) {
            if (!tricky.isPassable()) {
                boolean shouldOpen = tricky.removeBoundMonster(map.getMonsterAt(defeatedMonsterPos).getCharacterId());
                if(shouldOpen)
                    mapManager.replaceTileWithBackground(tricky.getPosition());
            }
        }
    }

    public BattleHandler getBattleHandler() {
        return battleHandler;
    }
}
