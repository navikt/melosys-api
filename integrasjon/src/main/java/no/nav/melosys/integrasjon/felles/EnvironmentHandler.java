package no.nav.melosys.integrasjon.felles;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentHandler {

    private static class EnvironmentHandlerHolder {
        private static EnvironmentHandler ENV_HANDLER = null;
    }

    private final Environment env;

    @Autowired
    public EnvironmentHandler(Environment environment) {
        this.env = environment;
        EnvironmentHandlerHolder.ENV_HANDLER = this;
    }

    Environment getEnv() {
        return env;
    }

    static EnvironmentHandler getInstance() {
        return EnvironmentHandlerHolder.ENV_HANDLER;
    }
}
