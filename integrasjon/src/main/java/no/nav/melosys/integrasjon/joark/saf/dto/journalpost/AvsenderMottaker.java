package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

public record AvsenderMottaker(
    String id,
    AvsenderMottakerType type,
    String navn,
    String land
) {
}
