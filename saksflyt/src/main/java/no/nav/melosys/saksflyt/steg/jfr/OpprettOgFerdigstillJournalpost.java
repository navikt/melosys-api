package no.nav.melosys.saksflyt.steg.jfr;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Optional;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.msm.AltinnDokument;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
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
    private final BehandlingService behandlingService;
    private final EregFasade eregFasade;
    private final JoarkFasade joarkFasade;
    private final TpsFasade tpsFasade;

    public OpprettOgFerdigstillJournalpost(AltinnSoeknadService altinnSoeknadService,
                                           BehandlingService behandlingService,
                                           @Qualifier("system") EregFasade eregFasade,
                                           @Qualifier("system") JoarkFasade joarkFasade,
                                           @Qualifier("system") TpsFasade tpsFasade) {
        this.altinnSoeknadService = altinnSoeknadService;
        this.behandlingService = behandlingService;
        this.eregFasade = eregFasade;
        this.joarkFasade = joarkFasade;
        this.tpsFasade = tpsFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.OPPRETT_OG_FERDIGSTILL_JOURNALPOST_FRA_ALTINN;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        final Behandling behandling = prosessinstans.getBehandling();
        final Fagsak fagsak = behandling.getFagsak();

        String ident = tpsFasade.hentIdentForAktørId(fagsak.hentBruker().getAktørId());
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, ident);

        Collection<AltinnDokument> dokumenter = altinnSoeknadService
            .hentDokumenterTilknyttetSoknad(prosessinstans.getData(ProsessDataKey.MOTTATT_SOKNAD_ID));

        Optional<Aktoer> representant = fagsak.hentRepresentant(Representerer.BRUKER);
        String avsenderNavn;
        if (representant.isPresent()) {
            avsenderNavn = eregFasade.hentOrganisasjonNavn(representant.get().getOrgnr());
        } else {
            avsenderNavn = tpsFasade.hentSammensattNavn(ident);
        }

        OpprettJournalpost opprettJournalpost = OpprettJournalpost.lagJournalpostForMottakAltinnSøknad(
            prosessinstans.getBehandling().getFagsak(), dokumenter, ident, avsenderNavn
        );

        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTEMA,
            LocalDate.ofInstant(opprettJournalpost.getForsendelseMottatt(), ZoneId.systemDefault()));
        String journalpostID = joarkFasade.opprettJournalpost(opprettJournalpost, true);

        behandling.setInitierendeJournalpostId(journalpostID);
        behandlingService.lagre(behandling);
    }
}
