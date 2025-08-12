package no.nav.melosys.integrasjon.felles;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

public final class ExceptionMapper {

    private ExceptionMapper() {
        throw new IllegalArgumentException("Utility");
    }

    public static RuntimeException mapException(RestClientException ex) {
        return mapException(ex, ex.getMessage());
    }

    public static RuntimeException mapException(RestClientException ex, String feilmelding) {
        if (ex instanceof HttpStatusCodeException httpStatusCodeException) {
            return switch (HttpStatus.valueOf(httpStatusCodeException.getStatusCode().value())) {
                case FORBIDDEN, UNAUTHORIZED -> new SikkerhetsbegrensningException(feilmelding, ex);
                case NOT_FOUND -> new IkkeFunnetException(feilmelding, ex);
                case BAD_REQUEST, INTERNAL_SERVER_ERROR, METHOD_NOT_ALLOWED, SERVICE_UNAVAILABLE ->
                    throw new IntegrasjonException(feilmelding, ex);
                default -> throw new TekniskException(feilmelding, ex);
            };
        }

        throw new TekniskException(feilmelding, ex);
    }
}
