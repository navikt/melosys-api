package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.brev.BrevbestillingService;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.tjenester.gui.dto.brev.BrevmalDto;
import no.nav.melosys.tjenester.gui.dto.brev.BrevmalFeltDto;
import no.nav.melosys.tjenester.gui.dto.brev.FeltType;
import no.nav.melosys.tjenester.gui.dto.brev.FeltvalgDto;
import no.nav.melosys.tjenester.gui.dto.brev.MottakerAdresseDto;
import no.nav.melosys.tjenester.gui.dto.brev.MottakerDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.RequestScope;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

@Protected
@RestController
@RequestMapping("/dokumenter/v2") //TODO Endre url når gjennomtestet
@Api(tags = {"dokumenterv2"})
@RequestScope
public class BrevbestillingTjeneste {
    private final BrevbestillingService brevbestillingService;
    private final BehandlingService behandlingService;
    private final BrevmottakerService brevmottakerService;

    @Autowired
    public BrevbestillingTjeneste(BrevbestillingService brevbestillingService, BehandlingService behandlingService, BrevmottakerService brevmottakerService) {
        this.brevbestillingService = brevbestillingService;
        this.behandlingService = behandlingService;
        this.brevmottakerService = brevmottakerService;
    }

    @GetMapping(value = "/tilgjengelige-maler/{behandlingID}", produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Henter alle tilgjengelige brevmaler for en behandling", response = BrevmalDto.class, responseContainer = "List")
    public List<BrevmalDto> hentTilgjengeligeMaler(@RequestParam Long behandlingId) throws FunksjonellException, TekniskException {
        return byggBrevmalListe(behandlingId);
    }

    @PostMapping(value = "pdf/brev/utkast/{behandlingID}/{produserbartDokument}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_PDF_VALUE)
    @ApiOperation(value = "Produser utkast")
    public ResponseEntity<byte[]> produserUtkast(@PathVariable long behandlingID,
                                                 @RequestBody BrevbestillingDto brevbestillingDto)
        throws FunksjonellException, TekniskException {

        byte[] pdf = brevbestillingService.produserUtkast(behandlingID, brevbestillingDto);
        return new ResponseEntity<>(pdf, genPdfHeaders("utkast_" + behandlingID, false), HttpStatus.OK);
    }

    @PostMapping("opprett/{behandlingID}/{produserbartDokument}")
    @ApiOperation(value = "Produser brev gjennom melosys-dokgen")
    public void produserBrev(@PathVariable("behandlingID") long behandlingID,
                             @RequestBody BrevbestillingDto brevbestillingDto) throws FunksjonellException, TekniskException {
        brevbestillingService.produserBrev(behandlingID, brevbestillingDto);
    }

    private List<BrevmalDto> byggBrevmalListe(long behandlingId) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        List<Produserbaredokumenter> produserbareDokumenter = brevbestillingService.hentBrevMaler(behandling);

        List<BrevmalDto> maler = new ArrayList<>();
        for(Produserbaredokumenter p : produserbareDokumenter) {
            Aktoersroller hovedMottaker = brevmottakerService.hentMottakerliste(p, behandling).getHovedMottaker();
            MottakerDto.Builder mottakerDto;
            List<FeltvalgDto> feltvalgDtos = new ArrayList<>();
            switch (p) {
                case MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD:
                    mottakerDto = new MottakerDto.Builder()
                        .medType("Bruker eller brukers fullmektig")
                        .medRolle(hovedMottaker);

                    leggTilAdresseOgFeilmelding(mottakerDto, MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, hovedMottaker, behandling);

                    maler.add(lagBrevmalDto(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, null,
                        singletonList(mottakerDto.build()),
                        null
                    ));
                    break;
                case MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE:
                    mottakerDto = new MottakerDto.Builder()
                        .medType("Bruker eller brukers fullmektig")
                        .medRolle(hovedMottaker);

                    leggTilAdresseOgFeilmelding(mottakerDto, MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE, hovedMottaker, behandling);

                    maler.add(lagBrevmalDto(MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE, null,
                        singletonList(mottakerDto.build()),
                        null
                    ));
                    break;
                case MANGELBREV_BRUKER:
                    mottakerDto = new MottakerDto.Builder()
                        .medType("Bruker eller brukers fullmektig")
                        .medRolle(hovedMottaker);

                    leggTilAdresseOgFeilmelding(mottakerDto, MANGELBREV_BRUKER, hovedMottaker, behandling);

                    feltvalgDtos.add(new FeltvalgDto.Builder().medKode("FRITEKST").medBeskrivelse("Fritekst (erstatter standardtekst)").build());
                    if (behandling.getType() == Behandlingstyper.SOEKNAD || behandling.getType() == Behandlingstyper.KLAGE) {
                        feltvalgDtos.add(new FeltvalgDto.Builder().medKode("STANDARD").medBeskrivelse("Standardtekst søknad/klage").build());
                    }

                    maler.add(lagBrevmalDto(MANGELBREV_BRUKER,
                        asList(
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
                        ),
                        singletonList(mottakerDto.build()),
                        "Hvis bruker eller arbeidsgiver har fullmektig som er lagt inn i sidemenyen, vil brevet automatisk bli sendt til denne."
                    ));
                    break;
                case MANGELBREV_ARBEIDSGIVER:
                    List<MottakerDto> mottakere = new ArrayList<>();
                    mottakerDto = new MottakerDto.Builder()
                        .medType("Arbeidsgiver eller arbeidsgivers fullmektig")
                        .medRolle(hovedMottaker);

                    leggTilAdresseOgFeilmelding(mottakerDto, MANGELBREV_ARBEIDSGIVER, hovedMottaker, behandling);

                    mottakere.add(mottakerDto.build());
                    mottakere.add(
                        new MottakerDto.Builder()
                            .medType("Annen organisasjon")
                            .medRolle(hovedMottaker)
                            .egendefinert()
                            .build()
                    );

                    feltvalgDtos.add(new FeltvalgDto.Builder().medKode("FRITEKST").medBeskrivelse("Fritekst (erstatter standardtekst)").build());
                    if (behandling.getType() == Behandlingstyper.SOEKNAD || behandling.getType() == Behandlingstyper.KLAGE) {
                        feltvalgDtos.add(new FeltvalgDto.Builder().medKode("STANDARD").medBeskrivelse("Standardtekst søknad/klage").build());
                    }

                    maler.add(lagBrevmalDto(MANGELBREV_ARBEIDSGIVER,
                        asList(
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
                        ),
                        mottakere,
                        "Hvis bruker eller arbeidsgiver har fullmektig som er lagt inn i sidemenyen, vil brevet automatisk bli sendt til denne."
                    ));
                    break;
                default:
                    break;
            }
        }
        return maler;
    }


    private MottakerDto.Builder leggTilAdresseOgFeilmelding(MottakerDto.Builder builder, Produserbaredokumenter produserbaredokumenter, Aktoersroller aktoersroller, Behandling behandling) throws TekniskException, FunksjonellException {
        try {
            var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(produserbaredokumenter, aktoersroller, behandling);
            if (aktoersroller == Aktoersroller.BRUKER && (brevAdresser.isEmpty() || brevAdresser.get(0).adresselinjer.stream().noneMatch(Objects::nonNull))) {
                return builder.medFeilmelding("Bruker har ingen registrert adresse.");
            }
            return builder.medAdresse(brevAdresser.stream().map(MottakerAdresseDto::av).collect(Collectors.toList()));
        } catch (TekniskException e) {
            if ("Finner ikke arbeidsforholddokument".equals(e.getMessage())) {
                return builder.medFeilmelding("Finner ingen arbeidsgivere. Hent registeropplysninger.");
            }
            throw new TekniskException(e);
        }
    }

    private BrevmalDto lagBrevmalDto(Produserbaredokumenter dokument, List<BrevmalFeltDto> felter, List<MottakerDto> mottakere, String mottakerHjelpetekst) {
        return new BrevmalDto.Builder()
            .medType(dokument)
            .medFelter(felter)
            .medMuligeMottakere(mottakere)
            .medMottakereHjelpetekst(mottakerHjelpetekst)
            .build();
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
