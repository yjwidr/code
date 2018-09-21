package com.netbrain.autoupdate.apiagent.entity;

import org.springframework.util.StringUtils;

import com.netbrain.autoupdate.apiagent.proxy.ProxySetting;

public class GetCommand {
    private String currentSoftwareVersion;
    private CurrentContentVersion currentContentVersion;
    private String ieLicenseId;
    private int commandType;
    private Command command;
    private ProxySetting proxySetting;
    public String getCurrentSoftwareVersion() {
        return currentSoftwareVersion;
    }
    public void setCurrentSoftwareVersion(String currentSoftwareVersion) {
        this.currentSoftwareVersion = currentSoftwareVersion;
    }
    public CurrentContentVersion getCurrentContentVersion() {
        return currentContentVersion;
    }
    public void setCurrentContentVersion(CurrentContentVersion currentContentVersion) {
        this.currentContentVersion = currentContentVersion;
    }
    public String getIeLicenseId() {
        return ieLicenseId;
    }
    public void setIeLicenseId(String ieLicenseId) {
        this.ieLicenseId = ieLicenseId;
    }
    public int getCommandType() {
        return commandType;
    }
    public void setCommandType(int commandType) {
        this.commandType = commandType;
    }
    public Command getCommand() {
        return command;
    }
    public void setCommand(Command command) {
        this.command = command;
    }
    public ProxySetting getProxySetting() {
		return proxySetting;
	}
	public void setProxySetting(ProxySetting proxySetting) {
		this.proxySetting = proxySetting;
	}
	public static class Command{
        private String target;
        private int targetMajor;
        private int targetMinor;
        private int targetRevision;
        public int getMajor() {
            return targetMajor;
        }
        public int getMinor() {
            return targetMinor;
        }
        public int getTargetRevision() {
            if(!StringUtils.isEmpty(target)) {
                String[] cv=target.split("\\.");
                if(cv.length==3) {
                    this.targetMajor=Integer.parseInt(cv[0]);
                    this.targetMinor=Integer.parseInt(cv[1]);
                    this.targetRevision=Integer.parseInt(cv[2]);
                }
            }
            return targetRevision;
        }
        private String[] existedVersionRanges;
        public String getTarget() {
            return target;
        }
        public void setTarget(String target) {
            this.target = target;
        }
        public String[] getExistedVersionRanges() {
            return existedVersionRanges;
        }
        public void setExistedVersionRanges(String[] existedVersionRanges) {
            this.existedVersionRanges = existedVersionRanges;
        }
    }
    public static class CurrentContentVersion{
        private int major;
        private int minor;
        private int revision;
        public String getContentVersion() {
            return major+"."+minor+"."+revision;
        }
        public int getMajor() {
            return major;
        }
        public void setMajor(int major) {
            this.major = major;
        }
        public int getMinor() {
            return minor;
        }
        public void setMinor(int minor) {
            this.minor = minor;
        }
        public int getRevision() {
            return revision;
        }
        public void setRevision(int revision) {
            this.revision = revision;
        }
    }

}


