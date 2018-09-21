package com.netbrain.autoupdate.apiagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "content.site")
@Validated
public class AUConfig extends BaseConfig {

	@Override
	public String getCertPath() {
		if(this.isSsl()) {
			if(this.getCert_verification().equalsIgnoreCase(SSL_WITH_VERIFY_CERT)) {
				if(this.getType() ==CERTIFICATE_INSTATLL_TYPE_PREPARE) {
					return "";
				}
				return AU_CERT_PATH;
			}
		}
		return null;
	}

}
