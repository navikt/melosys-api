package no.nav.melosys.saksflyt.agent.sob;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.AKTØR_ID;
import static no.nav.melosys.domain.ProsessDataKey.SAKSNUMMER;
import static no.nav.melosys.domain.ProsessSteg.*;

/**
 * Steget sørger for å skrive til Sak og Behandling når behandling opprettes
 *
 * Transisjoner:
 * 1) ProsessType.JFR_NY_SAK:
 * STATUS_BEH_OPPR → JFR_OPPDATER_JOURNALPOST eller FEILET_MASKINELT hvis feil
 * 2) ProsessType.JFR_KNYTT:
 * STATUS_BEH_OPPR → GSAK_OPPRETT_OPPGAVE eller FEILET_MASKINELT hvis feil
 */
@Component
public class OppdaterStatusBehandlingOpprettet extends SakOgBehandlingStegBehander {

    private static final Logger log = LoggerFactory.getLogger(OppdaterStatusBehandlingOpprettet.class);

    private final SakOgBehandlingFasade sakOgBehandlingFasade;

    @Autowired
    public OppdaterStatusBehandlingOpprettet(SakOgBehandlingFasade sakOgBehandlingFasade) {
        this.sakOgBehandlingFasade = sakOgBehandlingFasade;
        log.info("OppdaterStatusBehandlingOpprettet initialisert");
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return STATUS_BEH_OPPR;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        String aktørID = prosessinstans.getData(AKTØR_ID);
        String saksnummer = prosessinstans.getData(SAKSNUMMER);
        Behandling behandling = prosessinstans.getBehandling();

        sakOgBehandlingFasade.sendBehandlingOpprettet(lagBehandlingStatusMapper(saksnummer, behandling.getId(), aktørID));

        if (prosessinstans.getType() == ProsessType.JFR_KNYTT) {
            prosessinstans.setSteg(GSAK_OPPRETT_OPPGAVE);
        } else {
            prosessinstans.setSteg(JFR_OPPDATER_JOURNALPOST);
        }
        log.info("Oppdatert sob-status til opprettet for prosessinstans {}", prosessinstans.getId());
    }
}
