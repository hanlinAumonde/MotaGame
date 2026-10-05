package com.demo.mota.engine.state.equipset;

/**
 * 换上一套装备的结果。除 {@link Status#OK} 外都表示穿戴未被改动。
 *
 * @param skipped 套装里没能穿上的件数（不在背包 / 不满足槽位规则），仅 OK 时有意义
 */
public record EquipSetResult(Status status, int skipped) {

    public enum Status { OK, NOT_SAVED, OUT_OF_RANGE }

    public static final EquipSetResult NOT_SAVED = new EquipSetResult(Status.NOT_SAVED, 0);
    public static final EquipSetResult OUT_OF_RANGE = new EquipSetResult(Status.OUT_OF_RANGE, 0);

    public static EquipSetResult ok(int skipped) {
        return new EquipSetResult(Status.OK, skipped);
    }

    public boolean isOk() {
        return status == Status.OK;
    }
}
