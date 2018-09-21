package com.netbrain.autoupdate.apiagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "ie.api")
@Validated
public class IEConfig extends BaseConfig {
    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

	@Override
	public String getCertPath() {
		if(this.isSsl()) {
			if(this.getCert_verification().equalsIgnoreCase(SSL_WITH_VERIFY_CERT)) {
				if(this.getType() ==CERTIFICATE_INSTATLL_TYPE_PREPARE) {
					return "";
				}
				return IE_CERT_PATH;
			}
		}
		return null;
	}

}
