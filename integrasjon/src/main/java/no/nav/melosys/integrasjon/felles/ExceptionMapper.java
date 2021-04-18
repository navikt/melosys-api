package no.nav.melosys.integrasjon.felles;

import javax.ws.rs.*;

import no.nav.melosys.exception.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

public final class ExceptionMapper {

    private ExceptionMapper() {
        throw new IllegalArgumentException("Utility");
    }

    /**
     * WebTarget.get kan kaste ProcessingException eller WebApplicationException. Denne metoden kaster en MelosysException, basert på typen til parameteren.
     */
    public static void JaxGetRuntimeExTilMelosysEx(RuntimeException e) throws FunksjonellException, TekniskException {
        if (e instanceof NotAuthorizedException || e instanceof ForbiddenException) {
            throw new SikkerhetsbegrensningException(e.getMessage());
        } else if (e instanceof NotFoundException) {
            throw new IkkeFunnetException(e.getMessage());
        } else if (e instanceof ClientErrorException) {
            throw new FunksjonellException(e.getMessage(), e);
        } else if (e instanceof ServerErrorException || e instanceof ProcessingException) {
            throw new IntegrasjonException(e.getMessage(), e);
        } else {
            // Mæpper alle andre feil, inkl. RuntimeException til TekniskEx
            throw new TekniskException(e.getMessage(), e);
        }
    }

    public static RuntimeException tilException(RestClientException ex) {
        return tilException(ex, ex.getMessage());
    }

    public static RuntimeException tilException(RestClientException ex, String feilmelding) {
        if (ex instanceof HttpStatusCodeException httpStatusCodeException) {
            return switch (httpStatusCodeException.getStatusCode()) {
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
