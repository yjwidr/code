package com.netbrain.xf.flowengine.fscclient;

import com.netbrain.ngsystem.model.FSCInfo;
import com.netbrain.ngsystem.model.FrontServerController;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class NetlibClientTest {

    /**
     * This is an integration test and disabled by default.
     * To run this test, make sure you have a running FSC or a running simulated FSC
     * (see com.netbrain.xf.server.simulator.SimpleFrontServerController in test-utils project)
     * @throws Exception
     */
    @Test
    public void testLogin() throws Exception {
        if (true) {
            return;
        }

        FrontServerController fsc = new FrontServerController();
        FSCInfo fscInfo = new FSCInfo();
        fscInfo.setUniqueName("default");
        fscInfo.setIpOrHostname("10.10.0.60");
        fscInfo.setUsername("netbrain");
        fscInfo.setPassword("SLMBPbk5Ou8=");
        fscInfo.setPort(9095);
        List<FSCInfo> fscInfos = new ArrayList<FSCInfo>();
        fscInfos.add(fscInfo);
        fsc.setFscInfo(fscInfos);
        fsc.setActiveFSC("default");
        fsc.setUseSSL(true);
//        fsc.setCertificate("-----BEGIN CERTIFICATE-----\r\n" + 
//                "MIIDjzCCAnegAwIBAgIJALXLE85kOMldMA0GCSqGSIb3DQEBCwUAMF4xCzAJBgNV\r\n" + 
//                "BAYTAmNuMQswCQYDVQQIDAJiajELMAkGA1UEBwwCYmoxETAPBgNVBAoMCG5ldGJy\r\n" + 
//                "YWluMREwDwYDVQQLDAhuZXRicmFpbjEPMA0GA1UEAwwGcm9vdGNhMB4XDTE4MDQy\r\n" + 
//                "NzA4MDM0MVoXDTI4MDQyNDA4MDM0MVowXjELMAkGA1UEBhMCY24xCzAJBgNVBAgM\r\n" + 
//                "AmJqMQswCQYDVQQHDAJiajERMA8GA1UECgwIbmV0YnJhaW4xETAPBgNVBAsMCG5l\r\n" + 
//                "dGJyYWluMQ8wDQYDVQQDDAZyb290Y2EwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAw\r\n" + 
//                "ggEKAoIBAQCghcryxZBD5dSj/c8R1SrS/swAAaLgkkz/HTVhsC0ZYpDFTR51IXSf\r\n" + 
//                "CYWySevVd7WWFMdCfWdOvY2Cb1lL/y6v3DCLeClnjvcAN0U10kY17T9BvRL8uPx0\r\n" + 
//                "kYcthUDMlpkPmDLIvOEQpD8xr+w44QvDL3ZSpRpIV+2KwqvyvvVNX12AXalcKp1E\r\n" + 
//                "uMVmFWjzKpW+PXWPN6o0xTlaTw/TppZiiBIBPoCq/lsIeRddXU1LQPqSa6cPCKwu\r\n" + 
//                "++fBEVrRaahYvj4ItISAuNs3lzn9xgsNukFI2hbmMz2LbWnYKh4Z/IQvb2VmIHBq\r\n" + 
//                "ffg6efGAvqPmt2YhBe6zMZOPkuMxzdt9AgMBAAGjUDBOMB0GA1UdDgQWBBQt3RCN\r\n" + 
//                "H6eI4IQwINStmoWng3HokDAfBgNVHSMEGDAWgBQt3RCNH6eI4IQwINStmoWng3Ho\r\n" + 
//                "kDAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQAihOc0LoKcgEeacLyY\r\n" + 
//                "RiUDNIjyao7D3YiB8xXRK8RqxATV2g7ViYb2H0l6RyFRvJ1c6Xx3hgT23FWEBl5H\r\n" + 
//                "2nMQ6BxqeYU08BAlTgLlz8QwEPTOMZD+W1nbIgVLkZBiGeXhdZnToVtt+fEO6dOl\r\n" + 
//                "i08IBZDYe//OswyJj26yv6kNZN5w4Xi6nubhJvZNmXOtBZUgKYCmyjFfzG8/U+zM\r\n" + 
//                "G4Qkj50Se5BJrfKsHSbrSiV5wCm7y/+LhlvyO+8xOoPqI9H1wd30aEGFFZfzc/Lq\r\n" + 
//                "CPgnvoiVK8Utsyeqd+5+1el6pJk5mTqV7vFM/rGXCkSzl+ZPuoEi7YC5N5gSF75r\r\n" + 
//                "8a3q\r\n" + 
//                "-----END CERTIFICATE-----\r\n" + 
//                "");
        fsc.setCertificate("-----BEGIN CERTIFICATE-----\r\n" + 
                "MIIDpTCCAo2gAwIBAgIBAjANBgkqhkiG9w0BAQsFADBeMQswCQYDVQQGEwJjbjEL\r\n" + 
                "MAkGA1UECAwCYmoxCzAJBgNVBAcMAmJqMREwDwYDVQQKDAhuZXRicmFpbjERMA8G\r\n" + 
                "A1UECwwIbmV0YnJhaW4xDzANBgNVBAMMBnJvb3RjYTAeFw0xODA0MjcwODA4MDFa\r\n" + 
                "Fw0yODA0MjQwODA4MDFaMFExCzAJBgNVBAYTAmNuMQswCQYDVQQIDAJiajERMA8G\r\n" + 
                "A1UECgwIbmV0YnJhaW4xETAPBgNVBAsMCG5ldGJyYWluMQ8wDQYDVQQDDAZzZXJ2\r\n" + 
                "ZXIwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDcPbQcrwIb6AYUux5u\r\n" + 
                "chtlEIT1QgsJNZzIrUlTcvRCvft5NAJXaxCPzns3yQGKWFF5AEurG9mtLO1Uxv/I\r\n" + 
                "KCkGUEZCmaviswatz3ViXnZGVZJcbpZAYCbWjl5ZX0kQlCoh2T8uX1qadBlrnNbY\r\n" + 
                "s/TlVUkDFX0k8HWlCr1NfOGNNWq5rDVrYlOLFabM1qo6wu6gOasWxr88FZmOglkv\r\n" + 
                "ejFsMMzrfi80fyYR8hS/4syciCH2tpOdNmCh+HmGWDTTqqLn1u2++M9WhX8uvu3J\r\n" + 
                "AJqLZ4qjo9aS6TulsSKdtlaMSy9sZic0eom+qo9zji9upy4OI/x7zmpL4+LKyPyd\r\n" + 
                "k29NAgMBAAGjezB5MAkGA1UdEwQCMAAwLAYJYIZIAYb4QgENBB8WHU9wZW5TU0wg\r\n" + 
                "R2VuZXJhdGVkIENlcnRpZmljYXRlMB0GA1UdDgQWBBTKtN8Fh3YmUzW1Q12+xFkG\r\n" + 
                "mC9m9DAfBgNVHSMEGDAWgBQt3RCNH6eI4IQwINStmoWng3HokDANBgkqhkiG9w0B\r\n" + 
                "AQsFAAOCAQEAcjx8DfpH0GJ4bGtsHEsuMP3IigzNywHxysHErMZwVuOwW4eL0ssW\r\n" + 
                "i4jqJtn7Apb7y90slN1V5Xx4rW9mHs9gyKxqf3Gn1PsC5e8khZ6h2ZipRzT+9ITE\r\n" + 
                "QurYVCD/pWXrmhVBotv5g23uYFdn1+McsGjzPJPjrU0laa9vrm4P6ZOE2c8VEGVF\r\n" + 
                "LaQMRq7xkMBr3p4O30VMKiXrHFyXn7p7pWp9qjIyaXQXt4M8LvJBhI7W7hIq9sn7\r\n" + 
                "TuqJWOXfYD6R3o3VBGFYoOZGAlLezxewAcIcoxa/QMlJsE1g7/3s58KEacDFRChV\r\n" + 
                "60yXITBOEmiRSimWz6PVLUUqdTcKcum0+Q==\r\n" + 
                "-----END CERTIFICATE-----");
        fsc.setCertificateType(2);
        fsc.setConductCertAuthVerify(true);
        NetlibClient netlibClient = new NetlibClient(fsc, null);
        try {
            String response = netlibClient.sendCommand(FSCRequest.CMD_fsRunningTaskReq, "");
            DTGQueryResponse dtgQueryResponse = DTGQueryResponse.parseResponse(response);
            System.out.println(dtgQueryResponse.getDtgIDs().size());
            for (String id: dtgQueryResponse.getDtgIDs()) {
                System.out.println(id);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
