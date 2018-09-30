package com.xxx.autoupdate.apiserver.progress;

public class Progress {
    private long pBytesRead = 0L; 
    private long pContentLength = 0L; 
    private int pItems; 

    public long getpBytesRead() {
        return pBytesRead;
    }

    public void setpBytesRead(long pBytesRead) {
        this.pBytesRead = pBytesRead;
    }

    public long getpContentLength() {
        return pContentLength;
    }

    public void setpContentLength(long pContentLength) {
        this.pContentLength = pContentLength;
    }

    public int getpItems() {
        return pItems;
    }

    public void setpItems(int pItems) {
        this.pItems = pItems;
    }

    @Override
    public String toString() {
        float tmp = (float) pBytesRead;
        float result = tmp / pContentLength * 100;
        return "ProgressEntity [pBytesRead=" + pBytesRead + ", pContentLength=" + pContentLength + ", percentage="
                + result + "% , pItems=" + pItems + "]";
    }
}
