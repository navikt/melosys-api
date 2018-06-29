package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Map;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.OPPGAVE_ID;
import static no.nav.melosys.domain.ProsessSteg.*;

/**
 * Avslutter en oppgave i GSAK.
 *
 * Transisjoner:
 * JFR_AVSLUTT_OPPGAVE -> JFR_AKTOER_ID eller FEILET_MASKINELT hvis feil
 */
@Component
public class AvsluttOppgave extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AvsluttOppgave.class);

    GsakFasade gsakFasade;


    @Autowired
    public AvsluttOppgave(GsakFasade gsakFasade) {
        this.gsakFasade = gsakFasade;
    }
    
    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_AVSLUTT_OPPGAVE;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Override
    public void utførSteg(Prosessinstans prosessinstans) {
        String oppgaveID = prosessinstans.getData(OPPGAVE_ID);
        try {
            gsakFasade.ferdigstillOppgave(oppgaveID);
        } catch (SikkerhetsbegrensningException e) {
            log.error("...");
            // FIXME: MELOSYS-1316
        }

        ProsessType type = prosessinstans.getType();
        if (ProsessType.JFR_NY_SAK.equals(type)) {
            prosessinstans.setSteg(JFR_AKTOER_ID);
        } else if (ProsessType.JFR_KNYTT.equals(type)) {
            prosessinstans.setSteg(JFR_OPPDATER_JOURNALPOST);
        } else {
            // FIXME: MELOSYS-1316
        }
    }
}
