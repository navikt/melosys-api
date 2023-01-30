package no.nav.melosys.domain.brev.utkast;

public class VedleggUtkast {
    public record Saksvedlegg(
        String journalpostID,
        String dokumentID
    ) {
    }

    public record FritekstVedlegg(
        String tittel,
        String fritekst
    ) {
    }
}
