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
import static no.nav.melosys.domain.ProsessSteg.FERDIG;
import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;

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

    private final SakOgBehandlingClient sakOgBehandlingClient;

    @Autowired
    public OppdaterStatusBehandlingOpprettet(SakOgBehandlingClient sakOgBehandlingClient) {
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

        // FIXME: Legg til hendelsesId- og behandlingsId-løpenummer. BehandlingsId lagres i prosessinstansen og gjenbrukes når behandling avsluttes
        // behandlingsId skal være unik. Foreldrepenger prefikser med applikasjonsId.

        BehandlingStatusMapper.Builder builder = new BehandlingStatusMapper.Builder();
        builder.medSaksnummer(saksnummer);
        builder.medHendelsesprodusent(Fagsystem.MELOSYS.getKode());
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
