package no.nav.melosys.integrasjon.felles;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.ws.rs.NotSupportedException;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.felles.mdc.MDCOperations;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;

public interface RestConsumer {

    default String basicAuth() {
        return "Basic " + Base64.getEncoder().encodeToString(
            String.format("%s:%s", getEnv().getRequiredProperty("systemuser.username"), getEnv().getRequiredProperty("systemuser.password"))
                .getBytes(StandardCharsets.UTF_8));
    }

    default String getAuth(String scope) {
        if (ThreadLocalAccessInfo.shouldUseSystemToken()) {
            return basicAuth();
        }
        if (scope == null) {
            throw new NotSupportedException("Prøver å hente autoriseringstoken for bruker, men ingen scope har blitt angitt.");
        }
        // Prøver å få token fra STS.. HER MÅ VI ALTSÅ FÅ NY TOKEN FRA AZURE AD
        return "Bearer " + SubjectHandler.getInstance().getOidcTokenString();
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
    }

    default String getUserID() {
        return SubjectHandler.getInstance().getUserID();
    }

    default boolean isSystem() {
        return ThreadLocalAccessInfo.shouldUseSystemToken();
    }

    default RuntimeException tilException(String feilmelding, HttpStatus status) {
        if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
            return new SikkerhetsbegrensningException(feilmelding);
        } else if (status == HttpStatus.NOT_FOUND) {
            return new IkkeFunnetException(feilmelding);
        } else if (status.is4xxClientError()) {
            return new FunksjonellException(feilmelding);
        } else {
            throw new TekniskException(feilmelding);
        }
    }
}
