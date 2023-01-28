package no.nav.melosys.tjenester.gui.brev;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.finn.unleash.Unleash;
import no.nav.melosys.domain.brev.Etat;
import no.nav.melosys.featuretoggle.ToggleName;
import no.nav.melosys.service.brev.BrevbestillingFasade;
import no.nav.melosys.service.brev.BrevbestillingService;
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

    private final BrevbestillingService brevbestillingService;
    private final BrevmalListeBygger brevmalListeBygger;
    private final Aksesskontroll aksesskontroll;
    private final BrevbestillingFasade brevbestillingFasade;
    private final Unleash unleash;

    public BrevbestillingTjeneste(BrevbestillingFasade brevbestillingFasade,
                                  BrevbestillingService brevbestillingService,
                                  BrevmalListeBygger brevmalListeBygger,
                                  Aksesskontroll aksesskontroll, Unleash unleash) {
        this.brevbestillingFasade = brevbestillingFasade;
        this.brevbestillingService = brevbestillingService;
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
            var gammelMuligeBrevmottakereDto = brevbestillingService.hentMuligeMottakere(hentMuligeBrevmottakereRequest.produserbartdokument(), behandlingID, hentMuligeBrevmottakereRequest.orgnr());
            var hovedMottaker = MuligBrevmottaker.byggFraBrevmottakerDto(gammelMuligeBrevmottakereDto.getHovedMottaker());
            var kopiMottakere = gammelMuligeBrevmottakereDto.getKopiMottakere().stream().map(MuligBrevmottaker::byggFraBrevmottakerDto).toList();
            var fasteMottakere = gammelMuligeBrevmottakereDto.getFasteMottakere().stream().map(MuligBrevmottaker::byggFraBrevmottakerDto).toList();
            return new HentMuligeBrevmottakereResponse(hovedMottaker, kopiMottakere, fasteMottakere);
        }

        var hentMottakerRequest = hentMuligeBrevmottakereRequest.tilHentMottakereRequest(behandlingID);
        var hentMottakerResponse = brevbestillingFasade.hentMuligeMottakere(hentMottakerRequest);
        return HentMuligeBrevmottakereResponse.byggFraHentMottakerResponse(hentMottakerResponse);
    }

    @PostMapping(value = "pdf/brev/utkast/{behandlingID}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_PDF_VALUE)
    @ApiOperation(value = "Produser utkast")
    public ResponseEntity<byte[]> produserUtkast(@PathVariable long behandlingID,
                                                 @RequestBody BrevbestillingRequest brevbestillingRequest) {
        aksesskontroll.autoriser(behandlingID);

        if (!unleash.isEnabled(ToggleName.MELOSYS_MEL_4835)) {
            BrevbestillingDto brevbestillingDto = brevbestillingRequest.tilBrevbestillingDtoBuilder()
                .medBestillersId(SubjectHandler.getInstance().getUserID())
                .build();
            byte[] pdf = brevbestillingService.produserUtkast(behandlingID, brevbestillingDto);
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
            BrevbestillingDto brevbestillingDto = brevbestillingRequest.tilBrevbestillingDtoBuilder()
                .medBestillersId(SubjectHandler.getInstance().getUserID())
                .build();
            brevbestillingService.produserBrev(behandlingID, brevbestillingDto);
            return;
        }

        BrevbestillingDto brevbestillingDto = brevbestillingRequest.tilBrevbestillingDto();
        brevbestillingFasade.produserBrev(behandlingID, brevbestillingDto);
    }

    @PostMapping(value = "/mulige-mottakere-etater/{behandlingID}", produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Henter alle mulige mottakere for valgte etater")
    public List<MuligBrevmottaker> hentTilgjengeligeMottakereEtater(@PathVariable long behandlingID,
                                                                    @RequestBody HentMuligeBrevmottakereEtaterRequest hentMuligeBrevmottakereEtaterRequest) {
        aksesskontroll.autoriser(behandlingID);
        var muligeBrevmottakere = brevbestillingFasade.hentMuligeMottakereEtater(hentMuligeBrevmottakereEtaterRequest.orgnrEtater());
        return muligeBrevmottakere.stream().map(MuligBrevmottaker::byggFraBrevmottakerDto).toList();
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
