package no.nav.melosys.tjenester.gui.unntakshandtering;

import javax.ws.rs.core.Response.Status;

import no.nav.melosys.exception.FunksjonellException;

import org.junit.Test;

import static no.nav.melosys.tjenester.gui.unntakshandtering.BaseExceptionMapperTest.testToResponse;


public final class FunksjonellExceptionMapperTest {

    @Test
    public final void toResponseGirInternalServerError() {
        testToResponse(new FunksjonellExceptionMapper(),
                new FunksjonellException("Funker ikke"),
                Status.BAD_REQUEST);
    }

}
