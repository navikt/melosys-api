package no.nav.melosys.saksflyt.agent.sbeh;

import java.time.LocalDateTime;
import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.Fagsystem;
import no.nav.melosys.integrasjon.sakogbehandling.BehandlingStatusMapper;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingClient;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.BehandlingType.SØKNAD;
import static no.nav.melosys.domain.ProsessDataKey.AKTØR_ID;
import static no.nav.melosys.domain.ProsessDataKey.SAKSNUMMER;
import static no.nav.melosys.domain.ProsessDataKey.SOB_BEHANDLING_ID;
import static no.nav.melosys.domain.ProsessSteg.FERDIG;
import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;
import static no.nav.melosys.integrasjon.felles.mdc.MDCOperations.generateCallId;

/**
 * Steget sørger for å skrive til Sak og Behandling når behandling opprettes
 *
 * Transisjoner:
 * FERDIG → XXX hvis alt ok
 * FERDIG → FEILET_MASKINELT hvis oppdatering av status feilet
 */
@Component
public class OppdaterStatusBehandlingOpprettet extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterStatusBehandlingOpprettet.class);

    private static String ARKIVTEMA_MED = "MED";
    private static String ARKIVTEMA_UFM = "UFM";

    private final BehandlingRepository behandlingRepository;

    private final SakOgBehandlingClient sakOgBehandlingClient;

    @Autowired
    public OppdaterStatusBehandlingOpprettet(BehandlingRepository behandlingRepository, SakOgBehandlingClient sakOgBehandlingClient) {
        this.behandlingRepository = behandlingRepository;
        this.sakOgBehandlingClient = sakOgBehandlingClient;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return FERDIG;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    public void utførSteg(Prosessinstans prosessinstans) {
        String aktørID = prosessinstans.getData(AKTØR_ID, String.class);
        String saksnummer = prosessinstans.getData(SAKSNUMMER, String.class);
        Behandling behandling = prosessinstans.getBehandling();

        String fagsystemkode = Fagsystem.MELOSYS.getKode();
        String behandlingsId = String.format("%s-%d", fagsystemkode, behandlingRepository.hentNesteSakOgBehandlingSekvensVerdi());
        prosessinstans.setData(SOB_BEHANDLING_ID, behandlingsId);

        BehandlingStatusMapper.Builder builder = new BehandlingStatusMapper.Builder();
        builder.medBehandlingsId(behandlingsId);
        builder.medHendelsesId(generateCallId());
        builder.medSaksnummer(saksnummer);
        builder.medHendelsesprodusent(fagsystemkode);
        builder.medHendelsestidspunkt(LocalDateTime.now());
        if (SØKNAD.equals(behandling.getType())) {
            builder.medArkivtema(ARKIVTEMA_MED);
        } else {
            builder.medArkivtema(ARKIVTEMA_UFM);
        }
        builder.medAktørID(aktørID);
        builder.medAnsvarligEnhet(Integer.toString(MELOSYS_ENHET_ID));

        sakOgBehandlingClient.sendBehandlingOpprettet(builder.build());

        prosessinstans.setSteg(null);
    }
}
