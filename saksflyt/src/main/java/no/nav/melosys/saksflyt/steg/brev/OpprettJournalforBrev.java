package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.arkiv.JournalpostBestilling;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.DokgenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPRETT_OG_JOURNALFØR_BREV;

@Component
public class OpprettJournalforBrev implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettJournalforBrev.class);

    private final BehandlingService behandlingService;
    private final DokgenService dokgenService;
    private final JoarkFasade joarkFasade;
    private final EregFasade eregFasade;
    private final KontaktopplysningService kontaktopplysningService;

    @Autowired
    public OpprettJournalforBrev(BehandlingService behandlingService, DokgenService dokgenService, JoarkFasade joarkFasade, EregFasade eregFasade, KontaktopplysningService kontaktopplysningService) {
        this.behandlingService = behandlingService;
        this.dokgenService = dokgenService;
        this.joarkFasade = joarkFasade;
        this.eregFasade = eregFasade;
        this.kontaktopplysningService = kontaktopplysningService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPRETT_OG_JOURNALFØR_BREV;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        if (prosessinstans.getBehandling() == null) {
            throw new FunksjonellException("Prosessinstans mangler behandling");
        }
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        PersonDokument personDokument = behandling.hentPersonDokument();
        Produserbaredokumenter produserbartDokument = prosessinstans.getData(PRODUSERBART_BREV, Produserbaredokumenter.class);
        Aktoer mottaker = prosessinstans.getData(MOTTAKER, Aktoer.class);
        OrganisasjonDokument org = null;
        Kontaktopplysning kontaktopplysning = null;

        if (!Aktoersroller.BRUKER.equals(mottaker.getRolle())) {
            org = (OrganisasjonDokument) eregFasade.hentOrganisasjon(mottaker.getOrgnr()).getDokument();
            kontaktopplysning = kontaktopplysningService.hentKontaktopplysning(behandling.getFagsak().getSaksnummer(), mottaker.getOrgnr()).orElse(null);
        }

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling(
            produserbartDokument,
            behandling,
            org,
            kontaktopplysning
        );

        byte[] pdf = dokgenService.produserBrev(brevbestilling);
        log.info("Produserbartdokument {} for behandling {} produsert", produserbartDokument, behandling.getId());

        JournalpostBestilling bestilling = new JournalpostBestilling(
            produserbartDokument.getBeskrivelse(),
            dokgenService.hentMalnavn(produserbartDokument),
            personDokument.fnr,
            org == null ? personDokument.sammensattNavn : org.getNavn(),
            org == null ? personDokument.fnr : org.getOrgnummer(),
            org != null,
            behandling.getFagsak().getGsakSaksnummer().toString(),
            pdf
        );

        String journalpostId = joarkFasade.opprettJournalpost(OpprettJournalpost.lagJournalpostForBrev(bestilling), true);

        log.info("Brev for behandling {} er journalført, journalpostId {}", behandling.getId(), journalpostId);
        prosessinstans.setData(DISTRIBUERBAR_JOURNALPOST_ID, journalpostId);
    }
}
