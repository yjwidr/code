package com.netbrain.xf.flowengine.fscclient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DTGQueryResponse {
    private List<String> finishedDtgIDs = new ArrayList<>();
    private List<String> notExistDtgIDs = new ArrayList<>();
    private List<String> dtgIDs = new ArrayList<>();

    public List<String> getFinishedDtgIDs() {
        return finishedDtgIDs;
    }

    public void setFinishedDtgIDs(List<String> finishedDtgIDs) {
        this.finishedDtgIDs = finishedDtgIDs;
    }

    public List<String> getNotExistDtgIDs() {
        return notExistDtgIDs;
    }

    public void setNotExistDtgIDs(List<String> notExistDtgIDs) {
        this.notExistDtgIDs = notExistDtgIDs;
    }

    public List<String> getDtgIDs() {
        return dtgIDs;
    }

    public void setDtgIDs(List<String> dtgIDs) {
        this.dtgIDs = dtgIDs;
    }

    private DTGQueryResponse() {

    }

    /**
     * Parse the string response from FSC and set finishedDTG list and notExist DTG lsit
     * @param rawResponse
     * @return
     * @throws Exception when the format of response is invalid
     */
    public static DTGQueryResponse parseResponse(String rawResponse) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String[] segments = rawResponse.split(FSCResponse.FSC_RES_BODY_DELIMITER);

        if (segments.length < 2) {
            throw new Exception("Failed to parse Front Server Controller response " + rawResponse);
        } else {
            DTGQueryResponse dtgQueryResponse = new DTGQueryResponse();
            String body = segments[1];

            Map<String, Object> bodyMap = mapper.readValue(body, new TypeReference<Map<String, Object>>() { });
            if (bodyMap.get("not_exist") != null) {
                dtgQueryResponse.setNotExistDtgIDs((List<String>)bodyMap.get("not_exist"));
            }

            if (bodyMap.get("finish") != null) {
                dtgQueryResponse.setFinishedDtgIDs((List<String>)bodyMap.get("finish"));
            }

            if (bodyMap.get("data_task_group_id") != null) {
                dtgQueryResponse.setDtgIDs((List<String>)bodyMap.get("data_task_group_id"));
            }
            return dtgQueryResponse;
        }
    }
}
