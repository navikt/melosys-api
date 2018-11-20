package no.nav.melosys.tjenester.gui.unntakshandtering;

import javax.ws.rs.core.Response.Status;

import no.nav.melosys.exception.FunksjonellException;

import org.slf4j.event.Level;

public final class FunksjonellExceptionMapper extends BaseExceptionMapper<FunksjonellException> {

    public FunksjonellExceptionMapper() {
        super(Status.BAD_REQUEST, Level.ERROR, "FunksjonellException: ");
    }

}
