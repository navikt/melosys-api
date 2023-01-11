package no.nav.melosys.saksflyt.steg.behandling;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;

@Component
public class OpprettFagsakOgBehandlingFraSed implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettFagsakOgBehandlingFraSed.class);

    private final FagsakService fagsakService;
    private final JoarkFasade joarkFasade;

    public OpprettFagsakOgBehandlingFraSed(FagsakService fagsakService, JoarkFasade joarkFasade) {
        this.fagsakService = fagsakService;
        this.joarkFasade = joarkFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_MOTTAK_OPPRETT_FAGSAK_OG_BEH;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        MelosysEessiMelding eessiMelding = prosessinstans.getData(EESSI_MELDING, MelosysEessiMelding.class);

        Sakstemaer sakstema = prosessinstans.getData(SAKSTEMA, Sakstemaer.class);
        Behandlingstema behandlingstema = prosessinstans.getData(BEHANDLINGSTEMA, Behandlingstema.class);

        OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder()
            .medAktørID(prosessinstans.hentAktørIDFraDataEllerSED())
            .medSakstype(Sakstyper.EU_EOS)
            .medSakstema(sakstema)
            .medBehandlingstema(behandlingstema)
            .medBehandlingstype(Behandlingstyper.FØRSTEGANG)
            .medBehandlingsårsaktype(Behandlingsaarsaktyper.SED)
            .medMottaksdato(joarkFasade.hentMottaksDatoForJournalpost(eessiMelding.getJournalpostId()))
            .medInitierendeJournalpostId(eessiMelding.getJournalpostId())
            .medInitierendeDokumentId(eessiMelding.getDokumentId())
            .build();

        Fagsak fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest);
        prosessinstans.setData(SAKSNUMMER, fagsak.getSaksnummer());

        Behandling behandling = fagsak.hentAktivBehandling();
        prosessinstans.setBehandling(behandling);

        log.info("Fagsak {} opprettet med behandling {} for RINA-sak {}",
            fagsak.getSaksnummer(), behandling.getId(), eessiMelding.getRinaSaksnummer());
    }
}
