package no.nav.melosys.integrasjon.felles;

import javax.ws.rs.*;

import no.nav.melosys.exception.*;
import org.glassfish.jersey.client.authentication.RequestAuthenticationException;
import org.glassfish.jersey.client.authentication.ResponseAuthenticationException;

public final class ExceptionMapper {

    /**
     * WebTarget.get kan kaste ProcessingException eller WebApplicationException. Denne metoden kaster en MelosysException, basert på typen til parameteren.
     */
    public static void JaxGetRuntimeExTilMelosysEx(RuntimeException e) throws SikkerhetsbegrensningException, IkkeFunnetException, FunksjonellException, TekniskException {
        if (e instanceof RequestAuthenticationException || e instanceof ResponseAuthenticationException || e instanceof ForbiddenException) {
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
    
}
