package com.demo.mota.world.component;

import com.almasb.fxgl.entity.component.Component;
import com.demo.mota.engine.state.PlayerStateManager;
import com.demo.mota.engine.state.monster.DamageRange;
import com.demo.mota.engine.state.monster.Monster;
import com.demo.mota.ui.DamagePalette;
import com.demo.mota.ui.Fonts;
import com.demo.mota.ui.TextPainter;
import com.demo.mota.ui.ValueFormatter;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import static com.demo.mota.world.WorldGrid.CELL;

/**
 * 地图上的一只怪物：本体图（由实体工厂挂上）+ 右下角的伤害数字。
 *
 * <p>伤害数字是一个 {@link Text} 节点（等宽字体 + 无模糊的投影，与原来画两遍的阴影一样），
 * {@link #refresh} 只在文字或颜色变了时才改它——每次同步都会逐只对照，绝大多数时候什么都不做。
 */
public final class MonsterComponent extends Component {

    /**
     * 伤害数字字号（相对格宽）。收缩记法已把长度硬约束在 6 个字符内，
     * 故直接取能整格容纳该上限的最大字号并全图统一，避免同层怪物因位数不同而字号参差。
     */
    private static final double FONT_RATIO = 0.26;
    /** 兜底字号下限：仅在超出末级单位等极端情况下才会触发收缩 */
    private static final double FONT_MIN_RATIO = 0.17;
    private static final double PADDING = CELL * 0.05;

    private static final TextPainter PAINTER = new TextPainter();

    private final Monster monster;
    private final Text damage = new Text();
    private DamageLabel shown;

    public MonsterComponent(Monster monster) {
        this.monster = monster;
        damage.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.BLACK, 0, 0, 1, 1));
    }

    public Monster monster() {
        return monster;
    }

    /** 伤害数字节点，由实体工厂叠在本体图之上 */
    public Text damageText() {
        return damage;
    }

    /** 按玩家当前状态更新伤害数字；与上次显示的相同则什么都不做 */
    public void refresh(PlayerStateManager player) {
        DamageLabel label = DamageLabel.of(monster, player);
        if (label.equals(shown)) {
            return;
        }
        shown = label;
        Font font = PAINTER.fitFont(label.text(), Fonts::mono,
                CELL * FONT_RATIO, CELL * FONT_MIN_RATIO, CELL - 2 * PADDING);
        damage.setText(label.text());
        damage.setFont(font);
        damage.setFill(label.color());
        // 右下角对齐：基线贴底边留白，右边缘贴右边留白
        damage.setX(CELL - PADDING - PAINTER.measureWidth(label.text(), font));
        damage.setY(CELL - PADDING);
    }

    @Override
    public boolean isComponentInjectionRequired() {
        return false;
    }

    /**
     * 一只怪物此刻应显示的伤害数字：打不动显示 "???"，会被打死时强制标红；值相等即不必更新。
     */
    public record DamageLabel(String text, Color color) {

        static DamageLabel of(Monster monster, PlayerStateManager player) {
            DamageRange range = monster.getCurrentDamageRange();
            Color color = DamagePalette.colorOf(range, monster.isLethalTo(player));
            String text;
            if (range == DamageRange.OVER_KILL) {
                text = "???";
            } else if (monster.getCurrentDamage() == null) {
                text = "?";
            } else {
                text = ValueFormatter.formatScaled(monster.getCurrentDamage(), player.getMaxHP());
            }
            return new DamageLabel(text, color);
        }
    }
}
