package no.nav.melosys;

import java.util.ArrayList;
import java.util.List;

public interface Oppstart {

    void configureSsl();

    // Sikkerhet trenger system properties.
    default void loadSystemProperties() {
        List<String> list = new ArrayList<>();

        // Til StsConfigurationUtil
        list.add("securityTokenService.url");
        list.add("systemuser.username");
        list.add("systemuser.password");

        // Til LDAP
        list.add("ldap.url");
        list.add("ldap.username");
        list.add("ldap.domain");
        list.add("ldap.password");
        list.add("ldap.user.basedn");

        list.forEach(key -> loadProperty(key));
    }

    void loadProperty(String key);
}
