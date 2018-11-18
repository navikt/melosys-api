package no.nav.melosys.tjenester.gui.unntakshandtering;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import no.nav.melosys.exception.IkkeFunnetException;

import org.slf4j.event.Level;

@Provider
public final class IkkeFunnetExceptionMapper extends BaseExceptionMapper<IkkeFunnetException> {

    public IkkeFunnetExceptionMapper() {
        // TBD: Skal IkkeFunnetException logges?
        // De fleste opprinnelige forekomstene av unntakshåndtering logger
        // ikke, med følgende unntak:
        // * OppgaveTjeneste.mineOppgaver
        // * .hentOppgaver
        super(Status.NOT_FOUND, Level.WARN, "");
    }

    @Override
    public Response toResponse(IkkeFunnetException exception) {
        logger.warn("{} {}", melding, exception.getMessage());
        return Response.status(status).build();
    }

}
