package no.nav.melosys.integrasjon.joark.saf.dto;

// Paginering av journalposter
public record SideInfo(
    String sluttpeker,
    boolean finnesNesteSide
) {
}
