package com.netbrain.ngsystem.model;

public class FSCInfo {
    private String uniqueName;

    private String ipOrHostname;

    private int port;

    private String userName;

    private String password;

   public String getUniqueName() {
       return uniqueName;
   }

   public void setUniqueName(String uniqueName) {
       this.uniqueName = uniqueName;
   }

   public String getIpOrHostname() {
       return ipOrHostname;
   }

   public void setIpOrHostname(String ipOrHostname) {
       this.ipOrHostname = ipOrHostname;
   }

   public int getPort() {
       return port;
   }

   public void setPort(int port) {
       this.port = port;
   }

   public String getUserName() {
       return userName;
   }

   public void setUsername(String username) {
       this.userName = username;
   }

   public String getPassword() {
       return password;
   }

   public void setPassword(String password) {
       this.password = password;
   }
}
