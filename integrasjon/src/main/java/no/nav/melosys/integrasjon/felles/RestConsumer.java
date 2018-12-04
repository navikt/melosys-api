package no.nav.melosys.integrasjon.felles;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import no.nav.melosys.integrasjon.felles.mdc.MDCOperations;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.core.env.Environment;

public interface RestConsumer {

    String SYSTEM_USERNAME = "systemuser.username";
    String SYSTEM_PASSWORD = "systemuser.password";

    default String basicAuth() {
        return "Basic " + Base64.getEncoder().encodeToString(
            String.format("%s:%s", getEnv().getRequiredProperty(SYSTEM_USERNAME), getEnv().getRequiredProperty(SYSTEM_PASSWORD))
                .getBytes(StandardCharsets.UTF_8));
    }

    default String getAuth() {
        if (isSystem()) {
            return basicAuth();
        } else {
            return "Bearer " + SubjectHandler.getInstance().getOidcTokenString();
        }
    }

    default String getCallID() {
        String callID = MDCOperations.getFromMDC(MDCOperations.MDC_CALL_ID);
        if (callID == null) {
            callID = MDCOperations.generateCallId();
        }
      return callID;
    }

    default Environment getEnv() {
        return EnvironmentHandler.getInstance().getEnv();
    };

    default String getUserID() {
        return SubjectHandler.getInstance().getUserID();
    }

    default boolean isSystem() {
        return false;
    }
}
