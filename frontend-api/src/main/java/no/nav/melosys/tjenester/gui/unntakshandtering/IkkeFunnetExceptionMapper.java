package no.nav.melosys.tjenester.gui.unntakshandtering;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import no.nav.melosys.exception.IkkeFunnetException;

import org.slf4j.event.Level;

@Provider
public final class IkkeFunnetExceptionMapper extends BaseExceptionMapper<IkkeFunnetException> {

    public IkkeFunnetExceptionMapper() {
        super(Status.NOT_FOUND, Level.WARN, "");
    }

    @Override
    public Response toResponse(IkkeFunnetException exception) {
        logger.warn("{}", exception.getMessage());
        return Response.status(status).build();
    }

}
