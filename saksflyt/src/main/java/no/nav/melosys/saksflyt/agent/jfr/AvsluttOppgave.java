package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Map;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.ProsessDataKey.OPPGAVE_ID;
import static no.nav.melosys.domain.ProsessSteg.*;

/**
 * Avslutter en oppgave i GSAK.
 * <p>
 * Transisjoner:
 * 1) ProsessType.JFR_NY_SAK:
 *     JFR_AVSLUTT_OPPGAVE -> JFR_AKTØR_ID eller FEILET_MASKINELT hvis feil
 * 2) ProsessType.JFR_KNYTT:
 *     JFR_AVSLUTT_OPPGAVE -> JFR_OPPDATER_JOURNALPOST eller FEILET_MASKINELT hvis feil
 * */
@Component
public class AvsluttOppgave extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AvsluttOppgave.class);

    private final GsakFasade gsakFasade;

    @Autowired
    public AvsluttOppgave(GsakFasade gsakFasade) {
        this.gsakFasade = gsakFasade;
        log.info("AvsluttOppgave initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_AVSLUTT_OPPGAVE;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, SikkerhetsbegrensningException, FunksjonellException, IkkeFunnetException {
        log.debug("Starter behandling av {}", prosessinstans.getId());

        String oppgaveID = prosessinstans.getData(OPPGAVE_ID);
        gsakFasade.ferdigstillOppgave(oppgaveID);

        ProsessType type = prosessinstans.getType();
        if (type == ProsessType.JFR_NY_SAK) {
            prosessinstans.setSteg(JFR_AKTØR_ID);
        } else if (type == ProsessType.JFR_KNYTT) {
            prosessinstans.setSteg(JFR_OPPDATER_JOURNALPOST);
        } else {
            String feilmelding = "Ukjent prosess type: " + type;
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        log.info("Lukket oppgave {} for prosessinstans {}", oppgaveID, prosessinstans.getId());
    }
}
