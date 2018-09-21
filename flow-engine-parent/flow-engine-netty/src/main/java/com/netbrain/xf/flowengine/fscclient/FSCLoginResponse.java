package com.netbrain.xf.flowengine.fscclient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FSCLoginResponse {
    public static String[] loginCodeDesc = new String[] {
            "SUCCESS", "FcFAILED", "Invalid Pack Body", "Front Server Not Found", "Invalid Credential"
    };

    private int resCode;

    public int getResCode() {
        return resCode;
    }

    public boolean isSuccess() {
        return (resCode == 0);
    }

    public String getRetCodeDesc() {
        if (resCode >= 0 && resCode < loginCodeDesc.length) {
            return loginCodeDesc[resCode];
        } else {
            return "unknown";
        }
    }

    public static FSCLoginResponse parseLoginResBody(String body) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> bodyMap = mapper.readValue(body.replaceAll("\n", ""), new TypeReference<Map<String, Object>>() { });
        if (bodyMap.get("retCode") != null && bodyMap.get("retCode") instanceof Integer) {
            FSCLoginResponse response = new FSCLoginResponse();
            response.resCode = ((Integer)bodyMap.get("retCode")).intValue();
            return response;
        } else {
            throw new Exception("Failed to parse Front Server Controller login response: " + body);
        }
    }
}
