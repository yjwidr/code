package com.netbrain.xf.flowengine.fscclient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FSCResponse {
    // See fs_protocol.h in NSUtil project
    public static final int fsXFLoginRsp = 8100;
    public static final int fsTaskgroupStatusRsp = 8101;
    public static final int fsStopDTGRsp = 8102;
    public static final int fsXFHeartbeatRsp = 8103;
    public static final int fsRunningTaskRsp = 8104;
    public static final int fsStopAllTaskRsp = 8105;

    public static final String FSC_RES_BODY_DELIMITER = "\r\n\r\n";

    private String header;
    private int responseType;

    /**
     * The size of body. Specified in response header.
     */
    private int bodySize = -1;

    /**
     * The segmented data received from FSC. It includes header, delimiter and body
     */
    private StringBuilder segments = new StringBuilder();

    public String getHeader() {
        return header;
    }

    public int getResponseType() {
        return responseType;
    }

    public void appendSegment(String segment) {
        segments.append(segment);
    }

    public String getBody() {
        if (hasFullBody()) {
            String[] parts = segments.toString().split(FSCResponse.FSC_RES_BODY_DELIMITER);
            if (parts.length >= 2) {
                return parts[1].substring(0, bodySize);
            } else {
                throw new RuntimeException("Invalid format. No body data found in response.");
            }
        } else {
            throw new RuntimeException("Data is incomplete. Received data is less than specified body size");
        }
    }

    public String getFullResponse() {
        return getHeader() + FSCResponse.FSC_RES_BODY_DELIMITER + getBody();
    }

    public int getBodySize() {
        return bodySize;
    }

    public boolean hasFullBody() {
        return (bodySize >= 0 && (segments.length() - header.length() - FSC_RES_BODY_DELIMITER.length()) >= bodySize);
    }

    private boolean hasHeader() {
        return (bodySize > -1);
    }

    /**
     * The response looks as below, please note the double \r\n after each line
     * {"version":1,"protocol":2010,"test":"testdata","bodysize":84}
     *
     * {"not_exist":["912fd6bc-fd84-4bc8-9fa9-330757bb44f1"],"time":"2017-12-18 16:30:28"}
     *
     * It is possible a response is received in several segments.
     * @throws Exception
     */
    public synchronized void parseResponse() throws Exception {
        if (!hasHeader()) {
            String[] parts = segments.toString().split(FSCResponse.FSC_RES_BODY_DELIMITER);

            if (parts.length >= 1) {
                this.header = parts[0];
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> headerMap = mapper.readValue(parts[0], new TypeReference<Map<String, Object>>() {
                });
                if (headerMap.get("protocol") != null && headerMap.get("protocol") instanceof Integer) {
                    this.responseType = ((Integer) headerMap.get("protocol")).intValue();
                    this.bodySize = ((Integer) headerMap.get("bodysize")).intValue();
                } else {
                    throw new Exception("Failed to parse Front Server Controller, invalid header " + parts[0]);
                }
            }
        }
    }
}
