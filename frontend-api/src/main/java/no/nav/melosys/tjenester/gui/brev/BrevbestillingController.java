package no.nav.melosys.tjenester.gui.brev;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.brev.NorskMyndighet;
import no.nav.melosys.service.brev.BrevbestillingFasade;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.brev.*;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.RequestScope;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

@Protected
@RestController
@RequestMapping("/dokumenter/v2")
@Api(tags = {"dokumenterv2"})
@RequestScope
public class BrevbestillingController {

    private final BrevmalListeBygger brevmalListeBygger;
    private final Aksesskontroll aksesskontroll;
    private final BrevbestillingFasade brevbestillingFasade;

    public BrevbestillingController(BrevbestillingFasade brevbestillingFasade,
                                  BrevmalListeBygger brevmalListeBygger,
                                  Aksesskontroll aksesskontroll) {
        this.brevbestillingFasade = brevbestillingFasade;
        this.brevmalListeBygger = brevmalListeBygger;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping(value = "/tilgjengelige-maler/{behandlingID}", produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Henter alle tilgjengelige brevmaler for en behandling", response = BrevmalResponse.class, responseContainer = "List")
    public List<BrevmalResponse> hentTilgjengeligeMaler(@PathVariable long behandlingID) {
        aksesskontroll.autoriser(behandlingID);
        return brevmalListeBygger.byggBrevmalDtoListe(behandlingID);
    }

    @PostMapping(value = "/mulige-mottakere/{behandlingID}", produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Henter alle mulige mottakere for valgt dokumenttype, og organisasjonsnummer dersom hovedmottaker ikke er bruker")
    public HentMuligeBrevmottakereResponse hentMuligeBrevmottakere(@PathVariable long behandlingID,
                                                                   @RequestBody HentMuligeBrevmottakereRequest hentMuligeBrevmottakereRequest) {
        aksesskontroll.autoriser(behandlingID);

        var hentMuligeMottakereRequestDto = hentMuligeBrevmottakereRequest.tilHentMuligeBrevmottakereRequestDto(behandlingID);
        return HentMuligeBrevmottakereResponse.av(brevbestillingFasade.hentMuligeMottakere(hentMuligeMottakereRequestDto));
    }

    @PostMapping(value = "pdf/brev/utkast/{behandlingID}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_PDF_VALUE)
    @ApiOperation(value = "Produser utkast")
    public ResponseEntity<byte[]> produserUtkast(@PathVariable long behandlingID,
                                                 @RequestBody BrevbestillingRequest brevbestillingRequest) {
        aksesskontroll.autoriser(behandlingID);

        BrevbestillingDto brevbestillingDto = brevbestillingRequest.tilBrevbestillingDto();
        byte[] pdfInBytes = brevbestillingFasade.produserUtkast(behandlingID, brevbestillingDto);
        return new ResponseEntity<>(pdfInBytes, genPdfHeaders("utkast_" + behandlingID), HttpStatus.OK);
    }

    @PostMapping("opprett/{behandlingID}")
    @ApiOperation(value = "Produser brev gjennom melosys-dokgen")
    public void produserBrev(@PathVariable("behandlingID") long behandlingID,
                             @RequestBody BrevbestillingRequest brevbestillingRequest) {
        aksesskontroll.autoriser(behandlingID);

        BrevbestillingDto brevbestillingDto = brevbestillingRequest.tilBrevbestillingDto();
        brevbestillingFasade.produserBrev(behandlingID, brevbestillingDto);
    }

    @PostMapping(value = "/mulige-mottakere-norske-myndigheter/{behandlingID}", produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Henter alle mulige mottakere for valgte norske myndigheter")
    public List<MuligBrevmottaker> hentMuligeBrevmottakereNorskMyndighet(@PathVariable long behandlingID,
                                                                         @RequestBody HentMuligeBrevmottakereNorskMyndighetRequest hentMuligeBrevmottakereNorskMyndighetRequest) {
        aksesskontroll.autoriser(behandlingID);
        var muligeBrevmottakere = brevbestillingFasade.hentMuligeBrevmottakereNorskMyndighet(hentMuligeBrevmottakereNorskMyndighetRequest.orgnrNorskMyndighet());
        return muligeBrevmottakere.stream().map(MuligBrevmottaker::av).toList();
    }

    @GetMapping(value = "/tilgjengelige-norske-myndigheter", produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Henter alle tilgjengelige norske myndigheter", response = NorskMyndighet.class, responseContainer = "List")
    public List<NorskMyndighet> hentTilgjengeligeNorskeMyndigheter() {
        return brevbestillingFasade.hentTilgjengeligeNorskeMyndigheter();
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
