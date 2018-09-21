package com.netbrain.xf.flowengine.fscclient;

import com.netbrain.ngsystem.model.FSCInfo;
import com.netbrain.ngsystem.model.FrontServerController;
import com.netbrain.ngsystem.model.NoActiveFSCException;
import com.sun.jdi.event.ExceptionEvent;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class FSCRequest {
    private static final String FSC_REQ_BODY_DELIMITER = "\r\n\r\n";

    public static final int CMD_fsXFLoginReq = 8000;
    public static final int CMD_fsTaskgroupStatusReq = 8001;
    public static final int CMD_fsStopDTGReq = 8002;
    public static final int CMD_fsRunningTaskReq = 8004;
    public static final int CMD_fsStopAllTaskReq = 8005;

    public String getLoginReq(FrontServerController fsc) throws NoActiveFSCException {
        FSCInfo activeFscInfo = fsc.getActiveFSCInfo();
        StringBuilder loginStringBuilder = new StringBuilder();
        String body = loginStringBuilder.append("{\"userName\":\"")
                .append(activeFscInfo.getUserName())
                .append("\",\"password\":\"")
                .append(activeFscInfo.getPassword())
                .append("\"}")
                .toString();

        return "{\"version\":1,\"protocol\":" + CMD_fsXFLoginReq + ",\"bodysize\":" + body.length() + "}" + FSC_REQ_BODY_DELIMITER + body;
    }

    public String getDTGRequest(String dtgId, int commandType) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
        String currentTime = formatter.format(Instant.now());
        StringBuilder bodyBuilder = new StringBuilder();
        String body = bodyBuilder.append("{\"ids\":[\"")
                .append(dtgId)
                .append("\"],\"time\": \"")
                .append(currentTime)
                .append("\"}")
                .toString();

        String request = "{\"version\":1,\"protocol\":" + commandType + ",\"bodysize\":" + body.length() + "}\r\n\r\n" + body;

        return request;
    }

    public String getRunningDTGsReqeust(int commandType) {
        // issuer 1 means it comes from TaskEngine/WorkerShell evHelper
        String body = "{\"ignore_issuer\":false,\"issuer\":1,\"query_datataskgroup\":true,\"query_direct_live_task\":false}";
        return "{\"version\":1,\"protocol\":" + commandType + ",\"bodysize\":" + body.length() + "}\r\n\r\n" + body;
    }
}
