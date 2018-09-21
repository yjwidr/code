package com.netbrain.ngsystem.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "FrontServerController")
public class FrontServerController {
    private static Logger logger = LogManager.getLogger(FrontServerController.class.getSimpleName());

    private String id;

    private int deployMode;

    private String groupName;

    private String activeFSC;

    private List<FSCInfo> fscInfo = new ArrayList<FSCInfo>();

    private boolean useSSL;
    
    private String certificate;
    
    private int certificateType;
    
    private boolean conductCertAuthVerify;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getDeployMode(){ return this.deployMode; }

    public void setDeployMode(int deployMode){ this.deployMode = deployMode; }

    public String getGroupName() { return groupName; }

    public void setGroupName(String groupName){  this.groupName = groupName; }

    public String getActiveFSC(){ return this.activeFSC; }

    public void setActiveFSC(String activeFSC) { this.activeFSC = activeFSC; }

    public List<FSCInfo> getFscInfo() { return this.fscInfo; }

    public void setFscInfo(List<FSCInfo> fscInfo) { this.fscInfo = fscInfo; }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public int getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(int certificateType) {
        this.certificateType = certificateType;
    }
    
    public boolean isConductCertAuthVerify() {
        return conductCertAuthVerify;
    }

    public void setConductCertAuthVerify(boolean conductCertAuthVerify) {
        this.conductCertAuthVerify = conductCertAuthVerify;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public FSCInfo getActiveFSCInfo() throws NoActiveFSCException
    {
        try {
            if (!StringUtils.isEmpty(this.activeFSC)) {
                for (FSCInfo fscItem : this.getFscInfo()) {
                    if (fscItem.getUniqueName().equalsIgnoreCase(this.activeFSC)) {
                        return fscItem;
                    }
                }
                throw new Exception("Failed to get active FSC because there is no active fsc matched.");
            }
            else{
                return this.getFscInfo().get(0);
            }
        }
        catch (Exception e) {
            logger.warn(e.getMessage());
            throw new NoActiveFSCException(e.getMessage());
        }
    }
}
