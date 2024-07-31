package no.nav.melosys.service.dokument;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.getunleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.Distribusjonstype;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.SaksvedleggBestilling;
import no.nav.melosys.domain.brev.*;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.featuretoggle.ToggleName;
import no.nav.melosys.integrasjon.dokgen.DokgenConsumer;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.UtledMottaksdato;
import no.nav.melosys.service.bruker.SaksbehandlerService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.FritekstvedleggDto;
import no.nav.melosys.service.dokument.brev.KopiMottakerDto;
import no.nav.melosys.service.dokument.brev.SaksvedleggDto;
import no.nav.melosys.service.dokument.brev.mapper.DokgenMalMapper;
import no.nav.melosys.service.dokument.brev.mapper.DokumentproduksjonsInfoMapper;
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
    private final Unleash unleash;

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
                         UtledMottaksdato utledMottaksdato, Unleash unleash) {
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
        this.unleash = unleash;
    }

    @Transactional
    public byte[] produserUtkast(long behandlingId, BrevbestillingDto brevbestillingDto) {
        Produserbaredokumenter produserbartdokument = brevbestillingDto.getProduserbardokument();
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        Mottaker mottaker;
        if (hasText(brevbestillingDto.getOrgnr()) || hasText(brevbestillingDto.getInstitusjonID())) {
            mottaker = Mottaker.medRolle(brevbestillingDto.getMottaker());
            mottaker.setOrgnr(brevbestillingDto.getOrgnr());
            mottaker.setInstitusjonID(brevbestillingDto.getInstitusjonID());
        } else {
            mottaker = brevmottakerService.avklarMottakere(produserbartdokument,
                Mottaker.medRolle(brevbestillingDto.getMottaker()), behandling, true, false).get(0);
        }

        DokgenBrevbestilling.Builder<?> brevbestilling = lagDokgenBrevbestilling(brevbestillingDto);

        brevbestilling
            .medProduserbartdokument(produserbartdokument)
            .medBehandlingId(behandlingId)
            .medSaksbehandlerNavn(hentSaksbehandlerNavn(brevbestillingDto.getBestillersId()))
            .medBestillUtkast(true);

        return produserBrev(mottaker, brevbestilling.build());
    }

    @Transactional
    public byte[] produserBrev(Mottaker mottaker, DokgenBrevbestilling brevbestilling) {
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
    public void produserOgDistribuerBrev(long behandlingId, BrevbestillingDto brevbestillingDto) {
        Produserbaredokumenter produserbartDokument = brevbestillingDto.getProduserbardokument();
        var behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);

        DokgenBrevbestilling.Builder<?> brevbestilling = lagDokgenBrevbestilling(brevbestillingDto);

        brevbestilling
            .medProduserbartdokument(produserbartDokument)
            .medBehandlingId(behandlingId)
            .medSaksvedleggBestilling(lagSaksvedleggBestilling(brevbestillingDto.getSaksVedlegg()))
            .medSaksbehandlerNavn(hentSaksbehandlerNavn(brevbestillingDto.getBestillersId()))
            .medFritekstvedleggBestilling(lagFritekstvedleggBestilling(brevbestillingDto.getFritekstvedlegg()));

        List<Mottaker> mottakere = hentMottakere(brevbestillingDto, produserbartDokument, behandling);

        for (Mottaker mottaker : mottakere) {
            produserOgDistribuerBrev(behandling, mottaker, brevbestilling.build());
        }

        for (KopiMottakerDto kopiMottaker : brevbestillingDto.getKopiMottakere()) {
            var mottaker = Mottaker.medRolle(kopiMottaker.rolle());
            if (kopimottakerErFullmektigPrivatperson(kopiMottaker)) {
                mottaker = brevmottakerService.avklarMottaker(brevbestillingDto.getProduserbardokument(), mottaker, behandling);
            } else {
                mottaker.setOrgnr(kopiMottaker.orgnr());
                mottaker.setAktørId(kopiMottaker.aktørId());
                mottaker.setInstitusjonID(kopiMottaker.institusjonID());
            }
            brevbestilling.medBestillKopi(true);
            produserOgDistribuerBrev(behandling, mottaker, brevbestilling.build());
        }
    }

    private List<Mottaker> hentMottakere(BrevbestillingDto brevbestillingDto, Produserbaredokumenter produserbartDokument, Behandling behandling) {
        boolean erBrevTilOrganisasjon = hasText(brevbestillingDto.getOrgnr());
        boolean erBrevTilNorskMyndighet = Mottakerroller.NORSK_MYNDIGHET.equals(brevbestillingDto.getMottaker())
            && !brevbestillingDto.getOrgnrNorskMyndighet().isEmpty();
        boolean erBrevTilAnnenPerson = hasText(brevbestillingDto.getAnnenPersonMottakerIdent());
        boolean erBrevTilUtenlandskTrygdemyndighetFraBehandlingMedIngenFlyt =
            hasText(brevbestillingDto.getInstitusjonID()) && produserbartDokument.equals(Produserbaredokumenter.UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV);

        if (erBrevTilUtenlandskTrygdemyndighetFraBehandlingMedIngenFlyt) {
            return hentMottakerForUtenlandskTrygdemyndighet(brevbestillingDto);
        } else if (erBrevTilOrganisasjon) {
            return hentMottakerForOrganisasjon(brevbestillingDto);
        } else if (erBrevTilAnnenPerson) {
            return hentMottakerForAnnenPerson(brevbestillingDto);
        } else if (erBrevTilNorskMyndighet) {
            return hentMottakereForNorskMyndighet(brevbestillingDto);
        } else {
            return brevmottakerService.avklarMottakere(produserbartDokument,
                Mottaker.medRolle(brevbestillingDto.getMottaker()), behandling, false, false);
        }
    }

    private List<Mottaker> hentMottakerForUtenlandskTrygdemyndighet(BrevbestillingDto brevbestillingDto) {
        var mottaker = Mottaker.medRolle(brevbestillingDto.getMottaker());
        mottaker.setInstitusjonID(brevbestillingDto.getInstitusjonID());
        return List.of(mottaker);
    }

    private List<Mottaker> hentMottakerForOrganisasjon(BrevbestillingDto brevbestillingDto) {
        var mottaker = Mottaker.medRolle(brevbestillingDto.getMottaker());
        mottaker.setOrgnr(brevbestillingDto.getOrgnr());
        return List.of(mottaker);
    }

    private List<Mottaker> hentMottakerForAnnenPerson(BrevbestillingDto brevbestillingDto) {
        var mottaker = Mottaker.medRolle(brevbestillingDto.getMottaker());
        mottaker.setPersonIdent(brevbestillingDto.getAnnenPersonMottakerIdent());
        return List.of(mottaker);
    }

    private List<Mottaker> hentMottakereForNorskMyndighet(BrevbestillingDto brevbestillingDto) {
        List<Mottaker> mottakere = new ArrayList<>();
        for (String orgNr : brevbestillingDto.getOrgnrNorskMyndighet()) {
            var mottaker = Mottaker.medRolle(brevbestillingDto.getMottaker());
            mottaker.setOrgnr(orgNr);
            mottakere.add(mottaker);
        }
        return mottakere;
    }

    private void produserOgDistribuerBrev(Behandling behandling, Mottaker mottaker, DokgenBrevbestilling brevbestilling) {
        prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(behandling, mottaker, brevbestilling);
    }

    public DokumentproduksjonsInfo hentDokumentInfo(Produserbaredokumenter produserbartDokument) {
        return dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(produserbartDokument);
    }

    public boolean erTilgjengeligDokgenmal(Produserbaredokumenter produserbartDokument) {
        return dokumentproduksjonsInfoMapper.tilgjengeligeMalerIDokgen().contains(produserbartDokument) && erTogglet(produserbartDokument);
    }

    private boolean erTogglet(Produserbaredokumenter produserbartDokument) {
        return switch (produserbartDokument) {
            case INNHENTING_AV_INNTEKTSOPPLYSNINGER, ORIENTERING_ANMODNING_UNNTAK, AVSLAG_EFTA_STORBRITANNIA, INNVILGELSE_EFTA_STORBRITANNIA ->
                unleash.isEnabled(ToggleName.MELOSYS_KONVENSJON_EFTA_LAND_OG_STORBRITANNIA);
            default -> true;
        };
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

    private String hentSaksbehandlerNavnEllerNull(String ident) {
        return ident != null ? saksbehandlerService.hentNavnForIdent(ident) : null;
    }

    private boolean inneholderArbeidsgiverSomKopimottaker(Collection<KopiMottakerDto> kopimottakere) {
        return kopimottakere.stream().map(KopiMottakerDto::rolle).anyMatch(kopimottaker -> kopimottaker == Mottakerroller.ARBEIDSGIVER);
    }

    private boolean inneholderBrukerSomKopimottaker(Collection<KopiMottakerDto> kopimottakere) {
        return kopimottakere.stream().map(KopiMottakerDto::rolle).anyMatch(kopimottaker -> kopimottaker == Mottakerroller.BRUKER);
    }

    private boolean kopimottakerErFullmektigPrivatperson(KopiMottakerDto kopiMottaker) {
        return kopiMottaker.orgnr() == null && kopiMottaker.rolle() == Mottakerroller.FULLMEKTIG;
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

    private DokgenBrevbestilling.Builder<?> lagDokgenBrevbestilling(BrevbestillingDto brevbestillingDto) {
        return switch (brevbestillingDto.getProduserbardokument()) {
            case MANGELBREV_ARBEIDSGIVER, MANGELBREV_BRUKER -> new MangelbrevBrevbestilling.Builder()
                .medDistribusjonstype(Distribusjonstype.VIKTIG)
                .medInnledningFritekst(brevbestillingDto.getInnledningFritekst())
                .medManglerInfoFritekst(brevbestillingDto.getManglerFritekst())
                .medKontaktpersonNavn(brevbestillingDto.getKontaktpersonNavn())
                .medBrukerSkalHaKopi(inneholderBrukerSomKopimottaker(brevbestillingDto.getKopiMottakere()));
            case TRYGDEAVTALE_GB, TRYGDEAVTALE_US, TRYGDEAVTALE_CAN, TRYGDEAVTALE_AU -> new InnvilgelseBrevbestilling.Builder()
                .medDistribusjonstype(Distribusjonstype.VEDTAK)
                .medInnledningFritekst(brevbestillingDto.getInnledningFritekst())
                .medBegrunnelseFritekst(brevbestillingDto.getBegrunnelseFritekst())
                .medEktefelleFritekst(brevbestillingDto.getEktefelleFritekst())
                .medBarnFritekst(brevbestillingDto.getBarnFritekst())
                .medVirksomhetArbeidsgiverSkalHaKopi(inneholderArbeidsgiverSomKopimottaker(brevbestillingDto.getKopiMottakere()))
                .medNyVurderingBakgrunn(brevbestillingDto.getNyVurderingBakgrunn());
            case INNVILGELSE_FOLKETRYGDLOVEN -> new InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling.Builder()
                .medDistribusjonstype(Distribusjonstype.VEDTAK)
                .medNyVurderingBakgrunn(brevbestillingDto.getNyVurderingBakgrunn())
                .medInnledningFritekst(brevbestillingDto.getInnledningFritekst())
                .medBegrunnelseFritekst(brevbestillingDto.getBegrunnelseFritekst())
                .medTrygdeavgiftFritekst(brevbestillingDto.getTrygdeavgiftFritekst());
            case INNVILGELSE_EFTA_STORBRITANNIA -> new InnvilgelseEftaStorbritanniaBrevbestilling.Builder()
                .medDistribusjonstype(Distribusjonstype.VEDTAK)
                .medNyVurderingBakgrunn(brevbestillingDto.getNyVurderingBakgrunn())
                .medInnledningFritekst(brevbestillingDto.getInnledningFritekst())
                .medBegrunnelseFritekst(brevbestillingDto.getBegrunnelseFritekst())
                .medInnvilgelseFritekst(brevbestillingDto.getFritekst());
            case IKKE_YRKESAKTIV_PLIKTIG_FTRL, IKKE_YRKESAKTIV_FRIVILLIG_FTRL, PLIKTIG_MEDLEM_FTRL -> new DokgenBrevbestilling.Builder<>()
                .medDistribusjonstype(Distribusjonstype.VEDTAK);
            case INNHENTING_AV_INNTEKTSOPPLYSNINGER -> new InnhentingAvInntektsopplysningerBrevbestilling.Builder()
                .medDistribusjonstype(Distribusjonstype.VIKTIG)
                .medSkalViseStandardTekstOmOpplysninger(brevbestillingDto.isSkalViseStandardTekstOmOpplysninger())
                .medFritekst(brevbestillingDto.getFritekst());
            case ORIENTERING_ANMODNING_UNNTAK -> new OrienteringAnmodningUnntakBrevbestilling.Builder()
                .medFritekst(brevbestillingDto.getFritekst());
            case GENERELT_FRITEKSTBREV_BRUKER, GENERELT_FRITEKSTBREV_ARBEIDSGIVER, GENERELT_FRITEKSTBREV_VIRKSOMHET,
                UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV, FRITEKSTBREV -> new FritekstbrevBrevbestilling.Builder()
                .medDistribusjonstype(brevbestillingDto.getDistribusjonstype())
                .medFritekstTittel(brevbestillingDto.getFritekstTittel())
                .medFritekst(brevbestillingDto.getFritekst())
                .medKontaktpersonNavn(brevbestillingDto.getKontaktpersonNavn())
                .medKontaktopplysninger(brevbestillingDto.getKontaktopplysninger())
                .medBrukerSkalHaKopi(inneholderBrukerSomKopimottaker(brevbestillingDto.getKopiMottakere()))
                .medMottakerType(brevbestillingDto.getMottaker())
                .medDokumentTittel(brevbestillingDto.getDokumentTittel())
                .medSaksbehandlerNrToNavn(hentSaksbehandlerNavnEllerNull(brevbestillingDto.getSaksbehandlerNrToIdent()));
            case AVSLAG_MANGLENDE_OPPLYSNINGER -> new AvslagBrevbestilling.Builder()
                .medDistribusjonstype(Distribusjonstype.VEDTAK)
                .medFritekst(brevbestillingDto.getFritekst());
            case MELDING_HENLAGT_SAK -> new HenleggelseBrevbestilling.Builder()
                .medDistribusjonstype(Distribusjonstype.VIKTIG)
                .medFritekst(brevbestillingDto.getFritekst())
                .medBegrunnelseKode(brevbestillingDto.getBegrunnelseKode());
            case GENERELT_FRITEKSTVEDLEGG -> new FritekstvedleggBrevbestilling.Builder()
                .medFritekstvedleggTittel(brevbestillingDto.getFritekstTittel())
                .medFritekstvedleggTekst(brevbestillingDto.getFritekst())
                .medMottakerType(brevbestillingDto.getMottaker());
            case IKKE_YRKESAKTIV_VEDTAKSBREV -> new IkkeYrkesaktivBrevbestilling.Builder()
                .medDistribusjonstype(Distribusjonstype.VEDTAK);
            case VARSELBREV_MANGLENDE_INNBETALING -> new VarselbrevManglendeInnbetalingBrevbestilling.Builder()
                .medDistribusjonstype(Distribusjonstype.VIKTIG)
                .medFakturanummer(brevbestillingDto.getFakturanummer())
                .medBetalingsstatus(brevbestillingDto.getBetalingsstatus())
                .medFullmektigForBetaling(brevbestillingDto.getFullmektigForBetaling())
                .medBetalingsfrist(brevbestillingDto.getBetalingsfrist());
            case VEDTAK_OPPHOERT_MEDLEMSKAP -> new VedtakOpphoertMedlemskapBrevbestilling.Builder()
                .medDistribusjonstype(Distribusjonstype.VEDTAK)
                .medOpphørtDato(brevbestillingDto.getOpphørtDato())
                .medOpphørtBegrunnelseFritekst(brevbestillingDto.getBegrunnelseFritekst());
            case AVSLAG_EFTA_STORBRITANNIA -> new AvslagEftaStorbritanniaBrevbestilling.Builder()
                .medDistribusjonstype(Distribusjonstype.VEDTAK)
                .medInnledningFritekstAvslagEfta(brevbestillingDto.getInnledningFritekst())
                .medBegrunnelseFritekstAvslagEfta(brevbestillingDto.getFritekst());

            default -> new DokgenBrevbestilling.Builder<>().medDistribusjonstype(Distribusjonstype.VIKTIG);
        };
    }
}
