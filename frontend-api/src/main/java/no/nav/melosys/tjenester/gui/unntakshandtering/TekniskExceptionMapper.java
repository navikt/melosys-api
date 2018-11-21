package no.nav.melosys.tjenester.gui.unntakshandtering;

import javax.ws.rs.core.Response.Status;

import no.nav.melosys.exception.TekniskException;

import org.slf4j.event.Level;

public final class TekniskExceptionMapper extends BaseExceptionMapper<TekniskException> {

    public TekniskExceptionMapper() {
        super(Status.INTERNAL_SERVER_ERROR, Level.ERROR, "Teknisk feil: ");
    }

}
