package no.nav.melosys.sikkerhet.sts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StsLogin {
    private final String securityTokenServiceUrl;
    private final String username;
    private final String password;
    private final String stsPolicy;

    public StsLogin(
        @Value("${securityTokenService.url}") String securityTokenServiceUrl,
        @Value("${systemuser.username}") String username,
        @Value("${systemuser.password}") String password,
        @Value("${stsPolicy.url}") String stsPolicy) {
        this.securityTokenServiceUrl = securityTokenServiceUrl;
        this.username = username;
        this.password = password;
        this.stsPolicy = stsPolicy;
    }

    public String getSecurityTokenServiceUrl() {
        return securityTokenServiceUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getStsPolicy() {
        return stsPolicy;
    }
}
