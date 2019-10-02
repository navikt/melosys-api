package no.nav.melosys.service.journalforing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.melosys.integrasjon.joark.DokumentKategoriKode;
import no.nav.melosys.integrasjon.joark.Variantformat;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.*;

final class OpprettJournalpostRequestMapper {

    private static final String SENTRAL_UTSKRIFT = "S";
    private static final String MEDLEMSKAP_OG_AVGIFT = "4530";

    private OpprettJournalpostRequestMapper() {
    }

    static OpprettJournalpostRequest opprettJournalpostSedSomBrev(
        Long gsakSakId, String tema, String fnr, String mottakernavn, String mottakerland, String journalpostTittel,
        String dokumentTittel, byte[] dokument, Map<String, byte[]> vedlegg) {

        return OpprettJournalpostRequest.builder()
            .avsenderMottaker(avsenderMottaker(mottakernavn, mottakerland))
            .bruker(bruker(fnr))
            .dokumenter(dokumenter(dokumentTittel, dokument, vedlegg))
            .journalfoerendeEnhet(MEDLEMSKAP_OG_AVGIFT)
            .journalpostType(OpprettJournalpostRequest.JournalpostType.UTGAAENDE)
            .kanal(SENTRAL_UTSKRIFT)
            .sak(arkivsak(gsakSakId.toString()))
            .tema(tema)
            .tittel(journalpostTittel)
            .tilleggsopplysninger(tilleggsopplysning(null))
            .build();
    }

    private static AvsenderMottaker avsenderMottaker(String navn, String land) {
        return AvsenderMottaker.builder()
            .navn(navn)
            .land(land)
            .build();
    }

    private static Bruker bruker(String fnr) {
        return Bruker.builder()
            .id(fnr).idType(Bruker.BrukerIdType.FNR)
            .build();
    }

    private static List<Dokument> dokumenter(String dokumentTittel, byte[] dokument, Map<String, byte[]> vedlegg) {
        List<Dokument> dokumenter = new ArrayList<>();
        dokumenter.add(tilSedPdf(dokumentTittel, dokument));

        if (vedlegg != null) {
            dokumenter.addAll(vedlegg.entrySet().stream()
                .map(entry -> tilSedPdf(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));
        }

        return dokumenter;
    }

    private static Dokument tilSedPdf(String tittel, byte[] dokument) {
        return Dokument.builder()
            .tittel(tittel)
            .dokumentKategori(DokumentKategoriKode.SED.getKode())
            .dokumentvarianter(Collections.singletonList(
                DokumentVariant.builder()
                    .filtype(JournalpostFiltype.PDFA)
                    .fysiskDokument(dokument)
                    .variantformat(Variantformat.ARKIV.name())
                    .build()))
            .build();
    }

    private static Sak arkivsak(String gsakSaksnummer) {
        return Sak.builder().arkivsaksnummer(gsakSaksnummer).build();
    }

    private static List<Tilleggsopplysning> tilleggsopplysning(Map<String, String> tilleggsopplysninger) {
        if (tilleggsopplysninger == null) {
            return Collections.emptyList();
        }

        return tilleggsopplysninger.entrySet().stream()
            .map(entry -> new Tilleggsopplysning(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }
}
