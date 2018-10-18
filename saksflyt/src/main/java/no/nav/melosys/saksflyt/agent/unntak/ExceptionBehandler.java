package no.nav.melosys.saksflyt.agent.unntak;

import java.io.IOException;
import javax.xml.ws.soap.SOAPFaultException;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.*;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedCheckedException;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.DataAccessException;

import static no.nav.melosys.saksflyt.agent.unntak.KjedetUnntakBehandler.først;

public class ExceptionBehandler implements UnntakBehandler {
    
    private static Logger logger = LoggerFactory.getLogger(ExceptionBehandler.class);

    private static ExceptionBehandler instanse = new ExceptionBehandler();
    
    private static Retry retryIoFeil = Retry.prøvIgjen(100, 300000);
    private static UnntakBehandler settTilFeilet = SettTilFeilet.settTilFeilet();
    private static UnntakBehandler funksjonellFeilBehandler = først(OpprettHendelse.opprettHendelse("Funksjonell feil")).så(settTilFeilet);
    private static UnntakBehandler ikkeFunnetBehandler = først(OpprettHendelse.opprettHendelse("Ikke funnet")).så(settTilFeilet);
    private static UnntakBehandler sikkerhetsbegrensningBehandler = først(OpprettHendelse.opprettHendelse("Ingen tilgang")).så(settTilFeilet);
    private static UnntakBehandler tekniskFeilBehandler = først(OpprettHendelse.opprettHendelse("Teknisk feil")).så(settTilFeilet);
    
    private ExceptionBehandler() {
    }
    
    static ExceptionBehandler exceptionBehandler() {
        return instanse;
    }
    
    @Override
    public void behandleUnntak(Prosessinstans prosessinstans, String melding, Throwable t) {
        if (erForårsaketAv(t, Error.class)) {
            settTilFeilet.behandleUnntak(prosessinstans, "Kritisk feil i jvm", t);
            throw new RuntimeException("Kritisk feil i jvm", t);
        }
        if (erForårsaketAv(t, JsonProcessingException.class)) {
            // Disse er subklasser av IOException, men kastes også ved desirialisering internt også. Ønsker ikke retry for disse.  
            tekniskFeilBehandler.behandleUnntak(prosessinstans, melding, t);
            return;
        }
        if (erForårsaketAv(t, IOException.class)) {
            retryIoFeil.behandleUnntak(prosessinstans, melding, t);
            return;
        }
        if (erForårsaketAv(t, DataAccessException.class)) {
            settTilFeilet.behandleUnntak(prosessinstans, melding, t);
            throw new RuntimeException("Kritisk feil ved dataaksess", t);
        }
        if (erForårsaketAv(t, SikkerhetsbegrensningException.class)) {
            sikkerhetsbegrensningBehandler.behandleUnntak(prosessinstans, melding, t);
            return;
        }
        if (erForårsaketAv(t, IkkeFunnetException.class)) {
            ikkeFunnetBehandler.behandleUnntak(prosessinstans, melding, t);
            return;
        }
        if (erForårsaketAv(t, IntegrasjonException.class) || erForårsaketAv(t, SOAPFaultException.class)) {
            tekniskFeilBehandler.behandleUnntak(prosessinstans, melding, t);
            return;
        }
        if (erForårsaketAv(t, FunksjonellException.class)) {
            funksjonellFeilBehandler.behandleUnntak(prosessinstans, melding, t);
            return;
        }
        if (erForårsaketAv(t, TekniskException.class)) {
            tekniskFeilBehandler.behandleUnntak(prosessinstans, melding, t);
            return;
        }
        logger.error("Fikk ukjent unntak av type {}.", t.getClass().getName());
        settTilFeilet.behandleUnntak(prosessinstans, melding, t);
    }
    
    private static boolean erForårsaketAv(Throwable e, Class<? extends Throwable> clzz) {
        if (e == null) return false;
        if (clzz.isInstance(e)) return true;
        if (e instanceof NestedRuntimeException) {
            return ((NestedRuntimeException) e).contains(clzz);
        }
        if (e instanceof NestedCheckedException) {
            return ((NestedCheckedException) e).contains(clzz);
        }
        return erForårsaketAv(e.getCause(), clzz);
    }

}
