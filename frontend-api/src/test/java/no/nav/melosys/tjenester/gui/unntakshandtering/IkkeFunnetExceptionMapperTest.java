package no.nav.melosys.tjenester.gui.unntakshandtering;

import javax.ws.rs.core.Response.Status;

import no.nav.melosys.exception.IkkeFunnetException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class IkkeFunnetExceptionMapperTest {

    @Rule
    public final ExpectedException kastet = ExpectedException.none();

    @Test
    public final void toResponseKasterNotFoundException() {
        IkkeFunnetException unntak = new IkkeFunnetException("Borte-botte");
        BaseExceptionMapperTest.testToResponse(new IkkeFunnetExceptionMapper(), unntak, Status.NOT_FOUND);
    }
}
