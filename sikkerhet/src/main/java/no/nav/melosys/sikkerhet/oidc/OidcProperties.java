package no.nav.melosys.sikkerhet.oidc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "security.oidc")
public class OidcProperties {

    private String username;
    private String password;
    private String discoveryUrl;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDiscoveryUrl() {
        return discoveryUrl;
    }

    public void setDiscoveryUrl(String discoveryUrl) {
        this.discoveryUrl = discoveryUrl;
    }

    @Override
    public String toString() {
        return "OidcProperties{" +
            "username='" + username + '\'' +
            ", password='" + password + '\'' +
            ", discoveryUrl='" + discoveryUrl + '\'' +
            '}';
    }
}
