package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl;

import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;

public record IkkeOmfattetBarn(
    FamiliemedlemInfo info,
    Medfolgende_barn_begrunnelser begrunnelse
) {
}
