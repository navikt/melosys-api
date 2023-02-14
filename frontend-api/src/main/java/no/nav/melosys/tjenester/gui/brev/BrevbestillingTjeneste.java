package no.nav.melosys.tjenester.gui.brev;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.finn.unleash.Unleash;
import no.nav.melosys.domain.brev.NorskMyndighet;
import no.nav.melosys.featuretoggle.ToggleName;
import no.nav.melosys.service.brev.BrevbestillingFasade;
import no.nav.melosys.service.brev.BrevbestillingServiceOld;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
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

    private final BrevbestillingServiceOld brevbestillingServiceOld;
    private final BrevmalListeBygger brevmalListeBygger;
    private final Aksesskontroll aksesskontroll;
    private final BrevbestillingFasade brevbestillingFasade;
    private final Unleash unleash;

    public BrevbestillingTjeneste(BrevbestillingFasade brevbestillingFasade,
                                  BrevbestillingServiceOld brevbestillingServiceOld,
                                  BrevmalListeBygger brevmalListeBygger,
                                  Aksesskontroll aksesskontroll, Unleash unleash) {
        this.brevbestillingFasade = brevbestillingFasade;
        this.brevbestillingServiceOld = brevbestillingServiceOld;
        this.brevmalListeBygger = brevmalListeBygger;
        this.aksesskontroll = aksesskontroll;
        this.unleash = unleash;
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

        if (!unleash.isEnabled(ToggleName.MELOSYS_MEL_4835)) {
            var gammelMuligeBrevmottakereDto = brevbestillingServiceOld.hentMuligeMottakere(hentMuligeBrevmottakereRequest.produserbartdokument(), behandlingID, hentMuligeBrevmottakereRequest.orgnr());
            var hovedMottaker = MuligBrevmottaker.av(gammelMuligeBrevmottakereDto.getHovedMottaker());
            var kopiMottakere = gammelMuligeBrevmottakereDto.getKopiMottakere().stream().map(MuligBrevmottaker::av).toList();
            var fasteMottakere = gammelMuligeBrevmottakereDto.getFasteMottakere().stream().map(MuligBrevmottaker::av).toList();
            return new HentMuligeBrevmottakereResponse(hovedMottaker, kopiMottakere, fasteMottakere);
        }

        var hentMuligeMottakereRequestDto = hentMuligeBrevmottakereRequest.tilHentMuligeBrevmottakereRequestDto(behandlingID);
        var hentMuligeMottakereResponseDto = brevbestillingFasade.hentMuligeMottakere(hentMuligeMottakereRequestDto);
        return HentMuligeBrevmottakereResponse.av(hentMuligeMottakereResponseDto);
    }

    @PostMapping(value = "pdf/brev/utkast/{behandlingID}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_PDF_VALUE)
    @ApiOperation(value = "Produser utkast")
    public ResponseEntity<byte[]> produserUtkast(@PathVariable long behandlingID,
                                                 @RequestBody BrevbestillingRequest brevbestillingRequest) {
        aksesskontroll.autoriser(behandlingID);

        if (!unleash.isEnabled(ToggleName.MELOSYS_MEL_4835)) {
            BrevbestillingDto brevbestillingDto = brevbestillingRequest.tilBrevbestillingDto(SubjectHandler.getInstance().getUserID());
            byte[] pdf = brevbestillingServiceOld.produserUtkast(behandlingID, brevbestillingDto);
            return new ResponseEntity<>(pdf, genPdfHeaders("utkast_" + behandlingID), HttpStatus.OK);
        }

        BrevbestillingDto brevbestillingDto = brevbestillingRequest.tilBrevbestillingDto();
        byte[] pdfInBytes = brevbestillingFasade.produserUtkast(behandlingID, brevbestillingDto);
        return new ResponseEntity<>(pdfInBytes, genPdfHeaders("utkast_" + behandlingID), HttpStatus.OK);
    }

    @PostMapping("opprett/{behandlingID}")
    @ApiOperation(value = "Produser brev gjennom melosys-dokgen")
    public void produserBrev(@PathVariable("behandlingID") long behandlingID,
                             @RequestBody BrevbestillingRequest brevbestillingRequest) {
        aksesskontroll.autoriser(behandlingID);

        if (!unleash.isEnabled(ToggleName.MELOSYS_MEL_4835)) {
            BrevbestillingDto brevbestillingDto = brevbestillingRequest.tilBrevbestillingDto(SubjectHandler.getInstance().getUserID());
            brevbestillingServiceOld.produserBrev(behandlingID, brevbestillingDto);
            return;
        }

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
