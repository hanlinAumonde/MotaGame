package com.demo.mota.hud;

import com.almasb.fxgl.core.collection.PropertyMap;
import com.almasb.fxgl.dsl.FXGL;
import com.demo.mota.engine.GameNumber;
import com.demo.mota.engine.enums.KeyColor;
import com.demo.mota.engine.map.region.RegionInfo;
import com.demo.mota.engine.session.GameSession;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.ui.ValueFormatter;

import java.util.Map;

/**
 * 对局 HUD 的视图模型 = FXGL 的世界变量（world vars）。{@link #declare} 在 {@code initGameVars} 里声明这些变量，
 * {@link #update} 在每次行动 / 回到对局后把当前这一局写进去，状态栏的节点绑定它们（{@link StatusPanel}）。
 *
 * <p>存的是<b>显示用的值</b>：生命、攻击这类数值已经按收缩记法格式化（{@link ValueFormatter}，以生命上限为基准），
 * 所以别处（将来的成就、对话条件）也能直接读到画面上显示的内容。FXGL 每次 {@code startNewGame()} 都会清空世界变量，
 * 所以每局都要重新声明、状态栏也每局重新绑定。
 */
public final class HudModel {

    public static final String LEVEL = "hud.level";
    public static final String HP = "hud.hp";
    public static final String ATK = "hud.atk";
    public static final String DEF = "hud.def";
    public static final String GOLD = "hud.gold";
    public static final String EXP = "hud.exp";
    public static final String KEYS_YELLOW = "hud.keys.yellow";
    public static final String KEYS_BLUE = "hud.keys.blue";
    public static final String KEYS_RED = "hud.keys.red";
    public static final String REGION = "hud.region";
    public static final String FLOOR = "hud.floor";
    /** 状态栏的一句提示（「击败 史莱姆！」），空串表示不显示 */
    public static final String MESSAGE = "hud.message";

    private HudModel() {
    }

    /** {@code initGameVars} 里调用：声明全部变量及其类型（初值） */
    public static void declare(Map<String, Object> vars) {
        vars.put(LEVEL, 1);
        vars.put(HP, "");
        vars.put(ATK, "");
        vars.put(DEF, "");
        vars.put(GOLD, "");
        vars.put(EXP, "");
        vars.put(KEYS_YELLOW, 0);
        vars.put(KEYS_BLUE, 0);
        vars.put(KEYS_RED, 0);
        vars.put(REGION, "");
        vars.put(FLOOR, 0);
        vars.put(MESSAGE, "");
    }

    /** 把这一局的当前状态写进世界变量；值没变的属性不会通知绑定方 */
    public static void update(GameSession session, String message) {
        PropertyMap vars = FXGL.getWorldProperties();
        PlayerStateManager player = session.player();
        GameNumber maxHp = player.getMaxHP();
        vars.setValue(LEVEL, player.getLevelNumber());
        vars.setValue(HP, ValueFormatter.formatScaled(player.getCurrentHP(), maxHp)
                + "/" + ValueFormatter.formatScaled(maxHp, maxHp));
        vars.setValue(ATK, ValueFormatter.formatScaled(player.getEffectiveATK(), maxHp));
        vars.setValue(DEF, ValueFormatter.formatScaled(player.getEffectiveDEF(), maxHp));
        vars.setValue(GOLD, String.valueOf(player.getCurrentGoldAmount()));
        vars.setValue(EXP, ValueFormatter.formatScaled(player.getCurrentExp(), maxHp));
        vars.setValue(KEYS_YELLOW, player.getKeyCount(KeyColor.YELLOW));
        vars.setValue(KEYS_BLUE, player.getKeyCount(KeyColor.BLUE));
        vars.setValue(KEYS_RED, player.getKeyCount(KeyColor.RED));
        RegionInfo region = session.maps().getCurrentRegion();
        vars.setValue(REGION, region == null ? "" : region.name());
        vars.setValue(FLOOR, session.maps().getCurrentMap().getFloorNumber());
        vars.setValue(MESSAGE, message == null ? "" : message);
    }
}
