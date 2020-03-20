package no.nav.melosys;

import java.util.ArrayList;
import java.util.List;

public interface Oppstart {

    // Sikkerhet trenger system properties.
    default void loadSystemProperties() {
        List<String> list = new ArrayList<>();

        // Til StsConfigurationUtil
        list.add("securityTokenService.url");
        list.add("systemuser.username");
        list.add("systemuser.password");

        list.forEach(this::loadProperty);
    }

    void loadProperty(String key);
}
