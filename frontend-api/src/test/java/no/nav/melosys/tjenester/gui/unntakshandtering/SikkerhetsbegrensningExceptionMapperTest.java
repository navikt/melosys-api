package no.nav.melosys.tjenester.gui.unntakshandtering;

import javax.ws.rs.core.Response.Status;

import no.nav.melosys.exception.SikkerhetsbegrensningException;

import org.junit.Test;

import static no.nav.melosys.tjenester.gui.unntakshandtering.IkkeFunnetExceptionMapperTest.testToResponse;

public final class SikkerhetsbegrensningExceptionMapperTest {

    @Test
    public final void toResponseGirForbiddenStatus() {
        testToResponse(new SikkerhetsbegrensningExceptionMapper(),
                new SikkerhetsbegrensningException("Computer says no."),
                Status.FORBIDDEN);
    }

}
