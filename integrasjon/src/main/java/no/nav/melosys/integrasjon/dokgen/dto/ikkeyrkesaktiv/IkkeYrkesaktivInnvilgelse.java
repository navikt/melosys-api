package no.nav.melosys.integrasjon.dokgen.dto.ikkeyrkesaktiv;

import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;

public record IkkeYrkesaktivInnvilgelse(
    String innledningFritekst,
    String begrunnelseFritekst,
    String nyVurderingFritekst
) {
}
