package no.nav.melosys.integrasjon.gsak.felles;

import javax.ws.rs.core.Response;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;

import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SERVER_ERROR;

public interface GsakConsumer {
    /**
     * Mapper HTTP status til en MelosysException
     */
    default void statusTilException(int status, String feilmelding) throws TekniskException, SikkerhetsbegrensningException, FunksjonellException {
        if (status == 401 || status == 403) {
            throw new SikkerhetsbegrensningException(feilmelding);
        } else if (Response.Status.Family.familyOf(status) == CLIENT_ERROR) {
            throw new FunksjonellException(feilmelding);
        } else if (Response.Status.Family.familyOf(status) == SERVER_ERROR) {
            throw new TekniskException(feilmelding);
        }
    }

    void håndterFeil(Response response) throws TekniskException, SikkerhetsbegrensningException, FunksjonellException;
}
