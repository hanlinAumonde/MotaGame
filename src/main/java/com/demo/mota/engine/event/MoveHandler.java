package com.demo.mota.engine.event;

import com.demo.mota.engine.Item.Item;
import com.demo.mota.engine.enums.Direction;
import com.demo.mota.engine.enums.WallType;
import com.demo.mota.engine.map.GameMap;
import com.demo.mota.engine.map.MapManager;
import com.demo.mota.engine.map.Position;
import com.demo.mota.engine.map.tile.*;
import com.demo.mota.engine.state.PlayerStateManager;

/**
 * 玩家移动事件处理器。
 * 读取玩家移动操作 → 预计算目标坐标 → 根据图格类型分发处理逻辑。
 */
public class MoveHandler {
    private final MapManager mapManager;
    private final PlayerStateManager player;
    private final BattleHandler battleHandler;

    public MoveHandler(MapManager mapManager, PlayerStateManager player) {
        this.mapManager = mapManager;
        this.player = player;
        this.battleHandler = new BattleHandler();
    }

    /**
     * 处理玩家向指定方向移动的请求
     */
    public MoveResult handleMove(Direction direction) {
        // 更新玩家朝向
        player.setCurrentDirection(direction);

        // 计算目标坐标
        Position currentPos = mapManager.getPlayerPosition();
        Position targetPos = currentPos.getAdjacentPosition(direction);

        // 边界检查：如果目标位置和当前位置相同（说明在边界），不移动
        if (targetPos.equals(currentPos)) {
            return MoveResult.NO_MOVE;
        }

        GameMap map = mapManager.getCurrentMap();
        Tile targetTile = map.getTileAt(targetPos);

        // 根据 Tile 类型分发处理
        if (targetTile instanceof BackGroundTile) {
            return handleBackgroundTile(map, targetPos);
        } else if (targetTile instanceof WallTile wallTile) {
            return handleWallTile(wallTile, targetPos);
        } else if (targetTile instanceof DoorTile doorTile) {
            return handleDoorTile(doorTile, targetPos);
        } else if (targetTile instanceof FloorSwitcherTile switcherTile) {
            return handleFloorSwitcher(switcherTile);
        } else if (targetTile instanceof TrickyTile) {
            // 机关墙默认状态下不可通行，和实体墙行为相同
            return MoveResult.BLOCKED;
        }

        return MoveResult.BLOCKED;
    }

    /**
     * 处理背景图格：检查是否有怪物或道具
     */
    private MoveResult handleBackgroundTile(GameMap map, Position targetPos) {
        // 检查是否有怪物
        if (map.getMonsterAt(targetPos) != null) {
            boolean won = battleHandler.executeBattle(player, map, targetPos);
            if (won) {
                // 战斗胜利后检查机关墙
                checkTrickyTiles(map, targetPos);
                // 重新预计算所有怪物伤害（玩家属性可能因升级变化）
                battleHandler.recalculateAllDamage(player, map);
                return MoveResult.BATTLE_WON;
            } else {
                return MoveResult.BATTLE_LOST;
            }
        }

        // 检查是否有道具
        Item item = map.getItemAt(targetPos);
        if (item != null) {
            player.gainItem(item);
            map.removeItemAt(targetPos);
            // 道具可能改变攻防，重新预计算
            battleHandler.recalculateAllDamage(player, map);
            mapManager.setPlayerPosition(targetPos);
            return MoveResult.ITEM_PICKED;
        }

        // 普通移动
        mapManager.setPlayerPosition(targetPos);
        return MoveResult.MOVED;
    }

    /**
     * 处理墙图格：只有暗墙可以交互
     */
    private MoveResult handleWallTile(WallTile wallTile, Position targetPos) {
        if (wallTile.getWallType() == WallType.DARK) {
            // 暗墙：揭示为背景图格，玩家位置不更新
            mapManager.replaceTileWithBackground(targetPos);
            return MoveResult.DARK_WALL_REVEALED;
        }
        return MoveResult.BLOCKED;
    }

    /**
     * 处理门图格：检查是否有足够的钥匙
     */
    private MoveResult handleDoorTile(DoorTile doorTile, Position targetPos) {
        if (player.consumeKey(doorTile.getKeyColor())) {
            // 钥匙足够，开门（门变为背景图格），玩家位置不更新
            mapManager.replaceTileWithBackground(targetPos);
            return MoveResult.DOOR_OPENED;
        }
        return MoveResult.BLOCKED;
    }

    /**
     * 处理楼梯图格：触发楼层切换
     */
    private MoveResult handleFloorSwitcher(FloorSwitcherTile switcherTile) {
        int targetFloor = Integer.parseInt(switcherTile.getAimedFloorId());
        // 切换楼层，玩家出生点由地图文件定义
        mapManager.loadFloor(targetFloor);
        return MoveResult.FLOOR_SWITCHED;
    }

    /**
     * 怪物被击败后，检查所有机关墙是否应该开启
     */
    private void checkTrickyTiles(GameMap map, Position defeatedMonsterPos) {
        for (TrickyTile tricky : map.getAllTrickyTiles()) {
            if (!tricky.isPassable() && tricky.getBoundMonsterIds().isEmpty()) {
                mapManager.replaceTileWithBackground(tricky.getPosition());
            }
        }
    }

    public BattleHandler getBattleHandler() {
        return battleHandler;
    }
}
