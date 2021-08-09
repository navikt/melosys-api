package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.folketrygden.ValgtRepresentant;

public record ValgtRepresentantDto(String representantnummer,
                                   boolean selvbetalende,
                                   String organisasjonsnummer,
                                   String kontaktperson) {

    public ValgtRepresentant til() {
        return new ValgtRepresentant(representantnummer, selvbetalende, organisasjonsnummer, kontaktperson);
    }

    public static ValgtRepresentantDto av(ValgtRepresentant valgtRepresentant) {
        return new ValgtRepresentantDto(
            valgtRepresentant.getRepresentantnummer(),
            valgtRepresentant.isSelvbetalende(),
            valgtRepresentant.getOrgnr(),
            valgtRepresentant.getKontaktperson());
    }
}
