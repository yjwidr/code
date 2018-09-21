package com.netbrain.autoupdate.apiagent.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.alibaba.fastjson.JSON;
import com.netbrain.autoupdate.apiagent.client.AUClient;
import com.netbrain.autoupdate.apiagent.client.IEClient;
import com.netbrain.autoupdate.apiagent.constant.Constant;
import com.netbrain.autoupdate.apiagent.entity.APIResult;
import com.netbrain.autoupdate.apiagent.entity.ContentVersion;
import com.netbrain.autoupdate.apiagent.entity.ErrorReport;
import com.netbrain.autoupdate.apiagent.entity.GetCommand;
import com.netbrain.autoupdate.apiagent.entity.GetCommand.Command;
import com.netbrain.autoupdate.apiagent.entity.GetCommand.CurrentContentVersion;
import com.netbrain.autoupdate.apiagent.exception.BusinessException;
import com.netbrain.autoupdate.apiagent.exception.ErrorCodes;
import com.netbrain.autoupdate.apiagent.proxy.ProxySetting;
import com.netbrain.autoupdate.apiagent.utils.CommonUtils;

@Service
public class AgentService {
    private static Logger logger = LogManager.getLogger(AgentService.class.getName());
    @Autowired
    private AUClient auClient;
    @Autowired
    private IEClient ieClient;
    

    public void proxy() throws Exception {
    	
        String path = Constant.Empty;
        logger.debug("start ieClient.getCommand");
        APIResult<GetCommand> apiResult = ieClient.getCommand(Constant.ACD);
        
        validIEApiResult(apiResult, path, Constant.Empty, Constant.EST9, true);
        
        GetCommand rCommand = apiResult.getData();
        int commandType = rCommand.getCommandType();
        if (commandType == 0) {
            return;
        }
        
        String licenseId = rCommand.getIeLicenseId();
        validNotNull(path, licenseId, licenseId, Constant.LID, Constant.EST9);
        licenseId = CommonUtils.base64decode(licenseId);

        validGetCommand(rCommand, path,licenseId,Constant.EST9, commandType);
        
        String sv = rCommand.getCurrentSoftwareVersion();
        CurrentContentVersion ccv = rCommand.getCurrentContentVersion();
        ProxySetting proxySetting = rCommand.getProxySetting();
        if (commandType == 1) {
            try {
                String cv = ccv.getContentVersion();
                path = Constant.CD + Constant.QM + Constant.SV + sv + Constant.AND + Constant.CV + cv;
                logger.debug("start auClient.detect");
                APIResult<List<ContentVersion>> detectr = auClient.detect(path,
                        licenseId, proxySetting);

                validAUApiResult(detectr, path, licenseId, Constant.EST1, false);
                
                List<ContentVersion> cont = detectr.getData();
                path = Constant.ACV;
                String content = JSON.toJSONString(cont);
                logger.debug("start ieClient.versions. version content:" + content);
                APIResult<String> versionsr = ieClient.versions(path, content);
                
                validIEApiResult(versionsr, path, licenseId, Constant.EST1, false);
                
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException(ErrorCodes.PROXY_ERROR.getCode(),
                        String.format(ErrorCodes.ERROR_DetectCmd.getMessage(), ErrorCodes.PROXY_ERROR.getCode(), licenseId), Constant.ET,
                        Constant.EST1, e);
            }
        } else if (commandType == 2) {
            try {
                logger.debug("download start.");
                byte[] downloadData = null;
                Map<String, byte[]> diskData = null;
                Command command = rCommand.getCommand();
                
                validCommand(command, path, licenseId, Constant.EST2);
                
                String[] exists = command.getExistedVersionRanges();
                String target = command.getTarget();
                String[] targetArray = target.split(Constant.SLA);
                Map<String, List<String>> map = getCvrReleationParm(ccv, command, targetArray, exists, sv);
                printCvrParam(sv, ccv.getContentVersion(), map);
                validNotNull(path, licenseId, map, Constant.CRP, Constant.EST2);
                if (!ObjectUtils.isEmpty(map.get(Constant.NDL))) {
                    path = buildPath(sv, map.get(Constant.NDL));
                    long startTime1 = System.currentTimeMillis();
                    logger.debug("start auClient.download");
                    downloadData = auClient.download(sv, path, proxySetting);
                    logger.debug("download elapsed time={}ms", System.currentTimeMillis() - startTime1);
                }
                if (!ObjectUtils.isEmpty(map.get(Constant.DDL))) {
                    diskData = getDontDownloadData((List<String>) map.get(Constant.DDL), sv);
                }
                byte[] mergeData = mergeDiskAndDownload(diskData, downloadData);
                validNotNull(path, licenseId, mergeData, Constant.MD, Constant.EST2);
                long startTime2 = System.currentTimeMillis();
                
                logger.debug("start ieClient.upload");
                APIResult<String> uploadResult = ieClient.upload(Constant.ACP,
                        mergeData);
                logger.debug("upload elapsed time={}ms", System.currentTimeMillis() - startTime2);
                
                validIEApiResult(uploadResult, path, licenseId, Constant.EST2, false);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException(ErrorCodes.PROXY_ERROR.getCode(),
                        String.format(ErrorCodes.ERROR_DownloadCmd.getMessage(), ErrorCodes.PROXY_ERROR.getCode(), licenseId), Constant.ET,
                        Constant.EST2, e);
            }
        }
        return;
    }
    

    private byte[] mergeDiskAndDownload(Map<String, byte[]> map, byte[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(baos);
        ZipInputStream zis = null;
        ZipEntry ze = null;
        try {
            if (map != null) {
                for (Entry<String, byte[]> entry : map.entrySet()) {
                    ZipEntry zipEntry = new ZipEntry(entry.getKey());
                    zipEntry.setSize(entry.getValue().length);
                    out.putNextEntry(zipEntry);
                    out.write(entry.getValue());
                    out.closeEntry();
                }
            }
            if (data != null) {
                zis = new ZipInputStream(new ByteArrayInputStream(data));
                while ((ze = zis.getNextEntry()) != null) {
                    int b = -1;
                    byte[] buffer = new byte[1024];
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    while ((b = zis.read(buffer)) != -1) {
                        bos.write(buffer, 0, b);
                    }
                    ZipEntry zipEntry = new ZipEntry(ze.getName());
                    zipEntry.setSize(ze.getSize());
                    out.putNextEntry(zipEntry);
                    out.write(bos.toByteArray());
                    out.closeEntry();
                    bos.close();
                }
            }
        } finally {
            if (out != null)
                out.close();
            if (baos != null)
                baos.close();
            if (zis != null)
                zis.close();
        }
        return baos.toByteArray();
    }

    private Map<String, byte[]> getDontDownloadData(List<String> diffTargetExists, String currentSoftwareVersion)
            throws Exception {
        Map<String, byte[]> map = new HashMap<>();
        for (String cv : diffTargetExists) {
            String[] cvs = cv.split(Constant.SLA);
            String fileName = Constant.PKG + File.separator + currentSoftwareVersion + File.separator + cvs[0]
                    + Constant.DOT + cvs[1] + File.separator + cv + Constant.AT + currentSoftwareVersion
                    + Constant.CPKG;
            String key = cv + Constant.AT + currentSoftwareVersion + Constant.CPKG;
            map.put(key, getBytesByNio(fileName));
        }
        return map;
    }

    private Map<String, List<String>> getCvrReleationParm(CurrentContentVersion ccv, Command command,
                                                          String[] targetArray, String[] exists, String currentSoftwareVersion) {
        Map<String, List<String>> map = new HashMap<>();
        List<String> targetList = null;
        int targetMajor = Integer.parseInt(targetArray[0]);
        int targetMinor = Integer.parseInt(targetArray[1]);
        int targetRevision = Integer.parseInt(targetArray[2]);
        int currentMajor = ccv.getMajor();
        int currentMinor = ccv.getMinor();
        int curentRevision = ccv.getRevision();
        if (targetMajor != currentMajor || (targetMajor == currentMajor && targetMinor != currentMinor)) {
            targetList = getTargetListFromCurrentAndTarget(targetArray, targetRevision, 0);
            map = getTargetListFromExistsAndDisk(targetList, targetArray, exists, currentSoftwareVersion);
        } else if (targetMajor == currentMajor && targetMinor == currentMinor && targetRevision != curentRevision) {
            targetList = getTargetListFromCurrentAndTarget(targetArray, targetRevision, curentRevision);
            map = getTargetListFromExistsAndDisk(targetList, targetArray, exists, currentSoftwareVersion);
        }
        map.put(Constant.TL, targetList == null ? Collections.emptyList() : targetList);
        map.put(Constant.EXISTS, Arrays.asList(exists));
        return map;
    }

    private List<String> getTargetListFromCurrentAndTarget(String[] targetArray, int targetRevision,
                                                           int curentRevision) {
        List<String> targetList = new ArrayList<>();
        int start = 0;
        if (curentRevision != 0) {
            start = curentRevision + 1;
        }
        for (; start <= targetRevision; start++) {
            targetList.add(targetArray[0] + Constant.DOT + targetArray[1] + Constant.DOT + start);
        }
        return targetList;
    }

    private Map<String, List<String>> getTargetListFromExistsAndDisk(List<String> targetList, String[] targetArray,
                                                                     String[] exists, String currentSoftwareVersion) {
        Map<String, List<String>> map = new HashMap<>();
        List<String> disk = getFileName(Constant.PKG + File.separator + currentSoftwareVersion + File.separator
                + targetArray[0] + Constant.DOT + targetArray[1]);
        List<String> diskCv = getFileNameOnlyCv(disk);
        List<String> diffTargetExists = compare(targetList.toArray(new String[]{}), exists);
        List<String> needDownloadList = compare(diffTargetExists.toArray(new String[]{}),
                diskCv.toArray(new String[]{}));
        List<String> dontDownloadLisi = compare(diffTargetExists.toArray(new String[]{}),
                needDownloadList.toArray(new String[]{}));

        map.put(Constant.DISK, diskCv == null ? Collections.emptyList() : diskCv);
        map.put(Constant.DTE, diffTargetExists == null ? Collections.emptyList() : diffTargetExists);
        map.put(Constant.NDL, needDownloadList == null ? Collections.emptyList() : needDownloadList);
        map.put(Constant.DDL, dontDownloadLisi == null ? Collections.emptyList() : dontDownloadLisi);
        map.put(Constant.DDL, dontDownloadLisi == null ? Collections.emptyList() : dontDownloadLisi);
        return map;
    }

    private List<String> getFileNameOnlyCv(List<String> list) {
        List<String> cv = new ArrayList<>();
        for (String fileName : list) {
            fileName = fileName.substring(0, fileName.indexOf(Constant.AT));
            cv.add(fileName);
        }
        return cv;
    }

    private String buildPath(String currentSoftwareVersion, List<String> needDownloadList) {
        String cvParm = buildCvr(needDownloadList);
        StringBuffer sb = new StringBuffer();
        String path = Constant.Empty;
        if (!cvParm.isEmpty()) {
            path = sb.append(Constant.CDM).append(Constant.QM).append(Constant.SV).append(currentSoftwareVersion)
                    .append(Constant.AND).append(cvParm).toString();
        } else {
            path = sb.append(Constant.CDM).append(Constant.QM).append(Constant.SV).append(currentSoftwareVersion)
                    .toString();
        }
        return path;
    }


    private void validResultCodeNotZero(String path, String licenseId, int code, String errorMessage, int est) {
        if (code != 0) {
            throw new BusinessException(code,
                    String.format(errorMessage + ErrorCodes.ERROR_WHO.getMessage(), code, path, licenseId),
                    Constant.ET, est);
        }
    }
    

    private void validCommand(Command command, String path, String licenseId, int est) {
    	validNotNull(path, licenseId, command, Constant.CMD, est);
        validNotNull(path, licenseId, command.getExistedVersionRanges(), Constant.EXT, est);
        String target = command.getTarget();
        validNotNull(path, licenseId, target, Constant.TGT, est);
        String[] targetArray = target.split(Constant.SLA);
        valieVersionLengthThree(path, licenseId, targetArray.length, est);
    }

    private void valieVersionLengthThree(String path, String licenseId, int length, int est) {
        if (length != 3) {
            throw new BusinessException(ErrorCodes.ERROR_LENGTH.getCode(),
                    String.format(ErrorCodes.ERROR_LENGTH.getMessage(), path, licenseId), Constant.ET, est);
        }
    }

    private void validNotNull(String path, String licenseId, Object obj, String name, int est) {
        if (obj == null) {
            throw new BusinessException(ErrorCodes.NULL_OBJ.getCode(),
                    String.format(ErrorCodes.NULL_OBJ.getMessage(), path, name, licenseId), Constant.ET, est);
        }
    }
    
    private void validGetCommand(GetCommand rCommand, String path,String licenseId,int est, int commandType) throws UnsupportedEncodingException {
    	String sv = rCommand.getCurrentSoftwareVersion();
        validNotNull(path, licenseId, sv, Constant.CTSV, est);
        validNotNull(path, licenseId, sv, Constant.CTSV, est);
        CurrentContentVersion ccv = rCommand.getCurrentContentVersion();
        validNotNull(path, licenseId, ccv, Constant.CTCV, est);
        validNotNull(path, licenseId, ccv.getMajor(), Constant.MA, est);
        validNotNull(path, licenseId, ccv.getMinor(), Constant.MI, est);
        validNotNull(path, licenseId, ccv.getRevision(), Constant.RV, est);
        validNotNull(path, licenseId, commandType, Constant.CT, est);
    }
    
    private <T> void validIEApiResult(APIResult<T> apiResult, String path, String licenseId, int est, boolean checkData) {
    	validNotNull(path, licenseId, apiResult, Constant.Empty, est);
        validResultCodeNotZero(path, licenseId, apiResult.getOperationResult().getResultCode(),
                apiResult.getOperationResult().getResultDesc(), est);
        if(checkData) {
        	validNotNull(path, licenseId, apiResult.getData(), Constant.Empty, est);
        }
    }
    private <T> void validAUApiResult(APIResult<T> apiResult, String path, String licenseId, int est, boolean checkData) {
    	validNotNull(path, licenseId, apiResult, Constant.Empty, est);
        validResultCodeNotZero(path, licenseId, apiResult.getResultCode(),
                apiResult.getErrorMsg(), est);
        if(checkData) {
        	validNotNull(path, licenseId, apiResult.getData(), Constant.Empty, est);
        }
    }

    private String buildCvr(List<String> list) {
        StringBuffer sb = new StringBuffer();
        if (list != null) {
            for (int dx = 0; dx < list.size(); dx++) {
                if (dx != list.size() - 1) {
                    sb.append(Constant.CVR).append(list.get(dx)).append(Constant.AND);
                } else {
                    sb.append(Constant.CVR).append(list.get(dx));
                }
            }
        }
        return sb.toString();
    }

    private <T> List<T> compare(T[] methodAuthorities, T[] userAuthorities) {
        List<T> listUser = Arrays.asList(userAuthorities);
        List<T> listDifferent = new ArrayList<T>();
        for (T t : methodAuthorities) {
            if (!listUser.contains(t)) {
                listDifferent.add(t);
            }
        }
        return listDifferent;
    }

    private byte[] getBytesByNio(String filename) throws IOException {
        Path path = Paths.get(filename);
        if (!Files.exists(path)) {
            throw new FileNotFoundException(filename);
        }
        return Files.readAllBytes(path);
    }

    private List<String> getFileName(String path) {
        File file = new File(path);
        String[] fileName = file.list();
        return fileName == null ? Collections.emptyList() : Arrays.asList(fileName);
    }

    private void printCvrParam(String sv, String cv, Map<String, List<String>> map) {
        StringBuffer sb = new StringBuffer();
        sb.append(Constant.CTCV).append(Constant.EQ).append(cv).append(System.getProperty(Constant.RN));
        sb.append(Constant.CTSV).append(Constant.EQ).append(sv).append(System.getProperty(Constant.RN));
        for (Entry<String, List<String>> entry : map.entrySet()) {
            sb.append(entry.getKey()).append(Constant.EQ).append(entry.getValue())
                    .append(System.getProperty(Constant.RN));
        }
        logger.info(sb.toString());
    }
    
    public void uploadError(Exception e) {
		if (e instanceof BusinessException) {
			BusinessException be = (BusinessException) e;
			e.printStackTrace();
			ErrorReport error = new ErrorReport();
			error.setErrorMsg(be.getMessage());
			error.setErrorType(be.getErrorType());
			error.setErrorSubType(be.getErrorSubType());
			setErrorPost(e, error);
		} else {
			e.printStackTrace();
			ErrorReport error = new ErrorReport();
			error.setErrorMsg(e.getMessage());
			error.setErrorType(Constant.EST9);
			error.setErrorSubType(Constant.EST1);
			setErrorPost(e, error);
		}
	}

	private void setErrorPost(Exception e, ErrorReport error) {
		try {
			logger.error("error post:",e);
			error.setErrorDetails(CommonUtils.getStackMsg(e));
		} catch (IOException e1) {
			logger.error("error1", e1);
		}
		try {
			ieClient.post(Constant.ACE, JSON.toJSONString(error));
		} catch (Exception e1) {
			logger.error("error1", e1);
		}
	}
}
