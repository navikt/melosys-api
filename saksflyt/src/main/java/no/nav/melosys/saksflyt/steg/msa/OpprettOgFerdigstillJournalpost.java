package no.nav.melosys.saksflyt.steg.msa;

import java.util.Collection;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.msm.AltinnDokument;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.altinn.AltinnSoeknadService;
import no.nav.melosys.service.behandling.BehandlingService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class OpprettOgFerdigstillJournalpost implements StegBehandler {

    private final AltinnSoeknadService altinnSoeknadService;
    private final TpsFasade tpsFasade;
    private final JoarkFasade joarkFasade;
    private final BehandlingService behandlingService;

    public OpprettOgFerdigstillJournalpost(AltinnSoeknadService altinnSoeknadService,
                                           @Qualifier("system") TpsFasade tpsFasade,
                                           @Qualifier("system") JoarkFasade joarkFasade,
                                           BehandlingService behandlingService) {
        this.altinnSoeknadService = altinnSoeknadService;
        this.tpsFasade = tpsFasade;
        this.joarkFasade = joarkFasade;
        this.behandlingService = behandlingService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.MSA_OPPRETT_OG_FERDIGSTILL_JOURNALPOST;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {

        final Behandling behandling = prosessinstans.getBehandling();
        final Fagsak fagsak = behandling.getFagsak();

        String ident = tpsFasade.hentIdentForAktørId(fagsak.hentBruker().getAktørId());
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, ident);

        Collection<AltinnDokument> dokumenter = altinnSoeknadService
            .hentDokumenterTilknyttetSoknad(prosessinstans.getData(ProsessDataKey.MOTTATT_SOKNAD_ID));

        OpprettJournalpost opprettJournalpost = OpprettJournalpost.lagJournalpostForMottakAltinnSøknad(
            prosessinstans.getBehandling().getFagsak(), dokumenter, ident
        );

        String journalpostID = joarkFasade.opprettJournalpost(opprettJournalpost, true);

        behandling.setInitierendeJournalpostId(journalpostID);
        behandlingService.lagre(behandling);

        prosessinstans.setSteg(ProsessSteg.MSA_HENT_REGISTEROPPLYSNINGER);
    }
}
