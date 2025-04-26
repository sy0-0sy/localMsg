package com.sy.localMsg.taskStatus;

public enum TaskStatus {
    INIT,

    SUCCESS,

    FAIL,

    RETRY,
    ;
    public static TaskStatus of(String status) {
        return TaskStatus.valueOf(status);
    }
}
