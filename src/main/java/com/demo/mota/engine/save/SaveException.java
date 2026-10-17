package com.demo.mota.engine.save;

/** 读档失败：带上档位此刻的状态，界面据此给出对应提示 */
public class SaveException extends Exception {

    private final SaveSlotInfo.Status status;

    public SaveException(SaveSlotInfo.Status status, String message) {
        this(status, message, null);
    }

    public SaveException(SaveSlotInfo.Status status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public SaveSlotInfo.Status getStatus() {
        return status;
    }
}
