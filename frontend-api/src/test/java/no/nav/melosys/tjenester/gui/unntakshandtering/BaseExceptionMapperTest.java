package no.nav.melosys.tjenester.gui.unntakshandtering;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import org.junit.Test;
import org.slf4j.event.Level;

import static org.junit.Assert.assertEquals;

public final class BaseExceptionMapperTest {

    // TODO: Testnavnet overdriver, men gjenspeiler ambisjonen. Vi kan utvide
    // testen til å sjekke innholdet av en in-memory stream med redirigert
    // logging. I denne omgang ekserserer testen kun en grein som ikke blir
    // berørt av andre tester.
    @Test
    public final void avbilderMedLavtLoggnivåBlirIkkeLogget() {
        BaseExceptionMapper<Throwable> avbilder = new BaseExceptionMapper<Throwable>(Status.METHOD_NOT_ALLOWED, Level.TRACE, "Ooops") {
        };
        BaseExceptionMapperTest.testToResponse(avbilder, new Exception("Auuauua"), Status.METHOD_NOT_ALLOWED);
    }

    static <T extends Throwable> void testToResponse(ExceptionMapper<T> oversetter, T unntak, Status forventetStatus) {
        Response resultat = oversetter.toResponse(unntak);
        assertEquals(forventetStatus, resultat.getStatusInfo());
    }

}
