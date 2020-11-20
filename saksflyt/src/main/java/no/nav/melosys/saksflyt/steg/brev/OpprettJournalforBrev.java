package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.DokgenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID;
import static no.nav.melosys.domain.saksflyt.ProsessDataKey.PRODUSERBART_BREV;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPRETT_OG_JOURNALFØR_BREV;

@Component
public class OpprettJournalforBrev implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettJournalforBrev.class);

    private final BehandlingService behandlingService;
    private final DokgenService dokgenService;
    private final JoarkFasade joarkFasade;

    @Autowired
    public OpprettJournalforBrev(BehandlingService behandlingService, DokgenService dokgenService, JoarkFasade joarkFasade) {
        this.behandlingService = behandlingService;
        this.dokgenService = dokgenService;
        this.joarkFasade = joarkFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPRETT_OG_JOURNALFØR_BREV;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        Produserbaredokumenter produserbartDokument = prosessinstans.getData(PRODUSERBART_BREV, Produserbaredokumenter.class);

        byte[] pdf = dokgenService.produserBrev(produserbartDokument, behandling);
        log.info("Produserbartdokument {} for behandling {} produsert", produserbartDokument, behandling.getId());

        String journalpostId = joarkFasade.opprettJournalpost(
            OpprettJournalpost.lagJournalpostForBrev(produserbartDokument.getBeskrivelse(),
                behandling.hentPersonDokument().fnr, pdf), true);

        log.info("Brev for behandling {} er journalført, journalpostId {}", behandling.getId(), journalpostId);
        prosessinstans.setData(DISTRIBUERBAR_JOURNALPOST_ID, journalpostId);
    }
}
