package no.nav.melosys.tjenester.gui.brev;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.brev.Etat;
import no.nav.melosys.service.brev.BrevbestillingFasade;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.BrevmalListeBygger;
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
public class BrevbestillingTjeneste {

    private final BrevmalListeBygger brevmalListeBygger;
    private final Aksesskontroll aksesskontroll;
    private final BrevbestillingFasade brevbestillingFasade;

    public BrevbestillingTjeneste(BrevbestillingFasade brevbestillingFasade,
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
        var hentMuligeMottakereResponseDto = brevbestillingFasade.hentMuligeMottakere(hentMuligeMottakereRequestDto);
        return HentMuligeBrevmottakereResponse.av(hentMuligeMottakereResponseDto);
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

    @PostMapping(value = "/mulige-mottakere-etater/{behandlingID}", produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Henter alle mulige mottakere for valgte etater")
    public List<MuligBrevmottaker> hentMuligeBrevmottakereEtater(@PathVariable long behandlingID,
                                                                 @RequestBody HentMuligeBrevmottakereEtaterRequest hentMuligeBrevmottakereEtaterRequest) {
        aksesskontroll.autoriser(behandlingID);

        var muligeBrevmottakere = brevbestillingFasade.hentMuligeBrevmottakereEtater(hentMuligeBrevmottakereEtaterRequest.orgnrEtater());
        return muligeBrevmottakere.stream().map(MuligBrevmottaker::av).toList();
    }

    @GetMapping(value = "/tilgjengelige-etater", produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Henter alle tilgjengelige etater", response = Etat.class, responseContainer = "List")
    public List<Etat> hentTilgjengeligeEtater() {
        return brevbestillingFasade.hentTilgjengeligeEtater();
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
