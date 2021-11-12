package no.nav.melosys.integrasjon.dokgen.dto.felles;

import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;

public record Innvilgelse(
    String innledningFritekst,
    String begrunnelseFritekst,
    String ektefelleFritekst,
    String barnFritekst
) {
    public static Innvilgelse av(InnvilgelseBrevbestilling brevbestilling) {
        return new Innvilgelse(
            brevbestilling.getInnledningFritekst(),
            brevbestilling.getBegrunnelseFritekst(),
            brevbestilling.getEktefelleFritekst(),
            brevbestilling.getBarnFritekst()
        );
    }
}
