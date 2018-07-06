package no.nav.melosys.saksflyt.agent.sbeh;

import java.time.LocalDateTime;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.Fagsystem;
import no.nav.melosys.integrasjon.sakogbehandling.BehandlingStatusMapper;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.AKTØR_ID;
import static no.nav.melosys.domain.ProsessDataKey.SAKSNUMMER;
import static no.nav.melosys.domain.ProsessDataKey.SOB_BEHANDLING_ID;
import static no.nav.melosys.domain.ProsessSteg.FATTET_VEDTAK;
import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;
import static no.nav.melosys.integrasjon.felles.mdc.MDCOperations.generateCallId;

/**
 * Steget sørger for å skrive til Sak og Behandling når behandling opprettes
 *
 * Transisjoner:
 * FATTET_VEDTAK → null hvis alt ok
 * FATTET_VEDTAK → FEILET_MASKINELT hvis oppdatering av status feilet
 */
@Component
public class OppdaterStatusBehandlingAvsluttet extends SakOgBehandlingStegBehander {

    private static final Logger log = LoggerFactory.getLogger(OppdaterStatusBehandlingAvsluttet.class);

    private final SakOgBehandlingClient sakOgBehandlingClient;

    public OppdaterStatusBehandlingAvsluttet(SakOgBehandlingClient sakOgBehandlingClient) {
        this.sakOgBehandlingClient = sakOgBehandlingClient;
        log.info("OppdaterStatusBehandlingAvsluttet initialisert");
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return FATTET_VEDTAK;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws IntegrasjonException {
        log.debug("Starter behandling av {}", prosessinstans.getId());

        String aktørID = prosessinstans.getData(AKTØR_ID);
        String saksnummer = prosessinstans.getData(SAKSNUMMER);
        Behandling behandling = prosessinstans.getBehandling();
        Tema arkivtema = avgjørArkivTema(behandling.getType());

        if (arkivtema == null) {
            String feilmelding = "BehandlingType " + behandling.getType().getBeskrivelse() + " støttes ikke.";
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        String behandlingsId = prosessinstans.getData(SOB_BEHANDLING_ID);
        
        // FIXME: Nullsjekk på noe her?

        BehandlingStatusMapper.Builder builder = new BehandlingStatusMapper.Builder();
        builder.medBehandlingsId(behandlingsId);
        builder.medHendelsesId(generateCallId());
        builder.medSaksnummer(saksnummer);
        builder.medHendelsesprodusent(Fagsystem.MELOSYS.getKode());
        builder.medHendelsestidspunkt(LocalDateTime.now());
        builder.medArkivtema(arkivtema.getKode());
        builder.medAktørID(aktørID);
        builder.medAnsvarligEnhet(Integer.toString(MELOSYS_ENHET_ID));

        sakOgBehandlingClient.sendBehandlingAvsluttet(builder.build());

        prosessinstans.setSteg(null);
        log.info("Oppdatert sob-status til avsluttet for {}", prosessinstans.getId());
    }
}
