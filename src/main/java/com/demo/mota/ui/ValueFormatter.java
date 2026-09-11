package com.demo.mota.ui;

import com.demo.mota.engine.GameNumber;

/**
 * 游戏数值的紧凑显示格式化，地图上的伤害数字、状态面板与菜单共用。
 */
public final class ValueFormatter {
    /** 数值单位阶梯，每级相差 10^4：无 / 万 / 亿 / 兆 / 京 / 垓 */
    private static final String[] SCALE_UNITS = {"", "W", "E", "Z", "J", "G"};
    /** 相邻两级单位相差的位数 */
    private static final int UNIT_STEP = 4;
    /** 尾数（含小数点）允许的最大字符数，也是不带单位时的最大位数 */
    public static final int MAX_DIGITS = 5;
    /** 尾数整数部分不超过该位数时，才腾得出小数点和 1 位小数的位置 */
    private static final int FRACTION_INT_LIMIT = MAX_DIGITS - 2;

    private ValueFormatter() {}

    /**
     * 数值收缩显示：{@value #MAX_DIGITS} 位及以下原样，再往上每 {@value #UNIT_STEP}
     * 位换一级单位（{@code W} 万 / {@code E} 亿 / {@code Z} 兆 / {@code J} 京 / {@code G} 垓），
     * 使尾数（含小数点）<b>始终不超过 {@value #MAX_DIGITS} 个字符</b>，
     * 连同单位字母一格内最多 {@value #MAX_DIGITS} + 1 个字符。
     * 例：{@code 99999} → {@code 10W} → {@code 125.3W} → {@code 2501W} → {@code 12345W} → {@code 10E}。
     *
     * <p>小数位受两个条件共同约束：
     * <ol>
     *   <li>尾数整数部分不超过 {@value #FRACTION_INT_LIMIT} 位，否则放不下小数点；</li>
     *   <li>一个完整单位不超过玩家生命上限——否则伤害早已远超可承受范围，这一位只是噪音。</li>
     * </ol>
     * 保留时一律<b>截断</b>到 1 位（不四舍五入，避免 9.9W 进位成 10.0W 造成越级错觉），
     * 该位为 0 时（如 125.01W）整个小数部分省略。
     */
    public static String formatScaled(GameNumber value, GameNumber maxHp) {
        String text = value.toString();
        boolean negative = text.startsWith("-");
        String digits = negative ? text.substring(1) : text;
        int length = digits.length();

        int tier = length <= MAX_DIGITS ? 0
                : Math.min((length - MAX_DIGITS - 1) / UNIT_STEP + 1, SCALE_UNITS.length - 1);
        int intLength = length - tier * UNIT_STEP;
        String body = digits.substring(0, intLength);

        if (tier > 0 && intLength <= FRACTION_INT_LIMIT && unitWithinMaxHp(tier, maxHp)) {
            char fraction = digits.charAt(intLength);
            if (fraction != '0') {
                body = body + "." + fraction;
            }
        }
        return (negative ? "-" : "") + body + SCALE_UNITS[tier];
    }

    /**
     * 第 tier 级单位（10^(tier*{@value #UNIT_STEP})）是否不超过生命上限。
     * 该单位恰是最小的 tier*{@value #UNIT_STEP}+1 位数，故只需比较位数。
     */
    private static boolean unitWithinMaxHp(int tier, GameNumber maxHp) {
        return maxHp != null && maxHp.toString().length() >= tier * UNIT_STEP + 1;
    }
}
