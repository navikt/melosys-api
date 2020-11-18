package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.PRODUSERBART_BREV;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPRETT_OG_JOURNALFØR_BREV;

@Component
public class OpprettJournalforBrev implements StegBehandler {

    private final BehandlingService behandlingService;
    private final DokumentServiceFasade dokumentServiceFasade;
    private final JoarkFasade joarkFasade;
    private final ProsessinstansService prosessinstansService;

    @Autowired
    public OpprettJournalforBrev(BehandlingService behandlingService, DokumentServiceFasade dokumentServiceFasade, JoarkFasade joarkFasade, ProsessinstansService prosessinstansService) {
        this.behandlingService = behandlingService;
        this.dokumentServiceFasade = dokumentServiceFasade;
        this.joarkFasade = joarkFasade;
        this.prosessinstansService = prosessinstansService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPRETT_OG_JOURNALFØR_BREV;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        Produserbaredokumenter produserbartDokument = prosessinstans.getData(PRODUSERBART_BREV, Produserbaredokumenter.class);

        byte[] pdf = dokumentServiceFasade.produserBrev(produserbartDokument, behandling);

        String journalpostId = joarkFasade.opprettJournalpost(
            OpprettJournalpost.lagJournalpostForPdf(produserbartDokument.getBeskrivelse(),
                behandling.hentPersonDokument().fnr, pdf), true);

        prosessinstansService.opprettProsessinstansDistribuerJournalpost(journalpostId);
    }
}
