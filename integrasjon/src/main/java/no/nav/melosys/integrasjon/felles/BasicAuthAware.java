package no.nav.melosys.integrasjon.felles;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.core.env.Environment;

public interface BasicAuthAware {
    default String basicAuth() {
        Environment env = EnvironmentHandler.getInstance().getEnv();
        return "Basic " + Base64.getEncoder().encodeToString(
            String.format("%s:%s", env.getRequiredProperty("systemuser.username"), env.getRequiredProperty("systemuser.password"))
                .getBytes(StandardCharsets.UTF_8));
    }
}
