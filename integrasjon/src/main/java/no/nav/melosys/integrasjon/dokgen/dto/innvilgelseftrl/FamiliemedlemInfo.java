package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl;

public record FamiliemedlemInfo(
    String navn,
    String ident,
    IdentType identType
) {
}
