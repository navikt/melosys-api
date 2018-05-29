package no.nav.melosys.saksflyt.impl.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;

import static no.nav.melosys.domain.ProsessDataKey.OPPGAVE_ID;
import static no.nav.melosys.domain.ProsessSteg.JFR_AVSLUTT_OPPGAVE;
import static no.nav.melosys.domain.ProsessSteg.VURDER_INNGANGSVILKÅR;

/**
 * Avslutter en oppgave i GSAK.
 *
 * Transisjoner:
 * JFR_AVSLUTT_OPPGAVE -> VURDER_INNGANGSVILKÅR eller FEILET_MASKINELT hvis feil
 */
@Component
public class AvsluttOppgave extends StandardAbstraktAgent {

    private static final Logger log = LoggerFactory.getLogger(AvsluttOppgave.class);

    GsakFasade gsakFasade;

    @Autowired
    public AvsluttOppgave(Binge binge, ProsessinstansRepository prosessinstansRepo, GsakFasade gsakFasade) {
        super(binge, prosessinstansRepo);
        this.gsakFasade = gsakFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_AVSLUTT_OPPGAVE;
    }

    @Override
    public void utførSteg(Prosessinstans prosessinstans) {
        String oppgaveID = prosessinstans.getData(OPPGAVE_ID);
        try {
            gsakFasade.ferdigstillOppgave(oppgaveID);
        } catch (SikkerhetsbegrensningException e) {
            log.error("Feil i steg {}", inngangsSteg(), e);
            håndterFeil(prosessinstans, false);
        }

        prosessinstans.setSteg(VURDER_INNGANGSVILKÅR);
    }
}
