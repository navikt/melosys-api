package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

// Paginering av journalposter
public record SideInfo(
    String sluttpeker,
    boolean finnesNesteSide
) {
}
