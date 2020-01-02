package com.epoint.cleaning.params;

public enum CleanStatus
{
    未清理(0), 正在清洗(1), 异常数据(2), 正式数据(3), 修正数据(4);

    private int value;

    CleanStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}
