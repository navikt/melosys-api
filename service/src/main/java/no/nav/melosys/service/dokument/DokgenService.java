package no.nav.melosys.service.dokument;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.arkiv.Distribusjonstype;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.SaksvedleggBestilling;
import no.nav.melosys.domain.brev.*;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.integrasjon.dokgen.DokgenConsumer;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.UtledMottaksdato;
import no.nav.melosys.service.bruker.SaksbehandlerService;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.dokument.brev.FritekstvedleggDto;
import no.nav.melosys.service.dokument.brev.KopiMottaker;
import no.nav.melosys.service.dokument.brev.SaksvedleggDto;
import no.nav.melosys.service.dokument.brev.mapper.DokgenMalMapper;
import no.nav.melosys.service.dokument.brev.mapper.DokumentproduksjonsInfoMapper;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.util.StringUtils.hasText;

@Service
public class DokgenService {

    private final DokgenConsumer dokgenConsumer;
    private final DokumentproduksjonsInfoMapper dokumentproduksjonsInfoMapper;
    private final JoarkFasade joarkFasade;
    private final DokgenMalMapper dokgenMalMapper;
    private final BehandlingService behandlingService;
    private final EregFasade eregFasade;
    private final KontaktopplysningService kontaktopplysningService;
    private final BrevmottakerService brevmottakerService;
    private final ProsessinstansService prosessinstansService;
    private final SaksbehandlerService saksbehandlerService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;
    private final UtledMottaksdato utledMottaksdato;

    public DokgenService(DokgenConsumer dokgenConsumer,
                         DokumentproduksjonsInfoMapper dokumentproduksjonsInfoMapper,
                         JoarkFasade joarkFasade,
                         DokgenMalMapper dokgenMalMapper,
                         BehandlingService behandlingService,
                         EregFasade eregFasade,
                         KontaktopplysningService kontaktopplysningService,
                         BrevmottakerService brevmottakerService,
                         ProsessinstansService prosessinstansService,
                         SaksbehandlerService saksbehandlerService,
                         UtenlandskMyndighetService utenlandskMyndighetService,
                         UtledMottaksdato utledMottaksdato) {
        this.dokgenConsumer = dokgenConsumer;
        this.dokumentproduksjonsInfoMapper = dokumentproduksjonsInfoMapper;
        this.joarkFasade = joarkFasade;
        this.dokgenMalMapper = dokgenMalMapper;
        this.behandlingService = behandlingService;
        this.eregFasade = eregFasade;
        this.kontaktopplysningService = kontaktopplysningService;
        this.brevmottakerService = brevmottakerService;
        this.prosessinstansService = prosessinstansService;
        this.saksbehandlerService = saksbehandlerService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
        this.utledMottaksdato = utledMottaksdato;
    }

    @Transactional
    public byte[] produserUtkast(long behandlingId, BrevbestillingRequest brevbestillingRequest) {
        Produserbaredokumenter produserbartdokument = brevbestillingRequest.getProduserbardokument();
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        Aktoer mottaker;
        if (hasText(brevbestillingRequest.getOrgnr()) || hasText(brevbestillingRequest.getInstitusjonId())) {
            mottaker = new Aktoer();
            mottaker.setRolle(brevbestillingRequest.getMottaker());
            mottaker.setOrgnr(brevbestillingRequest.getOrgnr());
            mottaker.setInstitusjonId(brevbestillingRequest.getInstitusjonId());
        } else {
            mottaker = brevmottakerService.avklarMottakere(produserbartdokument,
                Mottaker.av(brevbestillingRequest.getMottaker()), behandling, true, false).get(0);
        }

        DokgenBrevbestilling.Builder<?> brevbestilling = lagDokgenBrevbestilling(brevbestillingRequest);

        brevbestilling
            .medProduserbartdokument(produserbartdokument)
            .medBehandlingId(behandlingId)
            .medSaksbehandlerNavn(hentSaksbehandlerNavn(brevbestillingRequest.getBestillersId()))
            .medBestillUtkast(true);

        return produserBrev(mottaker, brevbestilling.build());
    }

    @Transactional
    public byte[] produserBrev(Aktoer mottaker, DokgenBrevbestilling brevbestilling) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(brevbestilling.getBehandlingId());
        String malnavn = dokumentproduksjonsInfoMapper.hentMalnavn(brevbestilling.getProduserbartdokument());
        String orgnr = mottaker != null ? mottaker.getOrgnr() : null;
        DokgenBrevbestilling.Builder<?> builder = brevbestilling.toBuilder();

        builder.medBehandling(behandling);

        if (hasText(orgnr)) {
            settOrganisasjonsOpplysninger(behandling, orgnr, builder);
        }
        if (mottaker != null && mottaker.erUtenlandskMyndighet()) {
            settUtenlandskMyndighetOpplysninger(mottaker.hentMyndighetLandkode(), builder, brevbestilling.getProduserbartdokument());
        }

        settForsendelseMottattOgAvsender(behandling, builder);

        var dokgenDto = dokgenMalMapper.mapBehandling(builder.build(), mottaker);
        return dokgenConsumer.lagPdf(malnavn, dokgenDto, brevbestilling.isBestillKopi(), brevbestilling.isBestillUtkast());
    }

    @Transactional
    public void produserOgDistribuerBrev(long behandlingId, BrevbestillingRequest brevbestillingRequest) {
        Produserbaredokumenter produserbartDokument = brevbestillingRequest.getProduserbardokument();
        var behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);

        DokgenBrevbestilling.Builder<?> brevbestilling = lagDokgenBrevbestilling(brevbestillingRequest);

        brevbestilling
            .medProduserbartdokument(produserbartDokument)
            .medBehandlingId(behandlingId)
            .medSaksvedleggBestilling(lagSaksvedleggBestilling(brevbestillingRequest.getSaksVedlegg()))
            .medSaksbehandlerNavn(hentSaksbehandlerNavn(brevbestillingRequest.getBestillersId()))
            .medFritekstvedleggBestilling(lagFritekstvedleggBestilling(brevbestillingRequest.getFritekstvedlegg()));

        List<Aktoer> mottakere = hentMottakere(brevbestillingRequest, produserbartDokument, behandling);

        for (Aktoer aktoer : mottakere) {
            produserOgDistribuerBrev(behandling, aktoer, brevbestilling.build());
        }

        for (KopiMottaker kopiMottaker : brevbestillingRequest.getKopiMottakere()) {
            var aktoer = new Aktoer();
            aktoer.setRolle(kopiMottaker.rolle());
            aktoer.setOrgnr(kopiMottaker.orgnr());
            aktoer.setAktørId(kopiMottaker.aktørId());
            aktoer.setInstitusjonId(kopiMottaker.institusjonId());
            brevbestilling.medBestillKopi(true);
            produserOgDistribuerBrev(behandling, aktoer, brevbestilling.build());
        }
    }

    private List<Aktoer> hentMottakere(BrevbestillingRequest brevbestillingRequest, Produserbaredokumenter produserbartDokument, Behandling behandling) {
        List<Aktoer> mottakere = new ArrayList<>();
        boolean erBrevTilOrganisasjon = hasText(brevbestillingRequest.getOrgnr());
        boolean erBrevTilEtat = Aktoersroller.ETAT.equals(brevbestillingRequest.getMottaker())
            && !brevbestillingRequest.getOrgnrEtater().isEmpty();

        if (erBrevTilOrganisasjon) {
            Aktoer mottaker = new Aktoer();
            mottaker.setRolle(brevbestillingRequest.getMottaker());
            mottaker.setOrgnr(brevbestillingRequest.getOrgnr());
            mottakere.add(mottaker);
        } else if (erBrevTilEtat) {
            for (String orgNr : brevbestillingRequest.getOrgnrEtater()) {
                Aktoer mottaker = new Aktoer();
                mottaker.setRolle(brevbestillingRequest.getMottaker());
                mottaker.setOrgnr(orgNr);
                mottakere.add(mottaker);
            }
        } else {
            mottakere = brevmottakerService.avklarMottakere(produserbartDokument,
                Mottaker.av(brevbestillingRequest.getMottaker()), behandling, false, false);
        }
        return mottakere;
    }

    private void produserOgDistribuerBrev(Behandling behandling, Aktoer mottaker, DokgenBrevbestilling brevbestilling) {
        prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(behandling, mottaker, brevbestilling);
    }

    public DokumentproduksjonsInfo hentDokumentInfo(Produserbaredokumenter produserbartDokument) {
        return dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(produserbartDokument);
    }

    public boolean erTilgjengeligDokgenmal(Produserbaredokumenter produserbartDokument) {
        Set<Produserbaredokumenter> tilgjengeligeMaler = dokumentproduksjonsInfoMapper.utledTilgjengeligeMaler();
        return tilgjengeligeMaler.contains(produserbartDokument);
    }

    private void settOrganisasjonsOpplysninger(Behandling behandling, String orgnr,
                                               DokgenBrevbestilling.Builder<?> brevbestilling) {
        var kontaktopplysning = kontaktopplysningService.hentKontaktopplysning(behandling.getFagsak().getSaksnummer(), orgnr).orElse(null);
        String mottakerOrgnr = kontaktopplysning != null && kontaktopplysning.getKontaktOrgnr() != null ? kontaktopplysning.getKontaktOrgnr() : orgnr;
        brevbestilling
            .medOrg((OrganisasjonDokument) eregFasade.hentOrganisasjon(mottakerOrgnr).getDokument())
            .medKontaktopplysning(kontaktopplysning);
    }

    private void settUtenlandskMyndighetOpplysninger(Land_iso2 landkode, DokgenBrevbestilling.Builder<?> brevbestilling,
                                                     Produserbaredokumenter produserbartdokument) {
        var utenlandskMyndighet =
            utenlandskMyndighetService.hentUtenlandskMyndighet(landkode, produserbartdokument);
        brevbestilling.medUtenlandskMyndighet(utenlandskMyndighet);
    }

    private void settForsendelseMottattOgAvsender(Behandling behandling, DokgenBrevbestilling.Builder<?> brevbestilling) {
        Journalpost journalpost = null;
        if (behandling.getInitierendeJournalpostId() != null) {
            journalpost = joarkFasade.hentJournalpost(behandling.getInitierendeJournalpostId());
            brevbestilling.medAvsenderFraJournalpost(journalpost);
        }
        var mottaksdato = tilInstant(utledMottaksdato.getMottaksdato(behandling, journalpost));
        brevbestilling.medForsendelseMottatt(mottaksdato);
    }

    private Instant tilInstant(LocalDate localDate) {
        return localDate != null ? localDate.atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
    }

    private String hentSaksbehandlerNavn(String ident) {
        return ident != null ? saksbehandlerService.hentNavnForIdent(ident) : "N/A";
    }

    private boolean inneholderArbeidsgiverSomKopimottaker(Collection<KopiMottaker> kopimottakere) {
        return kopimottakere.stream().map(KopiMottaker::rolle).anyMatch(kopimottaker -> kopimottaker == Aktoersroller.ARBEIDSGIVER);
    }

    private boolean inneholderBrukerSomKopimottaker(Collection<KopiMottaker> kopimottakere) {
        return kopimottakere.stream().map(KopiMottaker::rolle).anyMatch(kopimottaker -> kopimottaker == Aktoersroller.BRUKER);
    }

    private List<SaksvedleggBestilling> lagSaksvedleggBestilling(List<SaksvedleggDto> saksvedleggDtoer) {
        if (saksvedleggDtoer == null) {
            return Collections.emptyList();
        }

        return saksvedleggDtoer.stream()
            .map(saksvedlegg -> new SaksvedleggBestilling(saksvedlegg.journalpostID(), saksvedlegg.dokumentID()))
            .toList();
    }

    private List<FritekstvedleggBestilling> lagFritekstvedleggBestilling(List<FritekstvedleggDto> fritekstvedleggDtoer) {
        if (fritekstvedleggDtoer == null) {
            return Collections.emptyList();
        }

        return fritekstvedleggDtoer.stream()
            .map(fritekstVedlegg -> new FritekstvedleggBestilling(fritekstVedlegg.tittel(), fritekstVedlegg.fritekst()))
            .toList();
    }

    private DokgenBrevbestilling.Builder<?> lagDokgenBrevbestilling(BrevbestillingRequest brevbestillingRequest) {
        return switch (brevbestillingRequest.getProduserbardokument()) {
            case MANGELBREV_ARBEIDSGIVER, MANGELBREV_BRUKER -> new MangelbrevBrevbestilling.Builder()
                .medDistribusjonstype(Distribusjonstype.VIKTIG)
                .medInnledningFritekst(brevbestillingRequest.getInnledningFritekst())
                .medManglerInfoFritekst(brevbestillingRequest.getManglerFritekst())
                .medKontaktpersonNavn(brevbestillingRequest.getKontaktpersonNavn())
                .medBrukerSkalHaKopi(inneholderBrukerSomKopimottaker(brevbestillingRequest.getKopiMottakere()));
            case INNVILGELSE_FOLKETRYGDLOVEN_2_8, TRYGDEAVTALE_GB, TRYGDEAVTALE_US, TRYGDEAVTALE_CAN, TRYGDEAVTALE_AU ->
                new InnvilgelseBrevbestilling.Builder()
                    .medDistribusjonstype(Distribusjonstype.VEDTAK)
                    .medInnledningFritekst(brevbestillingRequest.getInnledningFritekst())
                    .medBegrunnelseFritekst(brevbestillingRequest.getBegrunnelseFritekst())
                    .medEktefelleFritekst(brevbestillingRequest.getEktefelleFritekst())
                    .medBarnFritekst(brevbestillingRequest.getBarnFritekst())
                    .medVirksomhetArbeidsgiverSkalHaKopi(inneholderArbeidsgiverSomKopimottaker(brevbestillingRequest.getKopiMottakere()))
                    .medNyVurderingBakgrunn(brevbestillingRequest.getNyVurderingBakgrunn());
            case GENERELT_FRITEKSTBREV_BRUKER, GENERELT_FRITEKSTBREV_ARBEIDSGIVER, GENERELT_FRITEKSTBREV_VIRKSOMHET,
                UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV, FRITEKSTBREV -> new FritekstbrevBrevbestilling.Builder()
                .medDistribusjonstype(brevbestillingRequest.getDistribusjonstype())
                .medFritekstTittel(brevbestillingRequest.getFritekstTittel())
                .medFritekst(brevbestillingRequest.getFritekst())
                .medKontaktpersonNavn(brevbestillingRequest.getKontaktpersonNavn())
                .medKontaktopplysninger(brevbestillingRequest.isKontaktopplysninger())
                .medBrukerSkalHaKopi(inneholderBrukerSomKopimottaker(brevbestillingRequest.getKopiMottakere()))
                .medMottakerType(brevbestillingRequest.getMottaker())
                .medDokumentTittel(brevbestillingRequest.getDokumentTittel());
            case AVSLAG_MANGLENDE_OPPLYSNINGER -> new AvslagBrevbestilling.Builder()
                .medDistribusjonstype(Distribusjonstype.VEDTAK)
                .medFritekst(brevbestillingRequest.getFritekst());
            case MELDING_HENLAGT_SAK -> new HenleggelseBrevbestilling.Builder()
                .medDistribusjonstype(Distribusjonstype.VIKTIG)
                .medFritekst(brevbestillingRequest.getFritekst())
                .medBegrunnelseKode(brevbestillingRequest.getBegrunnelseKode());
            case GENERELT_FRITEKSTVEDLEGG -> new FritekstvedleggBrevbestilling.Builder()
                .medFritekstvedleggTittel(brevbestillingRequest.getFritekstTittel())
                .medFritekstvedleggTekst(brevbestillingRequest.getFritekst());
            default -> new DokgenBrevbestilling.Builder<>().medDistribusjonstype(Distribusjonstype.VIKTIG);
        };
    }
}
