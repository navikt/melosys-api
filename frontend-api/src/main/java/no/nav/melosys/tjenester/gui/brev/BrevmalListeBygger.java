package no.nav.melosys.tjenester.gui.brev;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.brev.BrevAdresse;
import no.nav.melosys.service.brev.BrevmalListeService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import no.nav.melosys.tjenester.gui.dto.brev.*;
import org.springframework.stereotype.Component;

import static java.util.Arrays.asList;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.VIRKSOMHET;
import static no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser.*;
import static no.nav.melosys.tjenester.gui.brev.BrevFelt.*;

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
                    lagBrevmalForMELDING_FORVENTET_SAKSBEHANDLINGSTID(dokument);
                case MANGELBREV_BRUKER, MANGELBREV_ARBEIDSGIVER ->
                    lagBrevmalForMANGELBREV(dokument, behandlingId);
                case GENERELT_FRITEKSTBREV_BRUKER, GENERELT_FRITEKSTBREV_ARBEIDSGIVER, GENERELT_FRITEKSTBREV_VIRKSOMHET ->
                    lagBrevmalForGENERELT_FRITEKSTBREV(dokument, behandlingId);
                case UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV ->
                    lagBrevmalForUTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV(dokument, behandlingId);
                case FRITEKSTBREV ->
                    lagBrevmalForFRITEKSTBREV(dokument);
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
                mottakere.add(lagMottakerMedAdresseOgFeilmelding(behandlingId, Mottakerroller.BRUKER, fagsak.harBrukerFullmektig()));
                if (!saksbehandlingRegler.harIngenFlyt(behandling)) {
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

            if (brevAdresser.stream().allMatch(BrevAdresse::getUgyldig)) {
                switch (rolle) {
                    case BRUKER -> {
                        String feilmelding = MANGLENDE_REGISTRERTE_ADRESSE_BRUKER.getBeskrivelse().replace("Ingen gyldig adresse funnet. ", "");
                        mottakerDto.setFeilmelding(new FeilmeldingDto(MANGLENDE_REGISTRERTE_ADRESSE.getBeskrivelse(), List.of(new FeilmeldingUnderpunkt(feilmelding))));
                    }
                    case FULLMEKTIG -> {
                        String feilmelding = MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT.getBeskrivelse().replace("\"Ingen gyldig adresse funnet. ", "");
                        mottakerDto.setFeilmelding(new FeilmeldingDto(MANGLENDE_REGISTRERTE_ADRESSE.getBeskrivelse(), List.of(new FeilmeldingUnderpunkt(feilmelding))));
                    }
                    case VIRKSOMHET, ARBEIDSGIVER -> {
                        FeilmeldingDto feilmeldingDto = new FeilmeldingDto(MANGLENDE_REGISTRERTE_ADRESSE.getBeskrivelse(), List.of());
                        mottakerDto.setFeilmelding(feilmeldingDto);
                    }
                    default ->
                        throw new FunksjonellException("Vi har ikke støtte for tom adresse for " + rolle); // Sjekk med Eirik/MELOSYS-6135 hva med arbeidsgiver
                }
            } else {
                mottakerDto.setAdresser(brevAdresser);
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

    private BrevmalTypeDto lagBrevmalForMELDING_FORVENTET_SAKSBEHANDLINGSTID(Produserbaredokumenter produserbartdokument) {
        return new BrevmalTypeDto.Builder().medType(produserbartdokument).build();
    }

    private BrevmalTypeDto lagBrevmalForMANGELBREV(Produserbaredokumenter produserbartdokument, long behandlingId) {
        List<FeltvalgAlternativDto> feltvalgAlternativDtos = new ArrayList<>();
        Behandling behandling = behandlingService.hentBehandling(behandlingId);

        if (harStandardTekstIMangelbrev(behandling)) {
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
                FELT_MANGLER_FRITEKST
            ))
            .build();
    }

    private boolean harStandardTekstIMangelbrev(Behandling behandling) {
        return behandling.getFagsak().getTema() == Sakstemaer.MEDLEMSKAP_LOVVALG && behandling.getType() == Behandlingstyper.FØRSTEGANG;
    }

    private BrevmalTypeDto lagBrevmalForGENERELT_FRITEKSTBREV(Produserbaredokumenter produserbartdokument, long behandlingId) {
        return new BrevmalTypeDto.Builder()
            .medType(produserbartdokument)
            .medFelter(asList(
                FELT_DISTRIBUSJONSTYPE,
                lagBrevTittelFelt(hentBrevTittelValg(behandlingId)),
                FELT_DOKUMENT_TITTEL,
                FELT_STANDARDTEKST_SJEKKBOKS,
                FELT_FRITEKST,
                FELT_VEDLEGG,
                FELT_FRITEKSTVEDLEGG
            ))
            .build();
    }

    private BrevmalTypeDto lagBrevmalForFRITEKSTBREV(Produserbaredokumenter produserbartdokument) {
        return new BrevmalTypeDto.Builder()
            .medType(produserbartdokument)
            .medFelter(asList(
                FELT_DISTRIBUSJONSTYPE,
                lagBrevTittelFelt(hentBrevTittelValg()),
                FELT_DOKUMENT_TITTEL,
                FELT_FRITEKST,
                FELT_VEDLEGG,
                FELT_FRITEKSTVEDLEGG
            ))
            .build();
    }

    private BrevmalTypeDto lagBrevmalForUTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV(Produserbaredokumenter produserbartdokument, long behandlingId) {
        return new BrevmalTypeDto.Builder()
            .medType(produserbartdokument)
            .medFelter(asList(
                FELT_DISTRIBUSJONSTYPE,
                lagBrevTittelFelt(hentBrevTittelValg(behandlingId)),
                FELT_DOKUMENT_TITTEL,
                FELT_FRITEKST,
                FELT_VEDLEGG,
                FELT_FRITEKSTVEDLEGG
            ))
            .build();
    }

    private static FeltValgDto hentBrevTittelValg() {
        return new FeltValgDto(
            List.of(
                new FeltvalgAlternativDto(FeltvalgAlternativKode.ORIENTERING_BESLUTNING),
                new FeltvalgAlternativDto(FeltvalgAlternativKode.FRITEKST, true)),
            FeltValgType.SELECT);
    }

    private FeltValgDto hentBrevTittelValg(long behandlingId) {
        var fagsak = behandlingService.hentBehandling(behandlingId).getFagsak();

        if (fagsak.getHovedpartRolle() == VIRKSOMHET) {
            return new FeltValgDto(
                List.of(new FeltvalgAlternativDto(FeltvalgAlternativKode.FRITEKST, true)),
                FeltValgType.SELECT);
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
        valgAlternativer.add(new FeltvalgAlternativDto(FeltvalgAlternativKode.FRITEKST, true));

        return new FeltValgDto(valgAlternativer, FeltValgType.SELECT);
    }
}
