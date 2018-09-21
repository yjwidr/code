package com.netbrain.autoupdate.apiagent.exception;

public enum ErrorCodes {
    NULL_OBJ(419001, "api=%s return %s is null,licenseId=%s"),
    ERROR_DATA(419002, "api %s APIResult's data is empty,licenseId=%s"),
    ERROR_CCV(419003, "api %s APIResult's data of CurrentContentVersion is empty"),
    ERROR_POSTERROR(419004, "Error when reporting error message"),
    ERROR_INFO(419005, "package can't empty"),
    ERROR_MD5(419006, "md5 not equals for url=%s path=%s"),
    ERROR_NOT200(419007, "result=%s httpstats.code=%s url=%s path=%s"),
    ERROR_500(500, "api=%s runtime exception,licenseId=%s"),
    ERROR_WHO(419008, " code=%s api=%s code is not 0,licenseId=%s"),
    ERROR_LENGTH(419009, "command.target error,must be like x.x.x,api=%s, licenseId=%s"),
    ERROR_FILENAME(419010, "download filename error %s"),
    ERROR_DetectCmd(419011, "detect Command Error,licenseId=%s"),
    ERROR_DownloadCmd(419012, "download Command Error,licenseId=%s"),
	PROXY_ERROR(419999,"proxy error");

    private int code;
    private String message;

    private ErrorCodes(int code, String message) {
        this.setCode(code);
        this.setMessage(message);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "[" + this.code + "]" + this.message;
    }
}