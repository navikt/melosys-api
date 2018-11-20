package no.nav.melosys.tjenester.gui.unntakshandtering;

import javax.ws.rs.core.Response.Status;

import no.nav.melosys.exception.SikkerhetsbegrensningException;

import org.slf4j.event.Level;

public final class SikkerhetsbegrensningExceptionMapper extends BaseExceptionMapper<SikkerhetsbegrensningException> {

    public SikkerhetsbegrensningExceptionMapper() {
        super(Status.FORBIDDEN, Level.WARN, "SikkerhetsbegrensningException: ");
    }

}
