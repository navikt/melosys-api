package no.nav.melosys.tjenester.gui.behandlinger;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.service.dokument.brev.SedPdfData;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@Api(tags = {"behandlinger", "sed", "utkast"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class SedUtkastController {
    private final EessiService eessiService;
    private final Aksesskontroll aksesskontroll;

    public SedUtkastController(
                            EessiService eessiService,
                            Aksesskontroll aksesskontroll) {
        this.eessiService = eessiService;
        this.aksesskontroll = aksesskontroll;
    }

    @PostMapping(value = "/behandlinger/{behandlingID}/sed/{sedType}/utkast",
        produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<byte[]> produserUtkastSed(@PathVariable("behandlingID") long behandlingID,
                                                    @PathVariable("sedType") SedType sedType,
                                                    @RequestBody SedPdfData sedPdfData) {
        aksesskontroll.autoriser(behandlingID);
        byte[] dokument = eessiService.genererSedPdf(behandlingID, sedType, sedPdfData);
        return lagResponseAvDokument(dokument, sedType.name() + "_utkast.pdf");
    }

    private static ResponseEntity<byte[]> lagResponseAvDokument(byte[] dokument, String filnavn) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_LENGTH, Integer.toString(dokument.length))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; attachment; filename=" + filnavn)
            .body(dokument);
    }
}
