package no.nav.melosys.tjenester.gui.unntakshandtering;

import javax.ws.rs.core.Response.Status;

import no.nav.melosys.exception.TekniskException;

import org.junit.Test;

import static no.nav.melosys.tjenester.gui.unntakshandtering.IkkeFunnetExceptionMapperTest.testToResponse;

public class TekniskExceptionMapperTest {

    @Test
    public void toResponseGirInternalServerError() {
        testToResponse(new TekniskExceptionMapper(),
                new TekniskException("Computer says no."),
                Status.INTERNAL_SERVER_ERROR);
    }

}
