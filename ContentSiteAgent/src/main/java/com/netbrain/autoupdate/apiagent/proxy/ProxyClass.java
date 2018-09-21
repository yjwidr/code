package com.netbrain.autoupdate.apiagent.proxy;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.Base64;

public class ProxyClass {
	public static Proxy getProxy(ProxySetting proxySetting) {
		Proxy proxy = Proxy.NO_PROXY;
		if(proxySetting!=null && proxySetting.isEnableProxy()) {
			proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxySetting.getProxyAddress(), proxySetting.getProxyPort()));
	        Authenticator authenticator = new Authenticator() {
        		@Override
                protected PasswordAuthentication getPasswordAuthentication() {
        			byte[] pwd = Base64.getDecoder().decode(proxySetting.getProxyPassword());
        			char[] pwdCharArr = new String(pwd).toCharArray();
                    return new PasswordAuthentication(proxySetting.getProxyUsername(),pwdCharArr);
                }
            };
            Authenticator.setDefault(authenticator);
		}
		return proxy;
	}
}
