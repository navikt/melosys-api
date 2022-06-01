package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.brev.BrevAdresse;
import no.nav.melosys.service.brev.BrevbestillingService;
import no.nav.melosys.service.dokument.BrevmottakerService;
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
import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

@Protected
@RestController
@RequestMapping("/dokumenter/v2") //TODO: Endre url når gjennomtestet
@Api(tags = {"dokumenterv2"})
@RequestScope
public class BrevbestillingTjeneste {
    private static final String BRUKER_ELLER_BRUKERS_FULLMEKTIG = "Bruker eller brukers fullmektig";
    private static final String VIRKSOMHET_TYPE = "Virksomhet";
    private static final String ARBEIDSGIVER_ELLER_ARBEIDSGIVERS_FULLMEKTIG = "Arbeidsgiver eller arbeidsgivers fullmektig";

    private final BrevbestillingService brevbestillingService;
    private final BehandlingService behandlingService;
    private final BrevmottakerService brevmottakerService;
    private final Aksesskontroll aksesskontroll;

    public BrevbestillingTjeneste(BrevbestillingService brevbestillingService,
                                  BehandlingService behandlingService,
                                  BrevmottakerService brevmottakerService, Aksesskontroll aksesskontroll) {
        this.brevbestillingService = brevbestillingService;
        this.behandlingService = behandlingService;
        this.brevmottakerService = brevmottakerService;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping(value = "/tilgjengelige-maler/{behandlingID}", produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Henter alle tilgjengelige brevmaler for en behandling", response = BrevmalDto.class, responseContainer = "List")
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
        List<Produserbaredokumenter> produserbareDokumenter = brevbestillingService.hentMuligeProduserbaredokumenter(behandlingId);

        return produserbareDokumenter.stream().map(p -> {
                Aktoersroller hovedMottaker = brevmottakerService.hentMottakerliste(p, behandlingId).getHovedMottaker();
                return switch (p) {
                    case MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE -> lagBrevMalDtoForForventetSaksbehandlingstid(p, hovedMottaker, behandlingId);
                    case MANGELBREV_BRUKER, MANGELBREV_ARBEIDSGIVER -> lagBrevMalDtoForMangelbrev(p, hovedMottaker, behandlingId);
                    case GENERELT_FRITEKSTBREV_BRUKER, GENERELT_FRITEKSTBREV_ARBEIDSGIVER, GENERELT_FRITEKSTBREV_VIRKSOMHET -> lagBrevMalDtoForFritekstbrev(p, hovedMottaker, behandlingId);
                    default -> null;
                };
            })
            .filter(Objects::nonNull)
            .toList();
    }

    private BrevmalDto lagBrevMalDtoForForventetSaksbehandlingstid(Produserbaredokumenter produserbartdokument, Aktoersroller hovedMottaker, long behandlingId) {
        var builder = new MottakerDto.Builder()
            .medType(BRUKER_ELLER_BRUKERS_FULLMEKTIG)
            .medRolle(hovedMottaker);

        leggTilAdresseOgFeilmelding(builder, produserbartdokument, hovedMottaker, behandlingId);

        return new BrevmalDto.Builder()
            .medType(produserbartdokument)
            .medMuligeMottakere(singletonList(builder.build()))
            .build();
    }

    private BrevmalDto lagBrevMalDtoForMangelbrev(Produserbaredokumenter produserbartdokument, Aktoersroller hovedMottaker, long behandlingId) {
        List<FeltvalgAlternativDto> feltvalgAlternativDtos = new ArrayList<>();
        Behandling behandling = behandlingService.hentBehandling(behandlingId);

        if (behandling.getType() == Behandlingstyper.SOEKNAD || behandling.erKlage()) {
            feltvalgAlternativDtos.add(new FeltvalgAlternativDto(FeltvalgAlternativKode.STANDARD));
        }
        feltvalgAlternativDtos.add(new FeltvalgAlternativDto(FeltvalgAlternativKode.FRITEKST.getKode(), "Fritekst (erstatter standardtekst)", true));

        FeltValgDto feltValgDto = new FeltValgDto(feltvalgAlternativDtos, FeltValgType.RADIO);
        List<MottakerDto> mottakere = hentMottakereForBrev(produserbartdokument, hovedMottaker, behandlingId);

        return new BrevmalDto.Builder()
            .medType(produserbartdokument)
            .medFelter(asList(
                new BrevmalFeltDto.Builder()
                    .medKode(BrevmalFeltKode.INNLEDNING_FRITEKST.getKode())
                    .medBeskrivelse(BrevmalFeltKode.INNLEDNING_FRITEKST.getBeskrivelse())
                    .medFeltType(FeltType.FRITEKST)
                    .erPåkrevd()
                    .medValg(feltValgDto)
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKode(BrevmalFeltKode.MANGLER_FRITEKST.getKode())
                    .medBeskrivelse(BrevmalFeltKode.MANGLER_FRITEKST.getBeskrivelse())
                    .medFeltType(FeltType.FRITEKST)
                    .erPåkrevd()
                    .build()
            ))
            .medMuligeMottakere(mottakere)
            .medMottakereHjelpetekst("Hvis bruker eller arbeidsgiver har fullmektig som er lagt inn i sidemenyen, vil brevet automatisk bli sendt til denne.")
            .build();
    }

    private BrevmalDto lagBrevMalDtoForFritekstbrev(Produserbaredokumenter produserbartdokument, Aktoersroller hovedMottaker, long behandlingId) {
        List<MottakerDto> mottakere = hentMottakereForBrev(produserbartdokument, hovedMottaker, behandlingId);

        return new BrevmalDto.Builder()
            .medType(produserbartdokument)
            .medFelter(asList(
                new BrevmalFeltDto.Builder()
                    .medKode(BrevmalFeltKode.BREV_TITTEL.getKode())
                    .medBeskrivelse(BrevmalFeltKode.BREV_TITTEL.getBeskrivelse())
                    .medFeltType(FeltType.TEKST)
                    .medHjelpetekst("Tittelen du skriver inn her, vil bli tittelen på brevet når du sender det ut.")
                    .medValg(hentFritekstTittelValg(behandlingId))
                    .medTegnBegrensning(60)
                    .erPåkrevd()
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKode(BrevmalFeltKode.STANDARDTEKST_KONTAKTINFORMASJON.getKode())
                    .medBeskrivelse(BrevmalFeltKode.STANDARDTEKST_KONTAKTINFORMASJON.getBeskrivelse())
                    .medFeltType(FeltType.SJEKKBOKS)
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKode(BrevmalFeltKode.FRITEKST.getKode())
                    .medBeskrivelse(BrevmalFeltKode.FRITEKST.getBeskrivelse())
                    .medFeltType(FeltType.FRITEKST)
                    .erPåkrevd()
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKode(BrevmalFeltKode.VEDLEGG.getKode())
                    .medBeskrivelse(BrevmalFeltKode.VEDLEGG.getBeskrivelse())
                    .medFeltType(FeltType.VEDLEGG)
                    .build()
            ))
            .medMuligeMottakere(mottakere)
            .medMottakereHjelpetekst("Hvis bruker eller arbeidsgiver har fullmektig som er lagt inn i sidemenyen, vil brevet automatisk bli sendt til denne.")
            .build();
    }

    private List<MottakerDto> hentMottakereForBrev(Produserbaredokumenter produserbartdokument, Aktoersroller hovedMottaker, long behandlingId) {
        List<MottakerDto> mottakere = new ArrayList<>();
        var builder = new MottakerDto.Builder()
            .medType(mapType(hovedMottaker))
            .medRolle(hovedMottaker);

        leggTilAdresseOgFeilmelding(builder, produserbartdokument, hovedMottaker, behandlingId);

        mottakere.add(builder.build());
        if (hovedMottaker == Aktoersroller.ARBEIDSGIVER || hovedMottaker == Aktoersroller.VIRKSOMHET) {
            mottakere.add(
                new MottakerDto.Builder()
                    .medType("Annen organisasjon")
                    .medRolle(Aktoersroller.ARBEIDSGIVER)
                    .orgnrSettesAvSaksbehandler()
                    .build()
            );
        }
        return mottakere;
    }

    private String mapType(Aktoersroller hovedmottaker) {
        return switch (hovedmottaker) {
            case BRUKER -> BRUKER_ELLER_BRUKERS_FULLMEKTIG;
            case VIRKSOMHET -> VIRKSOMHET_TYPE;
            default -> ARBEIDSGIVER_ELLER_ARBEIDSGIVERS_FULLMEKTIG;
        };
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

    private void leggTilAdresseOgFeilmelding(MottakerDto.Builder builder, Produserbaredokumenter produserbaredokumenter, Aktoersroller aktoersroller, long behandlingId) {
        try {
            var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(produserbaredokumenter, aktoersroller, behandlingId);
            if (aktoersroller == Aktoersroller.BRUKER && brevAdresser.stream().allMatch(BrevAdresse::isAdresselinjerEmpty)) {
                builder.medFeilmelding("Bruker har ingen registrert adresse.");
            } else {
                builder.medAdresse(brevAdresser.stream().map(MottakerAdresseDto::av).toList());
            }
        } catch (TekniskException e) {
            if ("Finner ikke arbeidsforholddokument".equals(e.getMessage())) {
                builder.medFeilmelding("Finner ingen arbeidsgivere. Hent registeropplysninger.");
            } else {
                throw new TekniskException(e);
            }
        }
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
