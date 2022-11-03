package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl;

import no.nav.melosys.domain.mottatteopplysninger.data.IdentType;

public record FamiliemedlemInfo(
    String navn,
    String ident,
    IdentType identType
) {
}
