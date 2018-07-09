package no.nav.melosys.saksflyt.agent.sbeh;

import java.time.LocalDateTime;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.Fagsystem;
import no.nav.melosys.integrasjon.sakogbehandling.BehandlingStatusMapper;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingClient;
import no.nav.melosys.repository.BehandlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.*;
import static no.nav.melosys.domain.ProsessSteg.JFR_OPPDATER_JOURNALPOST;
import static no.nav.melosys.domain.ProsessSteg.STATUS_BEH_OPPR;
import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;
import static no.nav.melosys.integrasjon.felles.mdc.MDCOperations.generateCallId;

/**
 * Steget sørger for å skrive til Sak og Behandling når behandling opprettes
 *
 * Transisjoner:
 * STATUS_BEH_OPPR → JFR_OPPDATER_JOURNALPOST hvis alt ok
 * STATUS_BEH_OPPR → FEILET_MASKINELT hvis oppdatering av status feilet
 */
@Component
public class OppdaterStatusBehandlingOpprettet extends SakOgBehandlingStegBehander {

    private static final Logger log = LoggerFactory.getLogger(OppdaterStatusBehandlingOpprettet.class);

    private final BehandlingRepository behandlingRepository;

    private final SakOgBehandlingClient sakOgBehandlingClient;

    @Autowired
    public OppdaterStatusBehandlingOpprettet(BehandlingRepository behandlingRepository, SakOgBehandlingClient sakOgBehandlingClient) {
        this.behandlingRepository = behandlingRepository;
        this.sakOgBehandlingClient = sakOgBehandlingClient;
        log.info("OppdaterStatusBehandlingOpprettet initialisert");
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return STATUS_BEH_OPPR;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws IntegrasjonException, TekniskException {
        log.debug("Starter behandling av {}", prosessinstans.getId());

        String aktørID = prosessinstans.getData(AKTØR_ID);
        String saksnummer = prosessinstans.getData(SAKSNUMMER);
        Behandling behandling = prosessinstans.getBehandling();
        Tema arkivtema = avgjørArkivTema(behandling.getType());

        String fagsystemkode = Fagsystem.MELOSYS.getKode();
        String behandlingsId = String.format("%s-%d", fagsystemkode, behandlingRepository.hentNesteSakOgBehandlingSekvensVerdi());
        prosessinstans.setData(SOB_BEHANDLING_ID, behandlingsId);

        // FIXME: Nullsjekk på noe her?

        BehandlingStatusMapper.Builder builder = new BehandlingStatusMapper.Builder();
        builder.medBehandlingsId(behandlingsId);
        builder.medHendelsesId(generateCallId());
        builder.medSaksnummer(saksnummer);
        builder.medHendelsesprodusent(fagsystemkode);
        builder.medHendelsestidspunkt(LocalDateTime.now());
        builder.medArkivtema(arkivtema.getKode());
        builder.medAktørID(aktørID);
        builder.medAnsvarligEnhet(Integer.toString(MELOSYS_ENHET_ID));

        sakOgBehandlingClient.sendBehandlingOpprettet(builder.build());

        prosessinstans.setSteg(JFR_OPPDATER_JOURNALPOST);
        log.info("Oppdatert sob-status til opprettet for {}", prosessinstans.getId());
    }
}
