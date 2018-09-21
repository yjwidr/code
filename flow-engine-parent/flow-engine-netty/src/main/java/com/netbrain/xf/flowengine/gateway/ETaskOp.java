package com.netbrain.xf.flowengine.gateway;

// This is a replica of ETaskOp defined in NBRMCommon.cs
public enum ETaskOp {
    Task_InvalidOp(0),
    Task_Add(1),
    Task_Mod(2),
    Task_Del(3),
    Task_DelBatch(4),
    Task_Stop(5),
    Task_GetRuntime(6),
    Task_Run(7);

    private int intValue;

    private ETaskOp(int value) {
        this.intValue = value;
    }

    public int getIntValue() {
        return intValue;
    }
}
