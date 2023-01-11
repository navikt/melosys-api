package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Distribusjonstype;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.brev.BrevAdresse;
import no.nav.melosys.service.brev.BrevbestillingService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import no.nav.melosys.tjenester.gui.dto.brev.*;
import org.springframework.stereotype.Component;

import static java.util.Arrays.asList;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;

@Component
public class BrevmalListeBygger {

    private final BrevbestillingService brevbestillingService;
    private final BehandlingService behandlingService;
    private final Unleash unleash;

    public BrevmalListeBygger(BrevbestillingService brevbestillingService, BehandlingService behandlingService, Unleash unleash) {
        this.brevbestillingService = brevbestillingService;
        this.behandlingService = behandlingService;
        this.unleash = unleash;
    }

    public List<BrevmalDto> byggBrevmalDtoListe(long behandlingId) {
        List<MottakerDto> mottakere = hentTilgjengeligeMottakere(behandlingId);

        return mottakere.stream().map(mottaker -> mottakerTilBrevmalDto(behandlingId, mottaker)).toList();
    }

    private BrevmalDto mottakerTilBrevmalDto(long behandlingId, MottakerDto mottaker) {
        List<Produserbaredokumenter> produserbareDokumenter = brevbestillingService.hentMuligeProduserbaredokumenter(behandlingId, mottaker.getRolle());

        List<BrevmalTypeDto> typer = produserbareDokumenter.stream().map(dokument -> switch (dokument) {
                case MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE ->
                    lagBrevmalTypeDtoForForventetSaksbehandlingstid(dokument);
                case MANGELBREV_BRUKER, MANGELBREV_ARBEIDSGIVER -> lagBrevmalTypeDtoForMangelbrev(dokument, behandlingId);
                case GENERELT_FRITEKSTBREV_BRUKER, GENERELT_FRITEKSTBREV_ARBEIDSGIVER,
                    GENERELT_FRITEKSTBREV_VIRKSOMHET -> lagBrevmalTypeDtoForGenereltFritekstbrev(dokument, behandlingId);
                case FRITEKSTBREV -> lagBrevmalTypeDtoForFritekstbrev(dokument, behandlingId);
                default -> null;
            })
            .filter(Objects::nonNull)
            .toList();

        return new BrevmalDto(mottaker, typer);
    }

    private List<MottakerDto> hentTilgjengeligeMottakere(long behandlingId) {
        var behandling = behandlingService.hentBehandling(behandlingId);
        var fagsak = behandling.getFagsak();
        List<MottakerDto> mottakere = new ArrayList<>();

        switch (fagsak.getHovedpartRolle()) {
            case BRUKER -> {
                mottakere.add(lagMottakerForRolle(behandlingId, BRUKER));
                if (!SaksbehandlingRegler.harTomFlyt(behandling, unleash.isEnabled("melosys.folketrygden.mvp"))) {
                    mottakere.add(lagMottakerForRolle(behandlingId, ARBEIDSGIVER));
                }
                mottakere.add(lagMottakerAnnenOrganisasjon(ARBEIDSGIVER));
                mottakere.add(lagMottakerAndreOffentligeEtater());
            }
            case VIRKSOMHET -> {
                mottakere.add(lagMottakerForRolle(behandlingId, VIRKSOMHET));
                mottakere.add(lagMottakerAnnenOrganisasjon(VIRKSOMHET));
                mottakere.add(lagMottakerAndreOffentligeEtater());
            }
            default -> throw new FunksjonellException("Sak må ha hovedpart for å kunne sende brev");

        }
        return mottakere;
    }

    private MottakerDto lagMottakerForRolle(long behandlingId, Aktoersroller rolle) {
        var builder = new MottakerDto.Builder()
            .medType(mapType(rolle))
            .medRolle(rolle);

        leggTilAdresseOgFeilmelding(builder, rolle, behandlingId);

        return builder.build();
    }

    private MottakerDto lagMottakerAnnenOrganisasjon(Aktoersroller tilhørendeRolle) {
        return new MottakerDto.Builder()
            .medType(MottakerType.ANNEN_ORGANISASJON)
            .medRolle(tilhørendeRolle)
            .orgnrSettesAvSaksbehandler()
            .build();
    }

    private MottakerDto lagMottakerAndreOffentligeEtater() {
        return new MottakerDto.Builder()
            .medType(MottakerType.ANDRE_OFFENTLIGE_ETATER)
            .medRolle(OFFENTLIG_ETAT)
            .orgnrSettesAvSaksbehandler()
            .build();
    }

    private MottakerType mapType(Aktoersroller hovedmottaker) {
        return switch (hovedmottaker) {
            case BRUKER -> MottakerType.BRUKER_ELLER_BRUKERS_FULLMEKTIG;
            case VIRKSOMHET -> MottakerType.VIRKSOMHET;
            case ARBEIDSGIVER -> MottakerType.ARBEIDSGIVER_ELLER_ARBEIDSGIVERS_FULLMEKTIG;
            default ->
                throw new FunksjonellException("Vi støtter ikke brev med hovedmottaker: " + hovedmottaker.getKode());
        };
    }

    private void leggTilAdresseOgFeilmelding(MottakerDto.Builder builder, Aktoersroller aktoersroller, long behandlingId) {
        try {
            var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(aktoersroller, behandlingId);
            if ((aktoersroller == BRUKER || aktoersroller == VIRKSOMHET) && brevAdresser.stream().allMatch(BrevAdresse::isAdresselinjerEmpty)) {
                builder.medFeilmelding(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE.getBeskrivelse());
            } else {
                builder.medAdresse(brevAdresser.stream().map(MottakerAdresseDto::av).toList());
            }
        } catch (TekniskException e) {
            if ("Finner ikke arbeidsforholddokument".equals(e.getMessage())) {
                builder.medFeilmelding(Kontroll_begrunnelser.INGEN_ARBEIDSGIVERE.getBeskrivelse());
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

    private BrevmalTypeDto lagBrevmalTypeDtoForFritekstbrev(Produserbaredokumenter produserbartdokument, long behandlingId) {
        return new BrevmalTypeDto.Builder()
            .medType(produserbartdokument)
            .medFelter(asList(
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.BREV_TITTEL)
                    .medFeltType(FeltType.TEKST)
                    .medValg(hentFritekstTittelValg(behandlingId))
                    .medTegnBegrensning(60)
                    .erPåkrevd()
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.DOKUMENTTITTEL)
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

    private FeltValgDto hentFritekstTittelValg(long behandlingId) {
        var behandling = behandlingService.hentBehandling(behandlingId);
        var fagsak = behandling.getFagsak();
        var fritekstFeltvalgAlternativDto = new FeltvalgAlternativDto(FeltvalgAlternativKode.FRITEKST, true);

        if (fagsak.getHovedpartRolle() == VIRKSOMHET) {
            return new FeltValgDto(List.of(fritekstFeltvalgAlternativDto), FeltValgType.SELECT);
        }

        final List<FeltvalgAlternativDto> valgAlternativer = new ArrayList<>();

        switch (fagsak.getType()) {
            case EU_EOS:
                valgAlternativer.add(new FeltvalgAlternativDto(FeltvalgAlternativKode.HENVENDELSE_OM_TRYGDETILHØRLIGHET));
                break;
            case FTRL:
                valgAlternativer.add(new FeltvalgAlternativDto(FeltvalgAlternativKode.CONFIRMATION_OF_MEMBERSHIP));
                valgAlternativer.add(new FeltvalgAlternativDto(FeltvalgAlternativKode.BEKREFTELSE_PÅ_MEDLEMSKAP));
            case TRYGDEAVTALE:
                valgAlternativer.add(new FeltvalgAlternativDto(FeltvalgAlternativKode.HENVENDELSE_OM_MEDLEMSKAP));
        }

        valgAlternativer.add(fritekstFeltvalgAlternativDto);

        return new FeltValgDto(valgAlternativer, FeltValgType.SELECT);
    }
}
