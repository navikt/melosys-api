package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.JournalpostBestilling;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.FritekstbrevBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.brev.DokumentNavnService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.DokumentproduksjonsInfo;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final UtenlandskMyndighetService utenlandskMyndighetService;
    private final JoarkFasade joarkFasade;
    private final PersondataFasade persondataFasade;
    private final EregFasade eregFasade;
    private final DokumentNavnService dokumentNavnService;

    public OpprettJournalforBrev(BehandlingService behandlingService,
                                 DokgenService dokgenService,
                                 UtenlandskMyndighetService utenlandskMyndighetService,
                                 @Qualifier("system") JoarkFasade joarkFasade,
                                 @Qualifier("system") PersondataFasade persondataFasade,
                                 @Qualifier("system") EregFasade eregFasade,
                                 DokumentNavnService dokumentNavnService) {
        this.behandlingService = behandlingService;
        this.dokgenService = dokgenService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
        this.joarkFasade = joarkFasade;
        this.persondataFasade = persondataFasade;
        this.eregFasade = eregFasade;
        this.dokumentNavnService = dokumentNavnService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPRETT_OG_JOURNALFØR_BREV;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        String aktørId = prosessinstans.getData(AKTØR_ID);
        String orgnr = prosessinstans.getData(ORGNR);
        String institusjonsid = prosessinstans.getData(INSTITUSJON_ID);

        if (isEmpty(aktørId) && isEmpty(orgnr) && isEmpty(institusjonsid)) {
            throw new FunksjonellException("Mangler mottaker");
        }
        if (prosessinstans.getBehandling() == null) {
            throw new FunksjonellException("Prosessinstans mangler behandling");
        }

        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(prosessinstans.getBehandling().getId());
        String brukerFnr = hentBrukerFolkeregisterIdent(behandling);
        Aktoersroller mottakerrolle = prosessinstans.getData(MOTTAKER, Aktoersroller.class, null);
        var brevbestilling = prosessinstans.getData(BREVBESTILLING, DokgenBrevbestilling.class);
        Produserbaredokumenter produserbartDokument = brevbestilling.getProduserbartdokument();

        String fnr = null;
        String sammensattNavn = null;

        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(mottakerrolle);

        if (!isEmpty(institusjonsid)) {
            mottaker.setInstitusjonId(institusjonsid);
            sammensattNavn = utenlandskMyndighetService.hentUtenlandskMyndighetForInstitusjonID(institusjonsid).navn;
        } else if (!isEmpty(orgnr)) {
            mottaker.setOrgnr(orgnr);
        } else {
            mottaker.setAktørId(aktørId);
            fnr = persondataFasade.hentFolkeregisterident(aktørId);
            sammensattNavn = persondataFasade.hentSammensattNavn(fnr);
        }

        byte[] pdf = dokgenService.produserBrev(mottaker, brevbestilling);
        log.info("Produserbartdokument {} for behandling {} produsert", produserbartDokument, behandling.getId());

        DokumentproduksjonsInfo dokumentproduksjonsInfo = dokgenService.hentDokumentInfo(produserbartDokument);

        JournalpostBestilling bestilling = new JournalpostBestilling.Builder()
            .medTittel(utledJournalføringsTittel(behandling, dokumentproduksjonsInfo, brevbestilling, mottaker))
            .medBrevkode(dokumentproduksjonsInfo.dokgenMalnavn())
            .medDokumentKategori(dokumentproduksjonsInfo.dokumentKategoriKode())
            .medBrukerFnr(brukerFnr)
            .medMottakerNavn(hasText(orgnr) ? eregFasade.hentOrganisasjonNavn(orgnr) : sammensattNavn)
            .medMottakerId(utledMottakerId(orgnr, fnr, institusjonsid))
            .medMottakerIdType(utledMottakerIdType(orgnr, institusjonsid))
            .medSaksnummer(behandling.getFagsak().getSaksnummer())
            .medPdf(pdf)
            .build();

        String journalpostId = joarkFasade.opprettJournalpost(OpprettJournalpost.lagJournalpostForBrev(bestilling), true);

        log.info("Brev for behandling {} er journalført, journalpostId {}", behandling.getId(), journalpostId);
        prosessinstans.setData(DISTRIBUERBAR_JOURNALPOST_ID, journalpostId);
    }

    private String utledMottakerId(String orgnr, String fnr, String institusjonId) {
        if (hasText(institusjonId)) {
            return institusjonId;
        }
        if (hasText(orgnr)) {
            return orgnr;
        }
        return fnr;
    }

    private OpprettJournalpost.KorrespondansepartIdType utledMottakerIdType(String orgnr, String institusjonId) {
        if (hasText(institusjonId)) {
            return OpprettJournalpost.KorrespondansepartIdType.UTENLANDSK_ORGANISASJON;
        }
        if (hasText(orgnr)) {
            return OpprettJournalpost.KorrespondansepartIdType.ORGNR;
        }
        return OpprettJournalpost.KorrespondansepartIdType.FNR;
    }

    private String hentBrukerFolkeregisterIdent(Behandling behandling) {
        return persondataFasade.hentFolkeregisterident(behandling.getFagsak().hentBrukersAktørID());
    }

    public String utledJournalføringsTittel(Behandling behandling, DokumentproduksjonsInfo dokumentproduksjonsInfo, DokgenBrevbestilling brevbestilling, Aktoer mottaker) {
        if (brevbestilling instanceof FritekstbrevBrevbestilling fritekstbrevBrevbestilling) {
            String fritekstTittel = fritekstbrevBrevbestilling.getFritekstTittel();
            if (isEmpty(fritekstTittel)) {
                throw new FunksjonellException("Tittel til fritekstbrev mangler, behandlingId:" + brevbestilling.getBehandlingId());
            }
            return fritekstTittel;
        }
        if (brevbestilling.getProduserbartdokument() == Produserbaredokumenter.STORBRITANNIA) {
            return dokumentNavnService.utledDokumentNavn(behandling, dokumentproduksjonsInfo, mottaker);
        }
        return dokumentproduksjonsInfo.journalføringsTittel();
    }

}
