package no.nav.melosys.tjenester.gui.unntakshandtering;

import javax.ws.rs.core.Response.Status;

import org.junit.Test;
import org.slf4j.event.Level;

import static no.nav.melosys.tjenester.gui.unntakshandtering.IkkeFunnetExceptionMapperTest.testToResponse;

public final class BaseExceptionMapperTest {

    // TODO: Testnavnet overdriver, men gjenspeiler ambisjonen. Vi kan utvide
    // testen til å sjekke innholdet av en in-memory stream med redirigert
    // logging. I denne omgang ekserserer testen kun en grein som ikke blir
    // berørt av andre tester.
    @Test
    public final void avbilderMedLavtLoggnivåBlirIkkeLogget() {
        BaseExceptionMapper<Throwable> avbilder = new BaseExceptionMapper<Throwable>(Status.METHOD_NOT_ALLOWED, Level.TRACE, "Ooops") {
        };
        testToResponse(avbilder, new Exception("Auuauua"), Status.METHOD_NOT_ALLOWED);
    }

}
