package no.nav.melosys.sikkerhet.sts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StsLogin {
    private final String location;
    private final String username;
    private final String password;

    public StsLogin(
        @Value("${securityTokenService.url}") String location,
        @Value("${systemuser.username}") String username,
        @Value("${systemuser.password}") String password) {
        this.location = location;
        this.username = username;
        this.password = password;
    }

    public String getLocation() {
        return location;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
