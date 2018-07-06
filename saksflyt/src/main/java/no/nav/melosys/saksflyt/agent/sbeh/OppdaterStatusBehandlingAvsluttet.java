package no.nav.melosys.saksflyt.agent.sbeh;

import java.time.LocalDateTime;

import no.nav.melosys.domain.*;
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
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return FATTET_VEDTAK;
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

        String behandlingsId = prosessinstans.getData(SOB_BEHANDLING_ID, String.class);

        BehandlingStatusMapper.Builder builder = new BehandlingStatusMapper.Builder();
        builder.medBehandlingsId(behandlingsId);
        builder.medHendelsesId(generateCallId());
        builder.medSaksnummer(saksnummer);
        builder.medHendelsesprodusent(Fagsystem.MELOSYS.getKode());
        builder.medHendelsestidspunkt(LocalDateTime.now());
        builder.medArkivtema(arkivtema.getKode());
        builder.medAktørID(aktørID);
        builder.medAnsvarligEnhet(Integer.toString(MELOSYS_ENHET_ID));

        // FIXME: MELOSYS-1316 (kaster IntegrasjonException)
        sakOgBehandlingClient.sendBehandlingAvsluttet(builder.build());

        prosessinstans.setSteg(null);
    }
}
