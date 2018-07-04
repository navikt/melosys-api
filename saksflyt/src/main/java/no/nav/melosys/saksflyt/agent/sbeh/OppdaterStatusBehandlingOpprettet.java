package no.nav.melosys.saksflyt.agent.sbeh;

import java.time.LocalDateTime;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.integrasjon.Fagsystem;
import no.nav.melosys.integrasjon.sakogbehandling.BehandlingStatusMapper;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingClient;
import no.nav.melosys.repository.BehandlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.AKTØR_ID;
import static no.nav.melosys.domain.ProsessDataKey.SAKSNUMMER;
import static no.nav.melosys.domain.ProsessDataKey.SOB_BEHANDLING_ID;
import static no.nav.melosys.domain.ProsessSteg.JFR_OPPRETT_GSAK_SAK;
import static no.nav.melosys.domain.ProsessSteg.JFR_OPPRETT_SAK_OG_BEH;
import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;
import static no.nav.melosys.integrasjon.felles.mdc.MDCOperations.generateCallId;

/**
 * Steget sørger for å skrive til Sak og Behandling når behandling opprettes
 *
 * Transisjoner:
 * JFR_OPPRETT_SAK_OG_BEH → JFR_OPPRETT_GSAK_SAK hvis alt ok
 * JFR_OPPRETT_SAK_OG_BEH → FEILET_MASKINELT hvis oppdatering av status feilet
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
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return JFR_OPPRETT_SAK_OG_BEH;
    }

    @Override
    public void utførSteg(Prosessinstans prosessinstans) {
        String aktørID = prosessinstans.getData(AKTØR_ID, String.class);
        String saksnummer = prosessinstans.getData(SAKSNUMMER, String.class);
        Behandling behandling = prosessinstans.getBehandling();
        Tema arkivtema = avgjørArkivtema(behandling.getType());

        if (arkivtema == null) {
            log.error("BehandlingType {} støttes ikke.", behandling.getType().getBeskrivelse());
            prosessinstans.setSteg(ProsessSteg.FEILET_MASKINELT);
            return;
        }

        String fagsystemkode = Fagsystem.MELOSYS.getKode();
        String behandlingsId = String.format("%s-%d", fagsystemkode, behandlingRepository.hentNesteSakOgBehandlingSekvensVerdi());
        prosessinstans.setData(SOB_BEHANDLING_ID, behandlingsId);

        BehandlingStatusMapper.Builder builder = new BehandlingStatusMapper.Builder();
        builder.medBehandlingsId(behandlingsId);
        builder.medHendelsesId(generateCallId());
        builder.medSaksnummer(saksnummer);
        builder.medHendelsesprodusent(fagsystemkode);
        builder.medHendelsestidspunkt(LocalDateTime.now());
        builder.medArkivtema(arkivtema.getKode());
        builder.medAktørID(aktørID);
        builder.medAnsvarligEnhet(Integer.toString(MELOSYS_ENHET_ID));

        // FIXME: MELOSYS-1316 (kaster IntegrasjonException)
        sakOgBehandlingClient.sendBehandlingOpprettet(builder.build());

        prosessinstans.setSteg(JFR_OPPRETT_GSAK_SAK);
    }
}
