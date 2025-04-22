package no.nav.melosys.tjenester.gui;

import java.util.Comparator;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.melosys.domain.arkiv.BrukerIdType;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.service.dokument.DokumentHentingService;
import no.nav.melosys.service.dokument.brev.SedPdfData;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.JournalpostInfoDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;


@Protected
@RestController
@RequestMapping("/dokumenter")
@Tag(name = "dokumenter")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class DokumentController {
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String APPLICATION_JSON_UTF8 = MediaType.APPLICATION_JSON_VALUE + "; charset=UTF-8";

    private final DokumentHentingService dokumentHentingService;
    private final EessiService eessiService;
    private final Aksesskontroll aksesskontroll;

    public DokumentController(DokumentHentingService dokumentHentingService,
                            EessiService eessiService,
                            Aksesskontroll aksesskontroll) {
        this.dokumentHentingService = dokumentHentingService;
        this.eessiService = eessiService;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping(value = "/{journalpostID}/{dokumentID}", produces = {APPLICATION_PDF, APPLICATION_JSON_UTF8})
    @Operation(summary = "hent dokument knyttet til journalpost")
    public ResponseEntity<byte[]> hentDokument(@PathVariable("journalpostID") String journalpostID,
                                               @PathVariable("dokumentID") String dokumentID) {

        Journalpost journalpost = dokumentHentingService.hentJournalpost(journalpostID);
        String saksnummerMelding = (journalpost.getSaksnummer() != null) ? "på sak " + journalpost.getSaksnummer() : "";

        if (journalpost.getBrukerIdType() == BrukerIdType.AKTØR_ID) {
            aksesskontroll.auditAutoriserAktørID(journalpost.getBrukerId(), "Innsyn i dokument %s %s".formatted(journalpost.getHoveddokument().getTittel(), saksnummerMelding));
        }
        if (journalpost.getBrukerIdType() == BrukerIdType.FOLKEREGISTERIDENT) {
            aksesskontroll.auditAutoriserFolkeregisterIdent(journalpost.getBrukerId(), "Innsyn i dokument %s %s".formatted(journalpost.getHoveddokument().getTittel(), saksnummerMelding));
        }

        byte[] dokument = dokumentHentingService.hentDokument(journalpostID, dokumentID);
        return lagResponseAvDokument(dokument, String.format("journalpost-dok-%s.pdf", dokumentID));
    }

    @Deprecated(since = "MELOSYS-5899")
    @GetMapping(value = "/pdf/{journalpostID}/{dokumentID}", produces = {APPLICATION_PDF, APPLICATION_JSON_UTF8})
    @Operation(summary = "hent dokument knyttet til journalpost")
    public ResponseEntity<byte[]> hentDokumentDeprecated(@PathVariable("journalpostID") String journalpostID,
                                               @PathVariable("dokumentID") String dokumentID) {

        Journalpost journalpost = dokumentHentingService.hentJournalpost(journalpostID);
        String saksnummerMelding = (journalpost.getSaksnummer() != null) ? "på sak " + journalpost.getSaksnummer() : "";

        if (journalpost.getBrukerIdType() == BrukerIdType.AKTØR_ID) {
            aksesskontroll.auditAutoriserAktørID(journalpost.getBrukerId(), "Innsyn i dokument %s %s".formatted(journalpost.getHoveddokument().getTittel(), saksnummerMelding));
        }
        if (journalpost.getBrukerIdType() == BrukerIdType.FOLKEREGISTERIDENT) {
            aksesskontroll.auditAutoriserFolkeregisterIdent(journalpost.getBrukerId(), "Innsyn i dokument %s %s".formatted(journalpost.getHoveddokument().getTittel(), saksnummerMelding));
        }

        byte[] dokument = dokumentHentingService.hentDokument(journalpostID, dokumentID);
        return lagResponseAvDokument(dokument, String.format("journalpost-dok-%s.pdf", dokumentID));
    }

    @Deprecated(since = "MELOSYS-5899")
    @GetMapping("/oversikt/{saksnummer}")
    @Operation(summary = "Henter alle dokumenter knyttet til en fagsak")
    public ResponseEntity<List<JournalpostInfoDto>> hentDokumenter(@PathVariable("saksnummer") String saksnummer) {
        List<JournalpostInfoDto> dokumentListe = dokumentHentingService.hentJournalposter(saksnummer)
            .stream()
            .map(JournalpostInfoDto::av)
            .sorted(Comparator.comparing(JournalpostInfoDto::hentGjeldendeTidspunkt, Comparator.nullsFirst(Comparator.reverseOrder())))
            .toList();
        return ResponseEntity.ok(dokumentListe);
    }

    @Deprecated(since = "MELOSYS-5899")
    @PostMapping(value = "/pdf/sed/utkast/{behandlingID}/{sedType}",
        produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Produserer utkast av SED som PDF")
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
