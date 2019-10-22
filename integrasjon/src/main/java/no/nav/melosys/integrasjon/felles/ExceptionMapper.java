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

    public static MelosysException springExTilMelosysEx(RestClientException ex) {
        return springExTilMelosysEx(ex, ex.getMessage());
    }

    public static MelosysException springExTilMelosysEx(RestClientException ex, String feilmelding) {
        if (ex instanceof HttpStatusCodeException) {
            switch (((HttpStatusCodeException)ex).getStatusCode()) {
                case UNAUTHORIZED:
                case FORBIDDEN:
                    return new SikkerhetsbegrensningException(feilmelding, ex);
                case NOT_FOUND:
                    return new IkkeFunnetException(feilmelding, ex);
                case INTERNAL_SERVER_ERROR:
                case BAD_REQUEST:
                case METHOD_NOT_ALLOWED:
                case SERVICE_UNAVAILABLE:
                    return new IntegrasjonException(feilmelding, ex);
                default:
                    return new TekniskException(feilmelding, ex);
            }
        }

        return new TekniskException(feilmelding, ex);
    }
    
}
