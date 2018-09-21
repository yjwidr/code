package com.netbrain.autoupdate.apiagent.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.netbrain.autoupdate.apiagent.config.IEConfig;
import com.netbrain.autoupdate.apiagent.entity.APIResult;
import com.netbrain.autoupdate.apiagent.entity.GetCommand;
import com.netbrain.autoupdate.apiagent.http.handler.HttpHandler;


@Component
public class IEClient {
    @Autowired
    private HttpHandler httpHandler;

    @Autowired
    private IEConfig ieConfig;

    public APIResult<String> upload(String path, byte[] data) throws Exception {
        String result=httpHandler.upload(ieConfig.getUrl(), path, ieConfig.isSsl(),ieConfig.getCertPath(),ieConfig.getKey(),data, null);
        APIResult<String> apiResult =JSON.parseObject(result, new TypeReference<APIResult<String>>(){});
        return apiResult;
    }
    public String post(String path,String json) throws Exception {
        String result=httpHandler.post(ieConfig.getUrl(), path, ieConfig.isSsl(),ieConfig.getCertPath(),ieConfig.getKey(),json, null);
        return result;
    }
    public APIResult<GetCommand> getCommand(String path) throws Exception {
        String result=httpHandler.get(ieConfig.getUrl(), path, ieConfig.isSsl(),ieConfig.getCertPath(),ieConfig.getKey(),null, null);
        return JSON.parseObject(result, new TypeReference<APIResult<GetCommand>>(){});
    }   
    public APIResult<String> versions(String path,String json) throws Exception {
        String result=httpHandler.post(ieConfig.getUrl(), path, ieConfig.isSsl(),ieConfig.getCertPath(),ieConfig.getKey(),json, null);
        APIResult<String> apiResult=JSON.parseObject(result, new TypeReference<APIResult<String>>(){});
        return apiResult;
    }
}
