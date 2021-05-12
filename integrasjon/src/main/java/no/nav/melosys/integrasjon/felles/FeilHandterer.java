package no.nav.melosys.integrasjon.felles;

import javax.ws.rs.core.Response;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;

import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SERVER_ERROR;

public interface FeilHandterer {

    default void httpStatusTilException(int status, String feilmelding) {
        if (status == 401 || status == 403) {
            throw new SikkerhetsbegrensningException(feilmelding);
        } else if (status == 404) {
            throw new IkkeFunnetException(feilmelding);
        } else if (Response.Status.Family.familyOf(status) == CLIENT_ERROR) {
            throw new FunksjonellException(feilmelding);
        } else if (Response.Status.Family.familyOf(status) == SERVER_ERROR) {
            throw new TekniskException(feilmelding);
        }
    }

    void håndterEvFeil(Response response);
}
