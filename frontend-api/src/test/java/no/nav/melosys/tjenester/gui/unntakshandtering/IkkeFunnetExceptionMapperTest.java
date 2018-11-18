package no.nav.melosys.tjenester.gui.unntakshandtering;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import no.nav.melosys.exception.IkkeFunnetException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class IkkeFunnetExceptionMapperTest {

    @Rule
    public final ExpectedException kastet = ExpectedException.none();

    @Test
    public final void toResponseKasterNotFoundException() {
        IkkeFunnetException unntak = new IkkeFunnetException("Borte-botte");
        testToResponse(new IkkeFunnetExceptionMapper(), unntak, Status.NOT_FOUND);
    }

    static <T extends Throwable> void testToResponse(ExceptionMapper<T> oversetter, T unntak, Status forventetStatus) {
        Response resultat = oversetter.toResponse(unntak);
        assertEquals(forventetStatus, resultat.getStatusInfo());
    }
}
