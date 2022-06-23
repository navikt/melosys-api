package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.brev.BrevAdresse;
import no.nav.melosys.service.brev.BrevbestillingService;
import no.nav.melosys.service.dokument.MuligeMottakereDto;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.brev.*;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.RequestScope;

import static java.util.Arrays.asList;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

@Protected
@RestController
@RequestMapping("/dokumenter/v2") //TODO: Endre url når gjennomtestet
@Api(tags = {"dokumenterv2"})
@RequestScope
public class BrevbestillingTjeneste {

    private final BrevbestillingService brevbestillingService;
    private final BehandlingService behandlingService;
    private final Aksesskontroll aksesskontroll;

    public BrevbestillingTjeneste(BrevbestillingService brevbestillingService,
                                  BehandlingService behandlingService,
                                  Aksesskontroll aksesskontroll) {
        this.brevbestillingService = brevbestillingService;
        this.behandlingService = behandlingService;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping(value = "/tilgjengelige-maler/{behandlingID}", produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Henter alle tilgjengelige brevmaler for en behandling", response = BrevmalTypeDto.class, responseContainer = "List")
    public List<BrevmalDto> hentTilgjengeligeMaler(@PathVariable long behandlingID) {
        aksesskontroll.autoriser(behandlingID);
        return byggBrevmalListe(behandlingID);
    }

    @PostMapping(value = "/mulige-mottakere/{behandlingID}", produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Henter alle mulige mottakere for valgt dokumenttype, og organisasjonsnummer dersom hovedmottaker ikke er bruker")
    public MuligeMottakereDto hentTilgjengeligeMottakere(@PathVariable long behandlingID,
                                                         @RequestBody HentMuligeMottakereRequestDto hentMuligeMottakereRequestDto) {
        aksesskontroll.autoriser(behandlingID);
        return brevbestillingService.hentMuligeMottakere(hentMuligeMottakereRequestDto.produserbartdokument(), behandlingID, hentMuligeMottakereRequestDto.orgnr());
    }

    @PostMapping(value = "pdf/brev/utkast/{behandlingID}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_PDF_VALUE)
    @ApiOperation(value = "Produser utkast")
    public ResponseEntity<byte[]> produserUtkast(@PathVariable long behandlingID,
                                                 @RequestBody BrevbestillingDto brevbestillingDto) {
        aksesskontroll.autoriser(behandlingID);
        BrevbestillingRequest brevbestillingRequest = brevbestillingDto.tilRequestBuilder()
            .medBestillersId(SubjectHandler.getInstance().getUserID())
            .build();
        byte[] pdf = brevbestillingService.produserUtkast(behandlingID, brevbestillingRequest);
        return new ResponseEntity<>(pdf, genPdfHeaders("utkast_" + behandlingID), HttpStatus.OK);
    }

    @PostMapping("opprett/{behandlingID}")
    @ApiOperation(value = "Produser brev gjennom melosys-dokgen")
    public void produserBrev(@PathVariable("behandlingID") long behandlingID,
                             @RequestBody BrevbestillingDto brevbestillingDto) {
        aksesskontroll.autoriser(behandlingID);
        BrevbestillingRequest brevbestillingRequest = brevbestillingDto.tilRequestBuilder()
            .medBestillersId(SubjectHandler.getInstance().getUserID())
            .build();
        brevbestillingService.produserBrev(behandlingID, brevbestillingRequest);
    }

    private List<BrevmalDto> byggBrevmalListe(long behandlingId) {
        List<MottakerDto> mottakere = hentTilgjengeligeMottakere(behandlingId);

        return mottakere.stream().map(mottaker -> {
            List<Produserbaredokumenter> produserbareDokumenter = brevbestillingService.hentMuligeProduserbaredokumenter(behandlingId, mottaker.getRolle());

            List<BrevmalTypeDto> typer = produserbareDokumenter.stream().map(p -> switch (p) {
                    case MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE ->
                        lagBrevmalTypeDtoForForventetSaksbehandlingstid(p);
                    case MANGELBREV_BRUKER, MANGELBREV_ARBEIDSGIVER ->
                        lagBrevmalTypeDtoForMangelbrev(p, behandlingId);
                    case GENERELT_FRITEKSTBREV_BRUKER, GENERELT_FRITEKSTBREV_ARBEIDSGIVER, GENERELT_FRITEKSTBREV_VIRKSOMHET ->
                        lagBrevmalTypeDtoForFritekstbrev(p, behandlingId);
                    default -> null;
                })
                .filter(Objects::nonNull)
                .toList();
            return new BrevmalDto(mottaker, typer);
        }).toList();
    }

    private List<MottakerDto> hentTilgjengeligeMottakere(long behandlingId) {
        var fagsak = behandlingService.hentBehandling(behandlingId).getFagsak();
        List<MottakerDto> mottakere = new ArrayList<>();

        if (fagsak.getHovedpartRolle() == BRUKER) {
            mottakere.addAll(lagMottakereForRolle(behandlingId, BRUKER));
            mottakere.addAll(lagMottakereForRolle(behandlingId, ARBEIDSGIVER));
        } else if (fagsak.getHovedpartRolle() == VIRKSOMHET) {
            mottakere.addAll(lagMottakereForRolle(behandlingId, VIRKSOMHET));
        } else {
            throw new FunksjonellException("Sak må ha hovedpart for å kunne sende brev");
        }
        return mottakere;
    }

    private List<MottakerDto> lagMottakereForRolle(long behandlingId, Aktoersroller rolle) {
        List<MottakerDto> mottakere = new ArrayList<>();
        var builder = new MottakerDto.Builder()
            .medType(mapType(rolle))
            .medRolle(rolle);

        leggTilAdresseOgFeilmelding(builder, rolle, behandlingId);

        mottakere.add(builder.build());

        if (rolle == Aktoersroller.ARBEIDSGIVER || rolle == Aktoersroller.VIRKSOMHET) {
            mottakere.add(
                new MottakerDto.Builder()
                    .medType(MottakerType.ANNEN_ORGANISASJON)
                    .medRolle(Aktoersroller.ARBEIDSGIVER)
                    .orgnrSettesAvSaksbehandler()
                    .build()
            );
        }
        return mottakere;
    }

    private MottakerType mapType(Aktoersroller hovedmottaker) {
        return switch (hovedmottaker) {
            case BRUKER -> MottakerType.BRUKER_ELLER_BRUKERS_FULLMEKTIG;
            case VIRKSOMHET -> MottakerType.VIRKSOMHET;
            case ARBEIDSGIVER -> MottakerType.ARBEIDSGIVER_ELLER_ARBEIDSGIVERS_FULLMEKTIG;
            default -> throw new FunksjonellException("Vi støtter ikke brev med hovedmottaker: " + hovedmottaker.getKode());
        };
    }

    private void leggTilAdresseOgFeilmelding(MottakerDto.Builder builder, Aktoersroller aktoersroller, long behandlingId) {
        try {
            var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(aktoersroller, behandlingId);
            if (aktoersroller == BRUKER && brevAdresser.stream().allMatch(BrevAdresse::isAdresselinjerEmpty)) {
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

        if (behandling.getType() == Behandlingstyper.SOEKNAD || behandling.erKlage()) {
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

    private BrevmalTypeDto lagBrevmalTypeDtoForFritekstbrev(Produserbaredokumenter produserbartdokument, long behandlingId) {
        return new BrevmalTypeDto.Builder()
            .medType(produserbartdokument)
            .medFelter(asList(
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
                    .medFeltType(FeltType.FRITEKST)
                    .erPåkrevd()
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKodeOgBeskrivelse(BrevmalFeltKode.VEDLEGG)
                    .medFeltType(FeltType.VEDLEGG)
                    .build()
            ))
            .build();
    }

    private FeltValgDto hentFritekstTittelValg(long behandlingId) {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        Sakstyper fagsakType = behandling.getFagsak().getType();

        final List<FeltvalgAlternativDto> valgAlternativer = new ArrayList<>();

        switch (fagsakType) {
            case EU_EOS:
                valgAlternativer.add(new FeltvalgAlternativDto(FeltvalgAlternativKode.HENVENDELSE_OM_TRYGDETILHØRLIGHET));
                break;
            case FTRL:
                valgAlternativer.add(new FeltvalgAlternativDto(FeltvalgAlternativKode.CONFIRMATION_OF_MEMBERSHIP));
                valgAlternativer.add(new FeltvalgAlternativDto(FeltvalgAlternativKode.BEKREFTELSE_PÅ_MEDLEMSKAP));
            case TRYGDEAVTALE:
                valgAlternativer.add(new FeltvalgAlternativDto(FeltvalgAlternativKode.HENVENDELSE_OM_MEDLEMSKAP));
        }

        valgAlternativer.add(new FeltvalgAlternativDto(FeltvalgAlternativKode.FRITEKST.getKode(), FeltvalgAlternativKode.FRITEKST.getBeskrivelse(), true));

        return new FeltValgDto(valgAlternativer, FeltValgType.SELECT);
    }

    private HttpHeaders genPdfHeaders(String navn) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String filename = navn + ".pdf";
        ContentDisposition.Builder contentDisposition = ContentDisposition.builder("inline");
        contentDisposition.filename(filename);
        headers.setContentDisposition(contentDisposition.build());
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return headers;
    }
}
