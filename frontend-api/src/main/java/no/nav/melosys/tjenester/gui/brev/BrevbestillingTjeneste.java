package no.nav.melosys.tjenester.gui.brev;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.finn.unleash.Unleash;
import no.nav.melosys.domain.brev.Etat;
import no.nav.melosys.featuretoggle.ToggleName;
import no.nav.melosys.service.brev.BrevbestillingFasade;
import no.nav.melosys.service.brev.BrevbestillingService;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.BrevmalListeBygger;
import no.nav.melosys.tjenester.gui.brev.dto.HentMuligeBrevmottakereResponseDto;
import no.nav.melosys.tjenester.gui.brev.dto.MuligBrevmottakerResponseDto;
import no.nav.melosys.tjenester.gui.dto.brev.BrevbestillingDto;
import no.nav.melosys.tjenester.gui.dto.brev.BrevmalDto;
import no.nav.melosys.tjenester.gui.dto.brev.HentMuligeBrevmottakereRequestDto;
import no.nav.melosys.tjenester.gui.dto.brev.HentMuligeMottakereEtaterRequestDto;
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
    @ApiOperation(value = "Henter alle tilgjengelige brevmaler for en behandling", response = BrevmalDto.class, responseContainer = "List")
    public List<BrevmalDto> hentTilgjengeligeMaler(@PathVariable long behandlingID) {
        aksesskontroll.autoriser(behandlingID);
        return brevmalListeBygger.byggBrevmalDtoListe(behandlingID);
    }

    @PostMapping(value = "/mulige-mottakere/{behandlingID}", produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Henter alle mulige mottakere for valgt dokumenttype, og organisasjonsnummer dersom hovedmottaker ikke er bruker")
    public HentMuligeBrevmottakereResponseDto hentMuligeBrevmottakere(@PathVariable long behandlingID,
                                                                      @RequestBody HentMuligeBrevmottakereRequestDto hentMuligeBrevmottakereRequestDto) {
        aksesskontroll.autoriser(behandlingID);

        if (!unleash.isEnabled(ToggleName.MELOSYS_MEL_4835)) {
            var gammelMuligeBrevmottakereDto = brevbestillingService.hentMuligeMottakere(hentMuligeBrevmottakereRequestDto.produserbartdokument(), behandlingID, hentMuligeBrevmottakereRequestDto.orgnr());
            var hovedMottaker = MuligBrevmottakerResponseDto.byggFraBrevmottakerDto(gammelMuligeBrevmottakereDto.getHovedMottaker());
            var kopiMottakere = gammelMuligeBrevmottakereDto.getKopiMottakere().stream().map(MuligBrevmottakerResponseDto::byggFraBrevmottakerDto).toList();
            var fasteMottakere = gammelMuligeBrevmottakereDto.getFasteMottakere().stream().map(MuligBrevmottakerResponseDto::byggFraBrevmottakerDto).toList();
            return new HentMuligeBrevmottakereResponseDto(hovedMottaker, kopiMottakere, fasteMottakere);
        }

        var hentMottakerRequest = hentMuligeBrevmottakereRequestDto.tilHentMottakereRequest(behandlingID);
        var hentMottakerResponse = brevbestillingFasade.hentMuligeMottakere(hentMottakerRequest);
        return HentMuligeBrevmottakereResponseDto.byggFraHentMottakerResponse(hentMottakerResponse);
    }

    @PostMapping(value = "pdf/brev/utkast/{behandlingID}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_PDF_VALUE)
    @ApiOperation(value = "Produser utkast")
    public ResponseEntity<byte[]> produserUtkast(@PathVariable long behandlingID,
                                                 @RequestBody BrevbestillingDto brevbestillingDto) {
        aksesskontroll.autoriser(behandlingID);

        if (!unleash.isEnabled(ToggleName.MELOSYS_MEL_4835)) {
            BrevbestillingRequest brevbestillingRequest = brevbestillingDto.tilRequestBuilder()
                .medBestillersId(SubjectHandler.getInstance().getUserID())
                .build();
            byte[] pdf = brevbestillingService.produserUtkast(behandlingID, brevbestillingRequest);
            return new ResponseEntity<>(pdf, genPdfHeaders("utkast_" + behandlingID), HttpStatus.OK);
        }

        BrevbestillingRequest brevbestillingRequest = brevbestillingDto.tilBrevbestillingRequest();
        byte[] pdfInBytes = brevbestillingFasade.produserUtkast(behandlingID, brevbestillingRequest);
        return new ResponseEntity<>(pdfInBytes, genPdfHeaders("utkast_" + behandlingID), HttpStatus.OK);
    }

    @PostMapping("opprett/{behandlingID}")
    @ApiOperation(value = "Produser brev gjennom melosys-dokgen")
    public void produserBrev(@PathVariable("behandlingID") long behandlingID,
                             @RequestBody BrevbestillingDto brevbestillingDto) {
        aksesskontroll.autoriser(behandlingID);
        BrevbestillingRequest brevbestillingRequest = brevbestillingDto.tilRequestBuilder()
            .medBestillersId(SubjectHandler.getInstance().getUserID())
            .build();

        if (!unleash.isEnabled(ToggleName.MELOSYS_MEL_4835)) {
            brevbestillingService.produserBrev(behandlingID, brevbestillingRequest);
            return;
        }
        brevbestillingFasade.produserBrev(behandlingID, brevbestillingRequest);
    }

    @PostMapping(value = "/mulige-mottakere-etater/{behandlingID}", produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Henter alle mulige mottakere for valgte etater")
    public List<MuligBrevmottakerResponseDto> hentTilgjengeligeMottakereEtater(@PathVariable long behandlingID,
                                                                               @RequestBody HentMuligeMottakereEtaterRequestDto hentMuligeMottakereRequestDto) {
        aksesskontroll.autoriser(behandlingID);
        var muligeBrevmottakere = brevbestillingService.hentMuligeMottakereEtater(
            hentMuligeMottakereRequestDto.produserbartdokument(),
            behandlingID,
            hentMuligeMottakereRequestDto.orgnrEtater());
        return muligeBrevmottakere.stream().map(MuligBrevmottakerResponseDto::byggFraBrevmottakerDto).toList();
    }

    @GetMapping(value = "/tilgjengelige-etater", produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Henter alle tilgjengelige etater", response = Etat.class, responseContainer = "List")
    public List<Etat> hentTilgjengeligeEtater() {
        return brevbestillingService.hentTilgjengeligeEtater();
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
