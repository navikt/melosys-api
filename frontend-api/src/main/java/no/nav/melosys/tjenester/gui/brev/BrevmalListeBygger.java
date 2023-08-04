package no.nav.melosys.tjenester.gui.brev;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Distribusjonstype;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.brev.BrevmalListeService;
import no.nav.melosys.service.brev.brevmalliste.BrevAdresse;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import no.nav.melosys.tjenester.gui.dto.brev.*;
import org.springframework.stereotype.Component;

import static java.util.Arrays.asList;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.VIRKSOMHET;

@Component
public class BrevmalListeBygger {
    private final BrevmalListeService brevmalListeService;
    private final BehandlingService behandlingService;
    private final SaksbehandlingRegler saksbehandlingRegler;

    public BrevmalListeBygger(BrevmalListeService brevmalListeService, BehandlingService behandlingService, SaksbehandlingRegler saksbehandlingRegler) {
        this.brevmalListeService = brevmalListeService;
        this.behandlingService = behandlingService;
        this.saksbehandlingRegler = saksbehandlingRegler;
    }

    public List<BrevmalResponse> byggBrevmalDtoListe(long behandlingId) {
        List<MottakerDto> mottakere = hentTilgjengeligeMottakere(behandlingId);

        return mottakere.stream().map(mottaker -> mottakerTilBrevmalDto(behandlingId, mottaker)).toList();
    }

    private BrevmalResponse mottakerTilBrevmalDto(long behandlingId, MottakerDto mottaker) {
        List<Produserbaredokumenter> produserbareDokumenter = brevmalListeService.hentMuligeProduserbaredokumenter(behandlingId, mottaker.getRolle());

        List<BrevmalTypeDto> typer = produserbareDokumenter.stream().map(dokument -> switch (dokument) {
                case MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE ->
                    lagBrevmalTypeDtoForForventetSaksbehandlingstid(dokument);
                case MANGELBREV_BRUKER, MANGELBREV_ARBEIDSGIVER -> lagBrevmalTypeDtoForMangelbrev(dokument, behandlingId);
                case GENERELT_FRITEKSTBREV_BRUKER, GENERELT_FRITEKSTBREV_ARBEIDSGIVER, GENERELT_FRITEKSTBREV_VIRKSOMHET ->
                    lagBrevmalTypeDtoForGenereltFritekstbrev(dokument, behandlingId);
                case UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV ->
                    lagBrevmalTypeDtoForUtenlandskTrygdemyndighetFritekstbrev(dokument, behandlingId);
                case FRITEKSTBREV -> lagBrevmalTypeDtoForFritekstbrev(dokument);
                default -> null;
            })
            .filter(Objects::nonNull)
            .toList();

        return new BrevmalResponse(mottaker, typer);
    }

    private List<MottakerDto> hentTilgjengeligeMottakere(long behandlingId) {
        var behandling = behandlingService.hentBehandling(behandlingId);
        var fagsak = behandling.getFagsak();
        List<MottakerDto> mottakere = new ArrayList<>();

        switch (fagsak.getHovedpartRolle()) {
            case BRUKER -> {
                mottakere.add(lagMottakerMedAdresseOgFeilmelding(behandlingId, Mottakerroller.BRUKER, fagsak.harBrukerRepresentant()));
                if (!saksbehandlingRegler.harTomFlyt(behandling)) {
                    mottakere.add(lagMottakerMedAdresseOgFeilmelding(behandlingId, Mottakerroller.ARBEIDSGIVER, false));
                }
                if (fagsak.erSakstypeTrygdeavtale() && behandling.harLand()) {
                    mottakere.add(lagMottakerMedRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET));
                }
                mottakere.add(lagMottakerMedRolle(Mottakerroller.ANNEN_ORGANISASJON));
                mottakere.add(lagMottakerMedRolle(Mottakerroller.NORSK_MYNDIGHET));
            }
            case VIRKSOMHET -> {
                mottakere.add(lagMottakerMedAdresseOgFeilmelding(behandlingId, Mottakerroller.VIRKSOMHET, false));
                mottakere.add(lagMottakerMedRolle(Mottakerroller.ANNEN_ORGANISASJON));
                mottakere.add(lagMottakerMedRolle(Mottakerroller.NORSK_MYNDIGHET));
            }
            default -> throw new FunksjonellException("Sak må ha hovedpart for å kunne sende brev");

        }
        return mottakere;
    }

    private MottakerDto lagMottakerMedAdresseOgFeilmelding(long behandlingId, Mottakerroller rolle, boolean harBrukerRepresentant) {
        var mottakerDto = new MottakerDto();
        mottakerDto.setType(hentTypeFraRolle(rolle));
        mottakerDto.setRolle(rolle);
        if (harBrukerRepresentant) {
            leggTilAdresseOgFeilmelding(mottakerDto, Mottakerroller.FULLMEKTIG, behandlingId);
        } else {
            leggTilAdresseOgFeilmelding(mottakerDto, rolle, behandlingId);
        }
        return mottakerDto;
    }

    private MottakerDto lagMottakerMedRolle(Mottakerroller rolle) {
        var mottakerDto = new MottakerDto();
        mottakerDto.setRolle(rolle);
        mottakerDto.setType(hentTypeFraRolle(rolle));
        return mottakerDto;
    }

    private String hentTypeFraRolle(Mottakerroller rolle) {
        var mottakerType = switch (rolle) {
            case BRUKER -> MottakerType.BRUKER_ELLER_BRUKERS_FULLMEKTIG;
            case VIRKSOMHET -> MottakerType.VIRKSOMHET;
            case ARBEIDSGIVER -> MottakerType.ARBEIDSGIVER_ELLER_ARBEIDSGIVERS_FULLMEKTIG;
            case ANNEN_ORGANISASJON -> MottakerType.ANNEN_ORGANISASJON;
            case UTENLANDSK_TRYGDEMYNDIGHET -> MottakerType.UTENLANDSK_TRYGDEMYNDIGHET;
            case NORSK_MYNDIGHET -> MottakerType.NORSK_MYNDIGHET;
            default -> throw new FunksjonellException("Vi støtter ikke brev med mottakerrolle: " + rolle.getKode());
        };
        return mottakerType.getBeskrivelse();
    }

    private void leggTilAdresseOgFeilmelding(MottakerDto mottakerDto, Mottakerroller rolle, long behandlingId) {
        try {
            List<BrevAdresse> brevAdresser = brevmalListeService.hentBrevAdresseTilMottakere(behandlingId, rolle);

            if ((rolle == Mottakerroller.BRUKER || rolle == Mottakerroller.FULLMEKTIG) && brevAdresser.stream().allMatch(BrevAdresse::isAdresselinjerEmpty) || brevAdresser.stream().allMatch(BrevAdresse::isPostnrEmpty)) {
                String feilmelding = rolle == Mottakerroller.BRUKER ? Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER.getBeskrivelse() : Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT.getBeskrivelse();
                FeilmeldingDto feilmeldingDto = new FeilmeldingDto(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE.getBeskrivelse(), List.of(new FeilmeldingUnderpunkt(feilmelding)));
                mottakerDto.setFeilmelding(feilmeldingDto);
            } else if (rolle == Mottakerroller.VIRKSOMHET && brevAdresser.stream().allMatch(BrevAdresse::isAdresselinjerEmpty) || brevAdresser.stream().allMatch(BrevAdresse::isPostnrEmpty)) {
                FeilmeldingDto feilmeldingDto = new FeilmeldingDto(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE.getBeskrivelse(), List.of());
                mottakerDto.setFeilmelding(feilmeldingDto);
            } else {
                mottakerDto.setAdresser(brevAdresser.stream().map(MottakerAdresseDto::av).toList());
            }
        } catch (TekniskException e) {
            if ("Finner ikke arbeidsforholddokument".equals(e.getMessage())) {
                FeilmeldingDto feilmeldingDto = new FeilmeldingDto(Kontroll_begrunnelser.INGEN_ARBEIDSGIVERE.getBeskrivelse(), new ArrayList<>());
                mottakerDto.setFeilmelding(feilmeldingDto);
            } else {
                throw new TekniskException(e);
            }
        }
    }

    private BrevmalTypeDto lagBrevmalTypeDtoForForventetSaksbehandlingstid(Produserbaredokumenter produserbartdokument) {
        return new BrevmalTypeDto.Builder().medType(produserbartdokument).build();
    }

    private BrevmalTypeDto lagBrevmalTypeDtoForMangelbrev(Produserbaredokumenter produserbartdokument, long behandlingId) {
        List<FeltvalgAlternativDto> feltvalgAlternativDtos = new ArrayList<>();
        Behandling behandling = behandlingService.hentBehandling(behandlingId);

        if (harStandardTekstIMangelbrev(behandling.getFagsak().getTema(), behandling.getType())) {
            feltvalgAlternativDtos.add(new FeltvalgAlternativDto(FeltvalgAlternativKode.STANDARD));
        }
        feltvalgAlternativDtos.add(new FeltvalgAlternativDto(FeltvalgAlternativKode.FRITEKST.getKode(), "Fritekst (erstatter standardtekst)", true));

        FeltValgDto feltValgDto = new FeltValgDto(feltvalgAlternativDtos, FeltValgType.RADIO);

        return new BrevmalTypeDto.Builder()
            .medType(produserbartdokument)
            .medFelter(asList(
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.INNLEDNING_FRITEKST)
                    .medFeltType(FeltType.FRITEKST)
                    .erPåkrevd()
                    .medValg(feltValgDto)
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.MANGLER_FRITEKST)
                    .medFeltType(FeltType.FRITEKST)
                    .erPåkrevd()
                    .build()
            ))
            .build();
    }

    private boolean harStandardTekstIMangelbrev(Sakstemaer sakstema, Behandlingstyper behandlingstype) {
        return sakstema == Sakstemaer.MEDLEMSKAP_LOVVALG && behandlingstype == Behandlingstyper.FØRSTEGANG;
    }

    private FeltValgDto hentDistribusjonstyper() {
        List<FeltvalgAlternativDto> distribusjonstyper = List.of(
            new FeltvalgAlternativDto(Distribusjonstype.VEDTAK.getKode(), Distribusjonstype.VEDTAK.getBeskrivelse(), false),
            new FeltvalgAlternativDto(Distribusjonstype.VIKTIG.getKode(), Distribusjonstype.VIKTIG.getBeskrivelse(), false),
            new FeltvalgAlternativDto(Distribusjonstype.ANNET.getKode(), Distribusjonstype.ANNET.getBeskrivelse(), false)
        );
        return new FeltValgDto(distribusjonstyper, FeltValgType.RADIO);
    }

    private BrevmalTypeDto lagBrevmalTypeDtoForGenereltFritekstbrev(Produserbaredokumenter produserbartdokument, long behandlingId) {
        return new BrevmalTypeDto.Builder()
            .medType(produserbartdokument)
            .medFelter(asList(
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.DISTRIBUSJONSTYPE)
                    .medHjelpetekst("Type brev må angis slik at bruker får riktig varseltekst om brevet som sendes. Gjelder det et vedtak eller en forespørsel, vil bruker få en påminnelse hvis brevet ikke har blitt lest innen 7 dager.")
                    .medValg(hentDistribusjonstyper())
                    .erPåkrevd()
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.BREV_TITTEL)
                    .medFeltType(FeltType.TEKST)
                    .medHjelpetekst("Tittelen du skriver inn her, vil bli tittelen på brevet når du sender det ut.")
                    .medValg(hentFritekstTittelValg(behandlingId))
                    .medTegnBegrensning(60)
                    .erPåkrevd()
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.STANDARDTEKST_KONTAKTINFORMASJON)
                    .medFeltType(FeltType.SJEKKBOKS)
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.FRITEKST)
                    .medHjelpetekst("Teksten du skriver inn her vil være hovedteksten i brevet du lager.")
                    .medFeltType(FeltType.FRITEKST)
                    .erPåkrevd()
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.VEDLEGG)
                    .medFeltType(FeltType.VEDLEGG)
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.FRITEKSTVEDLEGG)
                    .medFeltType(FeltType.FRITEKSTVEDLEGG)
                    .build()
            ))
            .build();
    }

    @Deprecated(since = "Når dokumentTittel kan gå ut i prod for alle fritekstbrev kan denne slettes og man bruker heller lagBrevmalTypeDtoForGenereltFritekstbrev")
    private BrevmalTypeDto lagBrevmalTypeDtoForFritekstbrev(Produserbaredokumenter produserbartdokument) {
        return new BrevmalTypeDto.Builder()
            .medType(produserbartdokument)
            .medFelter(asList(
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.DISTRIBUSJONSTYPE)
                    .medHjelpetekst("Type brev må angis slik at bruker får riktig varseltekst om brevet som sendes. Gjelder det et vedtak eller en forespørsel, vil bruker få en påminnelse hvis brevet ikke har blitt lest innen 7 dager.")
                    .medValg(hentDistribusjonstyper())
                    .erPåkrevd()
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.BREV_TITTEL)
                    .medFeltType(FeltType.TEKST)
                    .medValg(hentFritekstFeltValg())
                    .medTegnBegrensning(60)
                    .erPåkrevd()
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.DOKUMENT_TITTEL)
                    .medFeltType(FeltType.TEKST)
                    .medTegnBegrensning(60)
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.STANDARDTEKST_KONTAKTINFORMASJON)
                    .medFeltType(FeltType.SJEKKBOKS)
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.FRITEKST)
                    .medHjelpetekst("Teksten du skriver inn her vil være hovedteksten i brevet du lager.")
                    .medFeltType(FeltType.FRITEKST)
                    .erPåkrevd()
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.VEDLEGG)
                    .medFeltType(FeltType.VEDLEGG)
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.FRITEKSTVEDLEGG)
                    .medFeltType(FeltType.FRITEKSTVEDLEGG)
                    .build()
            ))
            .build();
    }

    private FeltValgDto hentFritekstFeltValg() {
        var orienteringBeslutningFeltvalgAlternativDto = new FeltvalgAlternativDto(FeltvalgAlternativKode.ORIENTERING_BESLUTNING);
        var fritekstFeltvalgAlternativDto = new FeltvalgAlternativDto(FeltvalgAlternativKode.FRITEKST, true);

        return new FeltValgDto(List.of(orienteringBeslutningFeltvalgAlternativDto, fritekstFeltvalgAlternativDto),
            FeltValgType.SELECT);
    }

    @Deprecated(since = "Når toggle melosys.trygdeavtale.fritekstbrev er enabled og dokumentTittel klart til å brukes i alle brev kan denne kombineres med lagBrevmalTypeDtoForGenereltFritekstbrev")
    private BrevmalTypeDto lagBrevmalTypeDtoForUtenlandskTrygdemyndighetFritekstbrev(Produserbaredokumenter produserbartdokument, long behandlingId) {
        return new BrevmalTypeDto.Builder()
            .medType(produserbartdokument)
            .medFelter(asList(
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.DISTRIBUSJONSTYPE)
                    .medHjelpetekst("Type brev må angis slik at bruker får riktig varseltekst om brevet som sendes. Gjelder det et vedtak eller en forespørsel, vil bruker få en påminnelse hvis brevet ikke har blitt lest innen 7 dager.")
                    .medValg(hentDistribusjonstyper())
                    .erPåkrevd()
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.BREV_TITTEL)
                    .medFeltType(FeltType.TEKST)
                    .medHjelpetekst("Tittelen du skriver inn her, vil bli tittelen på brevet når du sender det ut.")
                    .medValg(hentFritekstTittelValg(behandlingId))
                    .medTegnBegrensning(60)
                    .erPåkrevd()
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.DOKUMENT_TITTEL)
                    .medFeltType(FeltType.TEKST)
                    .medHjelpetekst("Tittelen du skriver inn her vil bli journalføringstittel.")
                    .medTegnBegrensning(60)
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.FRITEKST)
                    .medHjelpetekst("Teksten du skriver inn her vil være hovedteksten i brevet du lager.")
                    .medFeltType(FeltType.FRITEKST)
                    .erPåkrevd()
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.VEDLEGG)
                    .medFeltType(FeltType.VEDLEGG)
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.FRITEKSTVEDLEGG)
                    .medFeltType(FeltType.FRITEKSTVEDLEGG)
                    .build()
            ))
            .build();
    }

    private FeltValgDto hentFritekstTittelValg(long behandlingId) {
        var behandling = behandlingService.hentBehandling(behandlingId);
        var fagsak = behandling.getFagsak();
        var fritekstFeltvalgAlternativDto = new FeltvalgAlternativDto(FeltvalgAlternativKode.FRITEKST, true);

        if (fagsak.getHovedpartRolle() == VIRKSOMHET) {
            return new FeltValgDto(List.of(fritekstFeltvalgAlternativDto), FeltValgType.SELECT);
        }

        final List<FeltvalgAlternativDto> valgAlternativer = new ArrayList<>();

        switch (fagsak.getType()) {
            case EU_EOS ->
                valgAlternativer.add(new FeltvalgAlternativDto(FeltvalgAlternativKode.HENVENDELSE_OM_TRYGDETILHØRLIGHET));
            case FTRL -> {
                valgAlternativer.add(new FeltvalgAlternativDto(FeltvalgAlternativKode.CONFIRMATION_OF_MEMBERSHIP));
                valgAlternativer.add(new FeltvalgAlternativDto(FeltvalgAlternativKode.BEKREFTELSE_PÅ_MEDLEMSKAP));
                valgAlternativer.add(new FeltvalgAlternativDto(FeltvalgAlternativKode.HENVENDELSE_OM_MEDLEMSKAP));
            }
            case TRYGDEAVTALE ->
                valgAlternativer.add(new FeltvalgAlternativDto(FeltvalgAlternativKode.ENGELSK_FRITEKSTBREV));
        }

        valgAlternativer.add(fritekstFeltvalgAlternativDto);

        return new FeltValgDto(valgAlternativer, FeltValgType.SELECT);
    }
}
