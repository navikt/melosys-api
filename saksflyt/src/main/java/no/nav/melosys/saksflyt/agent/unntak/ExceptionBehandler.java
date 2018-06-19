package no.nav.melosys.saksflyt.agent.unntak;

import java.io.IOException;

import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;

public class ExceptionBehandler implements UnntakBehandler {
    
    private static Logger logger = LoggerFactory.getLogger(ExceptionBehandler.class);

    private static ExceptionBehandler instanse = new ExceptionBehandler();
    
    private static Retry retryIoFeil = Retry.prøvIgjen(100, 300000);
    private static SettTilFeilet settTilFeilet = SettTilFeilet.settTilFeilet();
    
    private ExceptionBehandler() {
    }
    
    public static ExceptionBehandler exceptionBehandler() {
        return instanse;
    }
    
    @Override
    public void behandleUnntak(Prosessinstans prosessinstans, Throwable t) {
        if (erForårsaketAv(t, Error.class)) {
            settTilFeilet.behandleUnntak(prosessinstans, t);
            throw new RuntimeException("Kritisk feil i jvm", t);
        }
        if (erForårsaketAv(t, IOException.class)) {
            retryIoFeil.behandleUnntak(prosessinstans, t);
            return;
        }
        if (erForårsaketAv(t, DataAccessException.class)) {
            settTilFeilet.behandleUnntak(prosessinstans, t);
            throw new RuntimeException("Kritisk feil ved dataaksess", t);
        }
        if (erForårsaketAv(t, TekniskException.class)) {
            settTilFeilet.behandleUnntak(prosessinstans, t);
            throw new RuntimeException(t);
        }
        if (erForårsaketAv(t, FunksjonellException.class)) {
            settTilFeilet.behandleUnntak(prosessinstans, t);
            return;
        }
        logger.info("Fikk unntak av type {}. Kastes som RTE og topper prosessering i denne tråden", t.getClass().getName());
        settTilFeilet.behandleUnntak(prosessinstans, t);
        throw new RuntimeException(t);
    }
    
    private static boolean erForårsaketAv(Throwable e, Class<? extends Throwable> clzz) {
        if (e == null) return false;
        if (clzz.isInstance(e)) return true;
        return erForårsaketAv(e.getCause(), clzz);
    }

}
