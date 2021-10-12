package no.nav.melosys.integrasjon.dokgen.dto.felles;

import java.util.List;

public record Mottaker(
    String navn,
    List<String> adresselinjer,
    String postnr,
    String poststed,
    String land
) {
}
