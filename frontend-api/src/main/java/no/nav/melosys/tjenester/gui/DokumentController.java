package no.nav.melosys.tjenester.gui;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.BrukerIdType;
import no.nav.melosys.domain.arkiv.DokumentVariant;
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
    private static final String IMAGE_PNG = "image/png";
    private static final String IMAGE_JPEG = "image/jpeg";
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

    @GetMapping(value = "/{journalpostID}/{dokumentID}", produces = {APPLICATION_PDF, IMAGE_PNG, IMAGE_JPEG, APPLICATION_JSON_UTF8})
    @Operation(summary = "hent dokument knyttet til journalpost")
    public ResponseEntity<byte[]> hentDokument(@PathVariable("journalpostID") String journalpostID,
                                               @PathVariable("dokumentID") String dokumentID) {

        Journalpost journalpost = dokumentHentingService.hentJournalpost(journalpostID);
        auditerInnsyn(journalpost);

        byte[] dokument = dokumentHentingService.hentDokument(journalpostID, dokumentID);
        Visning visning = Visning.av(finnArkivFiltype(journalpost, dokumentID));
        return lagResponseAvDokument(dokument, "journalpost-dok-" + dokumentID, visning);
    }

    @Deprecated(since = "MELOSYS-5899")
    @GetMapping(value = "/pdf/{journalpostID}/{dokumentID}", produces = {APPLICATION_PDF, APPLICATION_JSON_UTF8})
    @Operation(summary = "hent dokument knyttet til journalpost")
    public ResponseEntity<byte[]> hentDokumentDeprecated(@PathVariable("journalpostID") String journalpostID,
                                               @PathVariable("dokumentID") String dokumentID) {

        Journalpost journalpost = dokumentHentingService.hentJournalpost(journalpostID);
        auditerInnsyn(journalpost);

        byte[] dokument = dokumentHentingService.hentDokument(journalpostID, dokumentID);
        return lagResponseAvDokument(dokument, "journalpost-dok-" + dokumentID, Visning.PDF);
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
        return lagResponseAvDokument(dokument, sedType.name() + "_utkast", Visning.PDF);
    }

    private void auditerInnsyn(Journalpost journalpost) {
        if (journalpost.getBrukerIdType() == null) return;
        String saksnummerMelding = (journalpost.getSaksnummer() != null) ? "på sak " + journalpost.getSaksnummer() : "";
        String melding = "Innsyn i dokument %s %s".formatted(journalpost.getHoveddokument().getTittel(), saksnummerMelding);
        if (journalpost.getBrukerIdType() == BrukerIdType.AKTØR_ID) {
            aksesskontroll.auditAutoriserAktørID(journalpost.getBrukerId(), melding);
        }
        if (journalpost.getBrukerIdType() == BrukerIdType.FOLKEREGISTERIDENT) {
            aksesskontroll.auditAutoriserFolkeregisterIdent(journalpost.getBrukerId(), melding);
        }
    }

    private static DokumentVariant.Filtype finnArkivFiltype(Journalpost journalpost, String dokumentID) {
        return finnDokument(journalpost, dokumentID)
            .flatMap(d -> d.getDokumentVarianter().stream()
                .filter(DokumentVariant::erVariantArkiv)
                .map(DokumentVariant::getFiltype)
                .filter(java.util.Objects::nonNull)
                .findFirst())
            .orElse(null);
    }

    private static Optional<ArkivDokument> finnDokument(Journalpost journalpost, String dokumentID) {
        ArkivDokument hoved = journalpost.getHoveddokument();
        if (hoved != null && dokumentID.equals(hoved.getDokumentId())) {
            return Optional.of(hoved);
        }
        return journalpost.getVedleggListe().stream()
            .filter(v -> dokumentID.equals(v.getDokumentId()))
            .findFirst();
    }

    private static ResponseEntity<byte[]> lagResponseAvDokument(byte[] dokument, String filnavnUtenExt, Visning visning) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, visning.contentType)
            .header(HttpHeaders.CONTENT_LENGTH, Integer.toString(dokument.length))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; attachment; filename=%s.%s".formatted(filnavnUtenExt, visning.extension))
            .body(dokument);
    }

    private enum Visning {
        PDF(APPLICATION_PDF, "pdf"),
        PNG(IMAGE_PNG, "png"),
        JPEG(IMAGE_JPEG, "jpg");

        final String contentType;
        final String extension;

        Visning(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }

        static Visning av(DokumentVariant.Filtype filtype) {
            if (filtype == null) return PDF;
            return switch (filtype) {
                case PNG -> PNG;
                case JPEG -> JPEG;
                default -> PDF;
            };
        }
    }
}
