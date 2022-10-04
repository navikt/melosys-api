package no.nav.melosys.saksflyt.steg.brev;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.*;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.FritekstbrevBrevbestilling;
import no.nav.melosys.domain.brev.FritekstvedleggBestilling;
import no.nav.melosys.domain.brev.FritekstvedleggBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.brev.DokumentNavnService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.DokumentHentingService;
import no.nav.melosys.service.dokument.DokumentproduksjonsInfo;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPRETT_OG_JOURNALFØR_BREV;
import static org.springframework.util.ObjectUtils.isEmpty;

@Component
public class OpprettOgJournalforBrev implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettOgJournalforBrev.class);

    private final BehandlingService behandlingService;
    private final DokgenService dokgenService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;
    private final JoarkFasade joarkFasade;
    private final PersondataFasade persondataFasade;
    private final EregFasade eregFasade;
    private final DokumentNavnService dokumentNavnService;
    private final DokumentHentingService dokumentHentingService;

    public OpprettOgJournalforBrev(BehandlingService behandlingService,
                                   DokgenService dokgenService,
                                   UtenlandskMyndighetService utenlandskMyndighetService,
                                   JoarkFasade joarkFasade,
                                   PersondataFasade persondataFasade,
                                   EregFasade eregFasade,
                                   DokumentNavnService dokumentNavnService,
                                   DokumentHentingService dokumentHentingService) {
        this.behandlingService = behandlingService;
        this.dokgenService = dokgenService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
        this.joarkFasade = joarkFasade;
        this.persondataFasade = persondataFasade;
        this.eregFasade = eregFasade;
        this.dokumentNavnService = dokumentNavnService;
        this.dokumentHentingService = dokumentHentingService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPRETT_OG_JOURNALFØR_BREV;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        if (prosessinstans.getBehandling() == null) {
            throw new FunksjonellException("Prosessinstans mangler behandling");
        }

        MottakerType mottakerType = utledMottakerType(prosessinstans);
        String mottakerID = utledMottakerID(mottakerType, prosessinstans);
        Aktoer mottaker = lagMottaker(mottakerType, mottakerID, prosessinstans);

        var brevbestilling = prosessinstans.getData(BREVBESTILLING, DokgenBrevbestilling.class);
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        Produserbaredokumenter produserbartDokument = brevbestilling.getProduserbartdokument();

        byte[] pdf = dokgenService.produserBrev(mottaker, brevbestilling);
        List<Vedlegg> vedlegg = hentVedlegg(brevbestilling, behandling.getFagsak().getSaksnummer(), mottaker);
        log.info("Produserbartdokument {} for behandling {} produsert", produserbartDokument, behandling.getId());

        DokumentproduksjonsInfo dokumentproduksjonsInfo = dokgenService.hentDokumentInfo(produserbartDokument);

        JournalpostBestilling.Builder bestilling = new JournalpostBestilling.Builder()
            .medTittel(utledJournalføringsTittel(behandling, dokumentproduksjonsInfo, brevbestilling, mottaker))
            .medBrevkode(dokumentproduksjonsInfo.dokgenMalnavn())
            .medDokumentKategori(dokumentproduksjonsInfo.dokumentKategoriKode())
            .medMottakerNavn(utledNavn(mottakerID, mottakerType))
            .medMottakerId(mottakerType == MottakerType.PERSON_MED_AKTØR_ID ? persondataFasade.hentFolkeregisterident(mottakerID) : mottakerID)
            .medMottakerIdType(utledMottakerIdType(mottakerType))
            .medSaksnummer(behandling.getFagsak().getSaksnummer())
            .medPdf(pdf)
            .medVedlegg(vedlegg);

        settHovedpart(behandling, bestilling);

        String journalpostId = joarkFasade.opprettJournalpost(OpprettJournalpost.lagJournalpostForBrev(bestilling.build()), true);

        log.info("Brev for behandling {} er journalført, journalpostId {}", behandling.getId(), journalpostId);
        prosessinstans.setData(DISTRIBUERBAR_JOURNALPOST_ID, journalpostId);
    }

    private MottakerType utledMottakerType(Prosessinstans prosessinstans) {
        if (prosessinstans.hasData(INSTITUSJON_ID)) {
            return MottakerType.INSTITUSJON;
        } else if (prosessinstans.hasData(ORGNR)) {
            return MottakerType.ORGANISASJON;
        } else if (prosessinstans.hasData(PERSON_IDENT)) {
            return MottakerType.PERSON_MED_FNR;
        } else if (prosessinstans.hasData(AKTØR_ID)) {
            return MottakerType.PERSON_MED_AKTØR_ID;
        }
        throw new FunksjonellException("Mangler mottaker");
    }

    private String utledMottakerID(MottakerType mottakerType, Prosessinstans prosessinstans) {
        return switch (mottakerType) {
            case PERSON_MED_AKTØR_ID -> prosessinstans.getData(AKTØR_ID);
            case PERSON_MED_FNR -> prosessinstans.getData(PERSON_IDENT);
            case ORGANISASJON -> prosessinstans.getData(ORGNR);
            case INSTITUSJON -> prosessinstans.getData(INSTITUSJON_ID);
        };
    }

    private Aktoer lagMottaker(MottakerType mottakerType, String mottakerID, Prosessinstans prosessinstans) {
        var mottaker = new Aktoer();
        mottaker.setRolle(prosessinstans.getData(MOTTAKER, Aktoersroller.class, null));
        switch (mottakerType) {
            case PERSON_MED_AKTØR_ID -> mottaker.setAktørId(mottakerID);
            case PERSON_MED_FNR -> mottaker.setPersonIdent(mottakerID);
            case ORGANISASJON -> mottaker.setOrgnr(mottakerID);
            case INSTITUSJON -> mottaker.setInstitusjonId(mottakerID);
        }
        return mottaker;
    }

    private String utledNavn(String mottakerID, MottakerType mottakerType) {
        return switch (mottakerType) {
            case PERSON_MED_AKTØR_ID, PERSON_MED_FNR -> persondataFasade.hentSammensattNavn(mottakerID);
            case ORGANISASJON -> eregFasade.hentOrganisasjonNavn(mottakerID);
            case INSTITUSJON -> utenlandskMyndighetService.hentUtenlandskMyndighetForInstitusjonID(mottakerID).navn;
        };
    }

    private void settHovedpart(Behandling behandling, JournalpostBestilling.Builder bestilling) {
        var fagsak = behandling.getFagsak();
        if (fagsak.harAktørMedRolleType(Aktoersroller.VIRKSOMHET)) {
            bestilling
                .medHovedpartId(fagsak.hentVirksomhet().getOrgnr())
                .medHovedpartIdType(BrukerIdType.ORGNR);
        } else {
            bestilling
                .medHovedpartId(persondataFasade.hentFolkeregisterident(fagsak.hentBrukersAktørID()))
                .medHovedpartIdType(BrukerIdType.FOLKEREGISTERIDENT);
        }
    }

    private OpprettJournalpost.KorrespondansepartIdType utledMottakerIdType(MottakerType mottakerType) {
        return switch (mottakerType) {
            case PERSON_MED_AKTØR_ID, PERSON_MED_FNR -> OpprettJournalpost.KorrespondansepartIdType.FNR;
            case INSTITUSJON -> OpprettJournalpost.KorrespondansepartIdType.UTENLANDSK_ORGANISASJON;
            case ORGANISASJON -> OpprettJournalpost.KorrespondansepartIdType.ORGNR;
        };
    }

    private List<Vedlegg> hentVedlegg(DokgenBrevbestilling brevbestilling, String saksnummer, Aktoer mottaker) {
        List<Vedlegg> fritekstvedlegg = produserFritekstvedlegg(brevbestilling, mottaker);
        List<Vedlegg> vedleggFraJoark = hentVedleggDokumenterFraJoark(brevbestilling, saksnummer);
        return Stream.concat(fritekstvedlegg.stream(), vedleggFraJoark.stream()).toList();
    }

    private List<Vedlegg> produserFritekstvedlegg(DokgenBrevbestilling brevbestilling, Aktoer mottaker) {
        List<FritekstvedleggBestilling> fritekstvedleggBestilling = brevbestilling.getFritekstvedleggBestilling();
        if (fritekstvedleggBestilling == null) {
            return Collections.emptyList();
        }
        if(!(brevbestilling instanceof FritekstbrevBrevbestilling fritekstbrevBrevbestilling)) {
            log.warn("Forsøkte å produsere brev %s med fritekstvedlegg for behandling %d".formatted(brevbestilling.getProduserbartdokument(), brevbestilling.getBehandlingId()));
            return Collections.emptyList();
        }
        return fritekstvedleggBestilling.stream().map(vedlegg -> {
            var vedleggBestilling = new FritekstvedleggBrevbestilling.Builder()
                .medBehandlingId(fritekstbrevBrevbestilling.getBehandlingId())
                .medProduserbartdokument(Produserbaredokumenter.GENERELT_FRITEKSTVEDLEGG)
                .medFritekstTittel(vedlegg.tittel())
                .medFritekst(vedlegg.fritekst())
                .build();
            return new Vedlegg(dokgenService.produserBrev(mottaker, vedleggBestilling), vedlegg.tittel());
        }).toList();
    }

    private List<Vedlegg> hentVedleggDokumenterFraJoark(DokgenBrevbestilling brevbestilling, String fagsaknummer) {
        if (brevbestilling.getSaksvedleggBestilling() == null) {
            return Collections.emptyList();
        }
        List<Journalpost> journalposterForSaken = dokumentHentingService.hentJournalposter(fagsaknummer);
        List<SaksvedleggBestilling> saksvedleggbestillingListe = brevbestilling.getSaksvedleggBestilling();

        return saksvedleggbestillingListe.stream()
            .map(saksvedlegg -> {
                var arkivDokument = hentArkivDokumentFraJournalpost(saksvedlegg, journalposterForSaken);
                byte[] vedleggInnhold = joarkFasade.hentDokument(saksvedlegg.journalpostID(), saksvedlegg.dokumentID());
                return new Vedlegg(vedleggInnhold, arkivDokument.getTittel());
            }).toList();
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

    private ArkivDokument hentArkivDokumentFraJournalpost(SaksvedleggBestilling saksvedlegg, List<Journalpost> journalposter) {
        return journalposter.stream()
            .filter(journalpost -> saksvedlegg.journalpostID().equals(journalpost.getJournalpostId()))
            .findFirst()
            .orElseThrow(() -> new IkkeFunnetException(String.format("Finner ikke journalpost %s for saken %s", saksvedlegg.journalpostID(), "saksnummer")))
            .hentArkivDokument(saksvedlegg.dokumentID());
    }

}
