package com.netbrain.autoupdate.apiagent.http.handler;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.netbrain.autoupdate.apiagent.constant.Constant;
import com.netbrain.autoupdate.apiagent.exception.BusinessException;
import com.netbrain.autoupdate.apiagent.exception.ErrorCodes;
import com.netbrain.autoupdate.apiagent.http.connection.Connection;
import com.netbrain.autoupdate.apiagent.proxy.ProxySetting;
import com.netbrain.autoupdate.apiagent.utils.CommonUtils;

@Component
public class HttpHandler {
	private static Logger logger = LogManager.getLogger(HttpHandler.class.getName());
	@Autowired
	private Connection connection;

	public byte[] download(String sv, String url, String path, boolean isSsl,String certPath,String apiKey, ProxySetting proxySetting) throws Exception {
		byte[] data = null;
		InputStream is = null;
		HttpURLConnection httpConn = null;
		try {
			if (StringUtils.isEmpty(url)) {
				return data;
			}
			if (url.startsWith("https://") && isSsl) {
				httpConn = connection.connectHttps(url + path, certPath, proxySetting);
			} else if (url.startsWith("http://")) {
				httpConn = connection.connectHttp(url + path, proxySetting);
			} else {
				return data;
			}
			httpConn.setRequestMethod("GET");
			connection.setRequestAuth("netbrain",apiKey, httpConn);
			httpConn.setRequestProperty("Content-Type", "application/octet-stream");
			httpConn.setRequestProperty("Accept-Encoding", "identity");
			int code = httpConn.getResponseCode();
			if (code == 200) {
				httpConn.connect();
				long fileLength = httpConn.getContentLength();
				is = httpConn.getInputStream();
				byte[] fileByte = inputStreamToByte(is, fileLength);
				if (fileByte.length == 0) {
					return fileByte;
				}
				String md5 = httpConn.getHeaderField("md5");
				String md5s = CommonUtils.getMD5ForBytes(fileByte);
				if (md5s.equals(md5)) {
					logger.debug("file length={} ,md5={},md5s={},fileLength={}", fileByte.length, md5, md5s,
							fileLength);
					data = fileByte;
				} else {
					logger.debug("md5 not equals for url={} path={}", url, path);
					throw new BusinessException(ErrorCodes.ERROR_MD5.getCode(),
							String.format(ErrorCodes.ERROR_MD5.getMessage(), url, path));
				}
			} else {
				logger.debug("httpsConn.getResponseCode()={}, url={}, path={}", code, url, path);
				String result = printError(httpConn, path, code);
				throw new BusinessException(ErrorCodes.ERROR_NOT200.getCode(),
						String.format(ErrorCodes.ERROR_NOT200.getMessage(), result, code, url, path));
			}
		} finally {
			try {
				if (is != null)
					is.close();
				if (httpConn != null)
					httpConn.disconnect();
			} catch (Exception e) {
				logger.error(e.getMessage(),e);
			}
		}
		return data;

	}


	private String getSpeed(long size) {
		if (size < 1024) {
			return String.valueOf(size) + Constant.BS;
		} else {
			size = size / 1024;
		}
		if (size < 1024) {
			return String.valueOf(size) + Constant.KBS;
		} else {
			size = size / 1024;
		}
		if (size < 1024) {
			size = size * 100;
			return String.valueOf((size / 100)) + Constant.DOT + String.valueOf((size % 100)) + Constant.MBS;
		} else {
			size = size * 100 / 1024;
			return String.valueOf((size / 100)) + Constant.DOT + String.valueOf((size % 100)) + Constant.GBS;
		}
	}

	public File writeBytesToFile(byte[] b, String outputFile) throws IOException {
		File file = null;
		FileOutputStream os = null;
		try {
			file = new File(outputFile);
			File fileParent = file.getParentFile();
			if (!fileParent.exists()) {
				fileParent.mkdirs();
			}
			if (!file.exists()) {
				os = new FileOutputStream(file);
				os.write(b);
			}
		} finally {
			try {
				if (os != null) {
					os.close();
				}
			} catch (IOException var12) {
				var12.printStackTrace();
			}
		}
		return file;
	}

	public String get(String url, String path, boolean isSsl,String certPath,String apiKey, String licenseId, ProxySetting proxySetting) throws Exception {
		HttpURLConnection httpConn = null;
		String result = null;
		InputStream is = null;
		ByteArrayOutputStream baos = null;
		try {
			if (StringUtils.isEmpty(url)) {
				return result;
			}
			if (url.startsWith("https://") && isSsl) {
				httpConn = connection.connectHttps(url + path, certPath, proxySetting);
			} else if (url.startsWith("http://")) {
				httpConn = connection.connectHttp(url + path, proxySetting);
			} else {
				return result;
			}
			connection.setRequestAuth(licenseId, apiKey, httpConn);
			httpConn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			httpConn.setRequestProperty("accept", "application/json");
			int code = httpConn.getResponseCode();
			ensureHttp200(httpConn);
			if (200 == code) {
				is = httpConn.getInputStream();
				baos = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int len = 0;
				while (-1 != (len = is.read(buffer))) {
					baos.write(buffer, 0, len);
					baos.flush();
				}
				result = baos.toString("utf-8");
				return result;
			} else {
				result = printError(httpConn, path, code);
			}
		} finally {
			try {
				if (is != null)
					is.close();
				if (baos != null)
					baos.close();
				if (httpConn != null)
					httpConn.disconnect();
			} catch (Exception e) {
				logger.error(e.getMessage(),e);
			}
		}
		return result;
	}


	public String upload(String url, String path, boolean isSsl,String certPath,String apiKey, byte[] data, ProxySetting proxySetting) throws Exception {
		String result = post(url, path, isSsl,certPath,apiKey, null, data, proxySetting);
		return result;
	}

	public String post(String url, String path, boolean isSsl,String certPath,String apiKey, String json, ProxySetting proxySetting) throws Exception {
		String result = post(url, path, isSsl,certPath,apiKey, json, null, proxySetting);
		return result;
	}
	
	private final byte[] inputStreamToByte(InputStream inStream, long fileLength) throws Exception {
		ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
		byte[] buff = new byte[2048];
		int len = 0;
		long diff = 0l;
		long startTime = System.currentTimeMillis();
		while ((len = inStream.read(buff, 0, 2048)) != -1) {
			swapStream.write(buff, 0, len);
		}
		long endTime = System.currentTimeMillis();
		long timeGap = endTime - startTime;
		if (timeGap != 0) {
			diff = fileLength / (endTime - startTime) * 1000;
			logger.debug("avg download spead={}", getSpeed(diff));
		}
		byte[] fileByte = swapStream.toByteArray();
		return fileByte;
	}

	private String printError(HttpURLConnection httpConn, String path, int code) throws Exception {
		String result = null;
		InputStream is = null;
		ByteArrayOutputStream baos = null;
		try {
			is = httpConn.getErrorStream();
			if (is == null)
				is = httpConn.getInputStream();
			baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int len = 0;
			while (-1 != (len = is.read(buffer))) {
				baos.write(buffer, 0, len);
				baos.flush();
			}
			result = baos.toString("utf-8");
			logger.error("requestUrl={},result===={}", path, result);
		} finally {
			if (is != null)
				is.close();
			if (baos != null)
				baos.close();
		}
		return result;
	}

	private String post(String url, String path, boolean isSsl,String certPath,String apiKey, String json, byte[] body_data, ProxySetting proxySetting)
			throws Exception {
		final String END = "\r\n";
		final String PREFIX = "--";
		final String BOUNDARY = "*****";
		HttpURLConnection httpConn = null;
		BufferedInputStream bis = null;
		DataOutputStream dos = null;
		BufferedReader reader = null;
		OutputStreamWriter writer = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			if (StringUtils.isEmpty(url)) {
				logger.debug("url is empth.");
				return "";
			}
			if (url.startsWith("https://") && isSsl) {
				httpConn = connection.connectHttps(url + path, certPath, proxySetting);
			} else if (url.startsWith("http://")) {
				httpConn = connection.connectHttp(url + path, proxySetting);
			} else {
				return "";
			}
			connection.setRequestAuth("", apiKey, httpConn);
			httpConn.setRequestMethod("POST");
			if (body_data != null && body_data.length > 0) {
				httpConn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);
			} else if (!StringUtils.isEmpty(json)) {
				httpConn.setRequestProperty("Content-Type", "application/json");
				httpConn.setRequestProperty("accept", "application/json");
			} else {
				return "";
			}
			httpConn.connect();
			if (body_data != null && body_data.length > 0) {
				dos = new DataOutputStream(httpConn.getOutputStream());
				if (body_data != null && body_data.length > 0) {
					dos.writeBytes(PREFIX + BOUNDARY + END);
					dos.writeBytes(
							"Content-Disposition: form-data; name=\"file\"; filename=\"packagesZip.zip\"" + END);
					dos.writeBytes(END);
					dos.write(body_data);
					dos.writeBytes(END);
				}
				dos.writeBytes(PREFIX + BOUNDARY + PREFIX + END);
				dos.flush();

				ensureHttp200(httpConn);

				byte[] buffer = new byte[8 * 1024];
				int c = 0;
				bis = new BufferedInputStream(httpConn.getInputStream());
				while ((c = bis.read(buffer)) != -1) {
					baos.write(buffer, 0, c);
					baos.flush();
				}
				String resutl = new String(baos.toByteArray());
				if (httpConn.getResponseCode() != 200)
					throw new Exception(String.format("http response error,http code=%s,response content:\r\n%s",
							httpConn.getResponseCode(), resutl));
				return resutl;
			} else if (!StringUtils.isEmpty(json)) {
				writer = new OutputStreamWriter(httpConn.getOutputStream());
				writer.write(json);
				writer.flush();

				ensureHttp200(httpConn);
				String resutl = new String(httpConn.getInputStream().readAllBytes());
				return resutl;
			}else {
				logger.debug("http. null ");
			}
		} finally {
			try {
				if (dos != null)
					dos.close();
				if (bis != null)
					bis.close();
				if (baos != null)
					baos.close();
				if (httpConn != null)
					httpConn.disconnect();
				if (reader != null)
					reader.close();
				if (writer != null)
					writer.close();
			} catch (Exception e) {
				logger.error(e.getMessage(),e);
			}
		}
		return null;
	}

	private void ensureHttp200(HttpURLConnection httpConn) throws IOException, Exception {
		if (httpConn.getResponseCode() != 200) {
			InputStream errorStream = null;
			try {
				errorStream = httpConn.getErrorStream();
				if (errorStream == null)
					errorStream = httpConn.getInputStream();
				throw new Exception(String.format("http response error, http code=%s,url=%s,response content=%s",
						httpConn.getResponseCode(),
						httpConn.getURL(),
						errorStream == null ? "null" : new String(errorStream.readAllBytes(),"utf-8")));
			} finally {
				if (errorStream != null)
					errorStream.close();

			}
		}
	}
}
