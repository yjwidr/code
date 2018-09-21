package com.netbrain.autoupdate.apiagent.entity;

public class APIResult<T> {
    private String errorMsg;
    private int resultCode;
    private OperationResult operationResult;
    private T data;
    public String getErrorMsg() {
        return errorMsg;
    }
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
    public int getResultCode() {
        return resultCode;
    }
    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }
    public OperationResult getOperationResult() {
        return operationResult;
    }
    public void setOperationResult(OperationResult operationResult) {
        this.operationResult = operationResult;
    }
    public T getData() {
        return data;
    }
    public void setData(T data) {
        this.data = data;
    }
    public static class OperationResult{
        private int ResultCode;
        private String ResultDesc;
        public int getResultCode() {
            return ResultCode;
        }
        public void setResultCode(int resultCode) {
            ResultCode = resultCode;
        }
        public String getResultDesc() {
            return ResultDesc;
        }
        public void setResultDesc(String resultDesc) {
            ResultDesc = resultDesc;
        }
    }
}

