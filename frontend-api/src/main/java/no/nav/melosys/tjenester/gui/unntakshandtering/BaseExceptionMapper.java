package no.nav.melosys.tjenester.gui.unntakshandtering;

import java.util.Objects;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

abstract class BaseExceptionMapper<E extends Throwable> implements ExceptionMapper<E> {
    static final Logger logger = LoggerFactory.getLogger(BaseExceptionMapper.class);

    final Status status;
    final Level loggnivå;
    final String melding;

    /**
     * Konstruer en unntaksoversetter.
     *
     * @param status
     *            HTTP-status å returnere (påkrevd).
     * @param loggnivå
     *            nivået å logge unntaket på: <code>WARN</code> eller <code>ERROR</code> for fullstendig logging med Slf4J, <
     *            <code>WARN</code> eller <code>null</code> for å ikke logge noe.
     * @param melding
     *            en frivillig innledning til logging av unntaket (påkrevd). Gir anledning til å prefikse dump av stack trace
     *            med ekstra informasjon.
     */
    BaseExceptionMapper(Status status, Level loggnivå, String melding) {
        Objects.requireNonNull(status);
        Objects.requireNonNull(melding);
        this.status = status;
        this.loggnivå = loggnivå;
        this.melding = melding;
    }

    @Override
    public Response toResponse(E exception) {
        if (loggnivå.equals(Level.ERROR)) {
            logger.error(melding, exception);
        } else if (loggnivå.equals(Level.WARN)) {
            logger.warn(melding, exception);
        }
        return Response.status(status.getStatusCode(), exception.getMessage()).build();
    }

}
