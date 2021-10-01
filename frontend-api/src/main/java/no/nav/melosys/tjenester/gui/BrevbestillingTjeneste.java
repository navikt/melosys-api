package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.RequestScope;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

@Protected
@RestController
@RequestMapping("/dokumenter/v2") //TODO Endre url når gjennomtestet
@Api(tags = {"dokumenterv2"})
@RequestScope
public class BrevbestillingTjeneste {
    private static final String BRUKER_ELLER_BRUKERS_FULLMEKTIG = "Bruker eller brukers fullmektig";

    private final BrevbestillingService brevbestillingService;
    private final BehandlingService behandlingService;
    private final BrevmottakerService brevmottakerService;
    private final Aksesskontroll aksesskontroll;

    @Autowired
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
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        return brevbestillingService.hentMuligeMottakere(hentMuligeMottakereRequestDto.produserbartdokument(), behandling, hentMuligeMottakereRequestDto.orgnr());
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
        return new ResponseEntity<>(pdf, genPdfHeaders("utkast_" + behandlingID, false), HttpStatus.OK);
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
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        List<Produserbaredokumenter> produserbareDokumenter = brevbestillingService.hentMuligeProduserbaredokumenter(behandling);

        List<BrevmalDto> maler = new ArrayList<>();
        for(Produserbaredokumenter p : produserbareDokumenter) {
            Aktoersroller hovedMottaker = brevmottakerService.hentMottakerliste(p, behandling).getHovedMottaker();
            switch (p) {
                case MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD:
                case MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE:
                    maler.add(lagBrevMalDtoForForventetSaksbehandlingstid(p, hovedMottaker, behandling));
                    break;
                case MANGELBREV_BRUKER:
                case MANGELBREV_ARBEIDSGIVER:
                    maler.add(lagBrevMalDtoForMangelbrev(p, hovedMottaker, behandling));
                    break;
                default:
                    break;
            }
        }
        return maler;
    }

    private BrevmalDto lagBrevMalDtoForForventetSaksbehandlingstid(Produserbaredokumenter produserbartdokument, Aktoersroller hovedMottaker, Behandling behandling) {
        var builder = new MottakerDto.Builder()
            .medType(BRUKER_ELLER_BRUKERS_FULLMEKTIG)
            .medRolle(hovedMottaker);

        leggTilAdresseOgFeilmelding(builder, produserbartdokument, hovedMottaker, behandling);

        return new BrevmalDto.Builder()
            .medType(produserbartdokument)
            .medMuligeMottakere(singletonList(builder.build()))
            .build();
    }

    private BrevmalDto lagBrevMalDtoForMangelbrev(Produserbaredokumenter produserbartdokument, Aktoersroller hovedMottaker, Behandling behandling) {
        List<MottakerDto> mottakere = new ArrayList<>();
        List<FeltvalgDto> feltvalgDtos = new ArrayList<>();

        var builder = new MottakerDto.Builder()
            .medType(hovedMottaker == Aktoersroller.BRUKER ? BRUKER_ELLER_BRUKERS_FULLMEKTIG : "Arbeidsgiver eller arbeidsgivers fullmektig")
            .medRolle(hovedMottaker);

        leggTilAdresseOgFeilmelding(builder, produserbartdokument, hovedMottaker, behandling);

        mottakere.add(builder.build());
        if (hovedMottaker == Aktoersroller.ARBEIDSGIVER) {
            mottakere.add(
                new MottakerDto.Builder()
                    .medType("Annen organisasjon")
                    .medRolle(hovedMottaker)
                    .orgnrSettesAvSaksbehandler()
                    .build()
            );
        }

        if (behandling.getType() == Behandlingstyper.SOEKNAD || behandling.erKlage()) {
            feltvalgDtos.add(new FeltvalgDto.Builder().medKode("STANDARD").medBeskrivelse("Standardtekst søknad/klage").build());
        }
        feltvalgDtos.add(new FeltvalgDto.Builder().medKode("FRITEKST").medBeskrivelse("Fritekst (erstatter standardtekst)").build());

        return new BrevmalDto.Builder()
            .medType(produserbartdokument)
            .medFelter(asList(
                new BrevmalFeltDto.Builder()
                    .medKode("INNLEDNING_FRITEKST")
                    .medBeskrivelse("Innledningstekst")
                    .medFeltType(FeltType.FRITEKST)
                    .erPåkrevd()
                    .medValg(feltvalgDtos)
                    .build(),
                new BrevmalFeltDto.Builder()
                    .medKode("MANGLER_FRITEKST")
                    .medBeskrivelse("Hva skal mottakeren sende inn?")
                    .medFeltType(FeltType.FRITEKST)
                    .erPåkrevd()
                    .build()
            ))
            .medMuligeMottakere(mottakere)
            .medMottakereHjelpetekst("Hvis bruker eller arbeidsgiver har fullmektig som er lagt inn i sidemenyen, vil brevet automatisk bli sendt til denne.")
            .build();
    }

    private void leggTilAdresseOgFeilmelding(MottakerDto.Builder builder, Produserbaredokumenter produserbaredokumenter, Aktoersroller aktoersroller, Behandling behandling) {
        try {
            var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(produserbaredokumenter, aktoersroller, behandling);
            if (aktoersroller == Aktoersroller.BRUKER && brevAdresser.stream().allMatch(BrevAdresse::isAdresselinjerEmpty)) {
                builder.medFeilmelding("Bruker har ingen registrert adresse.");
            } else {
                builder.medAdresse(brevAdresser.stream().map(MottakerAdresseDto::av).collect(Collectors.toList()));
            }
        } catch (TekniskException e) {
            if ("Finner ikke arbeidsforholddokument".equals(e.getMessage())) {
                builder.medFeilmelding("Finner ingen arbeidsgivere. Hent registeropplysninger.");
            } else {
                throw new TekniskException(e);
            }
        }
    }

    private HttpHeaders genPdfHeaders(String navn, boolean download) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String filename = navn + ".pdf";
        ContentDisposition.Builder contentDisposition = ContentDisposition.builder(download ? "attachment" : "inline");
        contentDisposition.filename(filename);
        headers.setContentDisposition(contentDisposition.build());
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return headers;
    }
}
