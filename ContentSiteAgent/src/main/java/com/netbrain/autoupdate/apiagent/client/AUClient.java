package com.netbrain.autoupdate.apiagent.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.netbrain.autoupdate.apiagent.config.AUConfig;
import com.netbrain.autoupdate.apiagent.constant.Constant;
import com.netbrain.autoupdate.apiagent.daemon.Daemon;
import com.netbrain.autoupdate.apiagent.entity.APIResult;
import com.netbrain.autoupdate.apiagent.entity.ContentVersion;
import com.netbrain.autoupdate.apiagent.exception.BusinessException;
import com.netbrain.autoupdate.apiagent.exception.ErrorCodes;
import com.netbrain.autoupdate.apiagent.http.handler.HttpHandler;
import com.netbrain.autoupdate.apiagent.proxy.ProxySetting;

@Component
public class AUClient {
	private static Logger logger = LogManager.getLogger(AUClient.class.getName());
	@Autowired
	private HttpHandler httpHandler;

    @Autowired
    private AUConfig auConfig;

	@Autowired
	private Daemon daemon;

	public byte[] download(String sv, String path, ProxySetting proxySetting) throws Exception {
		byte[] result = httpHandler.download(sv, auConfig.getUrl(), path, auConfig.isSsl(), auConfig.getCertPath(), null, proxySetting);
		if(result != null && result.length > 0) {
			unzipAndWriteToDisk(sv, result);
		}
		return result;
	}

	public APIResult<List<ContentVersion>> detect(String path, String licenseId, ProxySetting proxySetting)
			throws Exception {

		String result = httpHandler.get(auConfig.getUrl(), path, auConfig.isSsl(), auConfig.getCertPath(), null, licenseId, proxySetting);

		try {
			APIResult<List<ContentVersion>> apiResult = JSON.parseObject(result,new TypeReference<APIResult<List<ContentVersion>>>() {});

			return apiResult;
		} catch (Exception ex) {
			logger.error("url:"+auConfig.getUrl()+",path:"+path);
			logger.error("detect::" + result);
			throw ex;
		}
	}
	
	private void unzipAndWriteToDisk(String sv, byte[] bytes) throws Exception {
		ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes));
		Map<String, byte[]> map = new HashMap<>();
		ZipEntry ze = null;
		while ((ze = zis.getNextEntry()) != null) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try{
				int b = -1;
				byte[] buffer = new byte[1024];
				while ((b = zis.read(buffer)) != -1) {
					bos.write(buffer, 0, b);
				}
			}finally {
				bos.close();
			}
			String fileName = ze.getName();
			String cv = fileName.substring(0, fileName.indexOf(Constant.AT));
			String[] cvs = cv.split(Constant.SLA);
			if (cvs.length == 3) {
				// writeBytesToFile(bos.toByteArray(),Constant.PKG+File.separator+sv+File.separator+cvs[0]+Constant.DOT+cvs[1]+File.separator+ze.getName());
				map.put(Constant.PKG + File.separator + sv + File.separator + cvs[0] + Constant.DOT + cvs[1]
						+ File.separator + ze.getName(), bos.toByteArray());
			} else {
				daemon.putFileQueue(map);
				throw new BusinessException(ErrorCodes.ERROR_FILENAME.getCode(),
						String.format(ErrorCodes.ERROR_FILENAME.getMessage(), fileName));
			}
		}
		daemon.putFileQueue(map);
		zis.close();
		return;
	}
}
