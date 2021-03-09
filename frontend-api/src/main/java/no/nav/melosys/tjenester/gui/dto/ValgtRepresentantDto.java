package no.nav.melosys.tjenester.gui.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.folketrygden.ValgtRepresentant;

public class ValgtRepresentantDto {
    private final String representantnummer;
    private final boolean selvbetalende;
    private final String organisasjonsnummer;
    private final String kontaktperson;

    @JsonCreator
    public ValgtRepresentantDto(@JsonProperty("representantnummer") String representantnummer,
                                @JsonProperty("selvbetalende") boolean selvbetalende,
                                @JsonProperty("orgnr") String organisasjonsnummer,
                                @JsonProperty("kontaktperson") String kontaktperson) {
        this.representantnummer = representantnummer;
        this.selvbetalende = selvbetalende;
        this.organisasjonsnummer = organisasjonsnummer;
        this.kontaktperson = kontaktperson;
    }

    public ValgtRepresentant til() {
        return new ValgtRepresentant(getRepresentantnummer(), isSelvbetalende(), getOrganisasjonsnummer(), getKontaktperson());
    }

    public static ValgtRepresentantDto av(ValgtRepresentant valgtRepresentant) {
        return new ValgtRepresentantDto(
            valgtRepresentant.getRepresentantnummer(),
            valgtRepresentant.isSelvbetalende(),
            valgtRepresentant.getOrgnr(),
            valgtRepresentant.getKontaktperson());
    }

    public String getRepresentantnummer() {
        return representantnummer;
    }

    public boolean isSelvbetalende() {
        return selvbetalende;
    }

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public String getKontaktperson() {
        return kontaktperson;
    }
}
