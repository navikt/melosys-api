package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.JournalpostBestilling;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.DokumentproduksjonsInfo;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPRETT_OG_JOURNALFØR_BREV;
import static org.springframework.util.ObjectUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

@Component
public class OpprettJournalforBrev implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettJournalforBrev.class);

    private final BehandlingService behandlingService;
    private final DokgenService dokgenService;
    private final JoarkFasade joarkFasade;
    private final PersondataFasade persondataFasade;
    private final EregFasade eregFasade;

    @Autowired
    public OpprettJournalforBrev(BehandlingService behandlingService, DokgenService dokgenService,
                                 @Qualifier("system") JoarkFasade joarkFasade,
                                 @Qualifier("system") PersondataFasade persondataFasade,
                                 @Qualifier("system") EregFasade eregFasade) {
        this.behandlingService = behandlingService;
        this.dokgenService = dokgenService;
        this.joarkFasade = joarkFasade;
        this.persondataFasade = persondataFasade;
        this.eregFasade = eregFasade;
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
        var brevbestilling = prosessinstans.getData(BREVBESTILLING, DokgenBrevbestilling.class);
        Produserbaredokumenter produserbartDokument = brevbestilling.getProduserbartdokument();

        String aktørId = prosessinstans.getData(AKTØR_ID);
        String orgnr = prosessinstans.getData(ORGNR, String.class, null);
        String fnr = null;
        String sammensattNavn = null;

        if (isEmpty(aktørId) && isEmpty(orgnr)) {
            throw new FunksjonellException("Mangler mottaker");
        }

        Aktoer mottaker = new Aktoer();

        if (isEmpty(orgnr)) {
            mottaker.setAktørId(aktørId);
            fnr = persondataFasade.hentFolkeregisterIdent(aktørId);
            sammensattNavn = persondataFasade.hentSammensattNavn(fnr);
        } else {
            mottaker.setOrgnr(orgnr);
        }

        byte[] pdf = dokgenService.produserBrev(mottaker, brevbestilling);
        log.info("Produserbartdokument {} for behandling {} produsert", produserbartDokument, behandling.getId());

        DokumentproduksjonsInfo dokumentproduksjonsInfo = dokgenService.hentDokumentInfo(produserbartDokument);

        JournalpostBestilling bestilling = new JournalpostBestilling.Builder()
            .medTittel(dokumentproduksjonsInfo.journalføringsTittel())
            .medBrevkode(dokumentproduksjonsInfo.dokgenMalnavn())
            .medDokumentKategori(dokumentproduksjonsInfo.dokumentKategoriKode())
            .medBrukerFnr(personDokument.fnr)
            .medMottakerNavn(hasText(orgnr) ? eregFasade.hentOrganisasjonNavn(orgnr) : sammensattNavn)
            .medMottakerId(hasText(orgnr) ? orgnr : fnr)
            .medErMottakerOrg(hasText(orgnr))
            .medSaksnummer(behandling.getFagsak().getSaksnummer())
            .medPdf(pdf)
            .build();

        String journalpostId = joarkFasade.opprettJournalpost(OpprettJournalpost.lagJournalpostForBrev(bestilling), true);

        log.info("Brev for behandling {} er journalført, journalpostId {}", behandling.getId(), journalpostId);
        prosessinstans.setData(DISTRIBUERBAR_JOURNALPOST_ID, journalpostId);
    }
}
