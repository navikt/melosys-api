package no.nav.melosys.tjenester.gui;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.*;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.dokument.DokumentService;
import no.nav.melosys.service.dokument.DokumentVisningService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.SedPdfData;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.tjenester.gui.dto.dokument.JournalpostInfoDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/dokumenter")
@Api(tags = {"dokumenter"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class DokumentTjeneste {
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String APPLICATION_JSON_UTF8 = MediaType.APPLICATION_JSON_VALUE + "; charset=UTF-8";

    private final DokumentService dokumentService;
    private final DokumentVisningService dokumentVisningService;
    private final EessiService eessiService;
    private final TilgangService tilgangService;

    @Autowired
    public DokumentTjeneste(DokumentService dokumentService,
                            DokumentVisningService dokumentVisningService,
                            EessiService eessiService,
                            TilgangService tilgangService) {
        this.dokumentService = dokumentService;
        this.dokumentVisningService = dokumentVisningService;
        this.eessiService = eessiService;
        this.tilgangService = tilgangService;
    }

    @GetMapping(value = "/pdf/{journalpostID}/{dokumentID}", produces = {APPLICATION_PDF, APPLICATION_JSON_UTF8})
    @ApiOperation(value = "hent dokument knyttet til journalpost", response = byte[].class)
    public ResponseEntity<byte[]> hentDokument(@PathVariable("journalpostID") String journalpostID,
                                               @PathVariable("dokumentID") String dokumentID)
        throws SikkerhetsbegrensningException, IkkeFunnetException {
        byte[] dokument;
        dokument = dokumentVisningService.hentDokument(journalpostID, dokumentID);
        return lagResponseAvDokument(dokument, String.format("journalpost-dok-%s.pdf", dokumentID));
    }

    @GetMapping("/oversikt/{saksnummer}")
    @ApiOperation(value = "Henter alle dokumenter knyttet til en fagsak", response = JournalpostInfoDto.class, responseContainer = "List")
    public ResponseEntity hentDokumenter(@PathVariable("saksnummer") String saksnummer) throws IkkeFunnetException, IntegrasjonException, SikkerhetsbegrensningException {
        List<JournalpostInfoDto> dokumentListe = dokumentVisningService.hentDokumenter(saksnummer)
            .stream()
            .map(JournalpostInfoDto::av)
            .sorted(Comparator.comparing(JournalpostInfoDto::hentGjeldendeTidspunkt, Comparator.nullsFirst(Comparator.reverseOrder())))
            .collect(Collectors.toList());
        return ResponseEntity.ok(dokumentListe);
    }

    @PostMapping(value = "pdf/brev/utkast/{behandlingID}/{produserbartDokument}", produces = {APPLICATION_PDF, APPLICATION_JSON_UTF8})
    public ResponseEntity produserUtkastBrev(@PathVariable("behandlingID") long behandlingID,
                                             @PathVariable("produserbartDokument") Produserbaredokumenter produserbartDokument,
                                             @RequestBody BrevbestillingDto brevBestillingDto) throws TekniskException, FunksjonellException {
        byte[] dokument;
        tilgangService.sjekkTilgang(behandlingID);
        dokument = dokumentService.produserUtkast(behandlingID, produserbartDokument, brevBestillingDto);
        return lagResponseAvDokument(dokument, produserbartDokument.getKode() + "_utkast.pdf");
    }

    @PostMapping(value = "pdf/sed/utkast/{behandlingID}/{sedType}", produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity produserUtkastSed(@PathVariable("behandlingID") long behandlingID,
                                            @PathVariable("sedType") SedType sedType,
                                            @RequestBody SedPdfData sedPdfData) throws MelosysException {

        tilgangService.sjekkTilgang(behandlingID);
        byte[] dokument = eessiService.genererSedPdf(behandlingID, sedType, sedPdfData);
        return lagResponseAvDokument(dokument, sedType.name() + "_utkast.pdf");
    }

    @PostMapping("opprett/{behandlingID}/{produserbartDokument}")
    public ResponseEntity produserDokument(@PathVariable("behandlingID") long behandlingID,
                                           @PathVariable("produserbartDokument") Produserbaredokumenter produserbartDokument,
                                           @RequestBody BrevbestillingDto brevBestillingDto) throws FunksjonellException, TekniskException {
        if (brevBestillingDto.mottaker == null) {
            throw new FunksjonellException("Mottaker trengs for å bestille.");
        }
        tilgangService.sjekkTilgang(behandlingID);
        // Produserer utkast for å få eventuelle feil før bestilling i saksflyt.
        dokumentService.produserUtkast(behandlingID, produserbartDokument, brevBestillingDto);
        dokumentService.produserDokumentISaksflyt(produserbartDokument, brevBestillingDto.mottaker, behandlingID, new BrevData(brevBestillingDto));
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity lagResponseAvDokument(byte[] dokument, String filnavn) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_LENGTH, Integer.toString(dokument.length))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; attachment; filename=" + filnavn)
            .body(dokument);
    }
}
