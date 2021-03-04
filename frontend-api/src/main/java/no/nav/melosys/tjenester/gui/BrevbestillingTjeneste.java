package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.brev.BrevbestillingService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.tjenester.gui.dto.brev.BrevmalDto;
import no.nav.melosys.tjenester.gui.dto.brev.BrevmalFeltDto;
import no.nav.melosys.tjenester.gui.dto.brev.FeltType;
import no.nav.melosys.tjenester.gui.dto.brev.MottakerDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.RequestScope;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_BRUKER;
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

    @Autowired
    public BrevbestillingTjeneste(BrevbestillingService brevbestillingService) {
        this.brevbestillingService = brevbestillingService;
    }

    @GetMapping(value = "/tilgjengelige-maler", produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Henter alle tilgjengelige brevmaler for en behandling", response = BrevmalDto.class, responseContainer = "List")
    public List<BrevmalDto> hentTilgjengeligeMaler(@RequestParam Long behandlingId) throws IkkeFunnetException {
        return byggBrevmalListe(behandlingId);
    }

    @PostMapping(value = "pdf/brev/utkast/{behandlingID}/{produserbartDokument}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_PDF_VALUE)
    @ApiOperation(value = "Produser utkast")
    public ResponseEntity<byte[]> produserUtkast(@PathVariable long behandlingID,
                                                 @PathVariable Produserbaredokumenter produserbartDokument,
                                                 @RequestBody BrevbestillingDto brevbestillingDto)
        throws FunksjonellException, TekniskException {

        byte[] pdf = brevbestillingService.produserUtkast(produserbartDokument, behandlingID, brevbestillingDto);
        return new ResponseEntity<>(pdf, genPdfHeaders("utkast_" + behandlingID, false), HttpStatus.OK);
    }

    @PostMapping("opprett/{behandlingID}/{produserbartDokument}")
    @ApiOperation(value = "Produser brev gjennom melosys-dokgen")
    public void produserBrev(@PathVariable("behandlingID") long behandlingID,
                             @PathVariable("produserbartDokument") Produserbaredokumenter produserbartDokument,
                             @RequestBody BrevbestillingDto brevbestillingDto) throws FunksjonellException, TekniskException {
        brevbestillingService.produserBrev(produserbartDokument, behandlingID, brevbestillingDto);
    }

    private List<BrevmalDto> byggBrevmalListe(long behandlingId) throws IkkeFunnetException {
        List<Produserbaredokumenter> produserbareDokumenter = brevbestillingService.hentBrevMaler(behandlingId);

        List<BrevmalDto> maler = new ArrayList<>();
        produserbareDokumenter.forEach(p -> {
            switch (p) {
                case MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD:
                    maler.add(lagBrevmalDto(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, null,
                        singletonList(new MottakerDto.Builder()
                            .medType("Bruker eller brukers fullmektig")
                            .medRolle(Aktoersroller.BRUKER)
                            .build())
                    ));
                    break;
                case MANGELBREV_BRUKER:
                    List<MottakerDto> mottakere = new ArrayList<>();
                    mottakere.add(
                        new MottakerDto.Builder()
                            .medType("Bruker eller brukers fullmektig")
                            .medRolle(Aktoersroller.BRUKER)
                            .build()
                    );
                    mottakere.add(
                        new MottakerDto.Builder()
                            .medType("Arbeidsgiver eller arbeidsgivers fullmektig")
                            .medRolle(Aktoersroller.ARBEIDSGIVER)
                            .build()
                    );

                    maler.add(lagBrevmalDto(MANGELBREV_BRUKER,
                        asList(
                            new BrevmalFeltDto.Builder()
                                .medKode("INNLEDNING_FRITEKST")
                                .medBeskrivelse("Fritekst til innledning")
                                .medFeltType(FeltType.FRITEKST)
                                .medHjelpetekst("")
                                .build(),
                            new BrevmalFeltDto.Builder()
                                .medKode("MANGLER_FRITEKST")
                                .medBeskrivelse("Fritekst om manglende dokumentasjon")
                                .medFeltType(FeltType.FRITEKST)
                                .medHjelpetekst("")
                                .erPåkrevd()
                                .build()
                        ),
                        mottakere
                    ));
                    break;
                default:
                    break;
            }
        });

        return maler;
    }

    private BrevmalDto lagBrevmalDto(Produserbaredokumenter dokument, List<BrevmalFeltDto> felter, List<MottakerDto> mottakere) {
        return new BrevmalDto.Builder()
            .medType(dokument)
            .medBeskrivelse(dokument.getBeskrivelse())
            .medFelter(felter)
            .medMuligeMottakere(mottakere)
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
