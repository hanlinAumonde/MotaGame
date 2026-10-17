package com.demo.mota.ui.screen.saveload;

/** 存读档界面的用途：同一个界面、同一份档位列表，确认键的含义不同 */
public enum SaveLoadMode {
    /** 把当前进度写入档位（只能从游戏菜单进入，自动存档档位不可写） */
    SAVE("存档"),
    /** 从档位读取进度 */
    LOAD("读档");

    private final String displayName;

    SaveLoadMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
