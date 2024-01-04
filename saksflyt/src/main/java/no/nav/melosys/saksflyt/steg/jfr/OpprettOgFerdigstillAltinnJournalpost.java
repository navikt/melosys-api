package no.nav.melosys.saksflyt.steg.jfr;

import java.util.Collection;
import java.util.Optional;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.kodeverk.Fullmaktstype;
import no.nav.melosys.domain.msm.AltinnDokument;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.altinn.AltinnSoeknadService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.stereotype.Component;

@Component
public class OpprettOgFerdigstillAltinnJournalpost implements StegBehandler {
    private final AltinnSoeknadService altinnSoeknadService;
    private final BehandlingService behandlingService;
    private final EregFasade eregFasade;
    private final JoarkFasade joarkFasade;
    private final PersondataFasade persondataFasade;

    public OpprettOgFerdigstillAltinnJournalpost(AltinnSoeknadService altinnSoeknadService,
                                                 BehandlingService behandlingService,
                                                 EregFasade eregFasade,
                                                 JoarkFasade joarkFasade,
                                                 PersondataFasade persondataFasade) {
        this.altinnSoeknadService = altinnSoeknadService;
        this.behandlingService = behandlingService;
        this.eregFasade = eregFasade;
        this.joarkFasade = joarkFasade;
        this.persondataFasade = persondataFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.OPPRETT_OG_FERDIGSTILL_JOURNALPOST_FRA_ALTINN;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        final Behandling behandling = prosessinstans.getBehandling();
        final Fagsak fagsak = behandling.getFagsak();

        String ident = persondataFasade.hentFolkeregisterident(fagsak.hentBrukersAktørID());
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, ident);

        Collection<AltinnDokument> dokumenter = altinnSoeknadService
            .hentDokumenterTilknyttetSoknad(prosessinstans.getData(ProsessDataKey.MOTTATT_SOKNAD_ID));

        Optional<Aktoer> fullmektig = fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_SØKNAD);
        String avsenderNavn;
        if (fullmektig.isPresent()) {
            avsenderNavn = eregFasade.hentOrganisasjonNavn(fullmektig.get().getOrgnr());
        } else {
            avsenderNavn = eregFasade.hentOrganisasjonNavn(fagsak.hentUnikArbeidsgiver().getOrgnr());
        }

        OpprettJournalpost opprettJournalpost = OpprettJournalpost.lagJournalpostForMottakAltinnSøknad(
            prosessinstans.getBehandling().getFagsak(), dokumenter, ident, avsenderNavn
        );

        String journalpostID = joarkFasade.opprettJournalpost(opprettJournalpost, true);

        behandling.setInitierendeJournalpostId(journalpostID);
        behandlingService.lagre(behandling);
    }
}
