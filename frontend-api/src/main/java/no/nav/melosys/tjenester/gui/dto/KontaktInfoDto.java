package no.nav.melosys.tjenester.gui.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.Kontaktopplysning;

public class KontaktInfoDto {
    private String kontaktnavn;
    private String kontaktorgnr;
    private String kontakttelefon;

    @JsonCreator
    public KontaktInfoDto(@JsonProperty("kontaktnavn") String kontaktnavn,
                          @JsonProperty("kontaktorgnr") String kontaktorgnr,
                          @JsonProperty("kontakttelefon") String kontakttelefon) {
        this.kontaktnavn = kontaktnavn;
        this.kontaktorgnr = kontaktorgnr;
        this.kontakttelefon = kontakttelefon;
    }

    public static KontaktInfoDto av(Kontaktopplysning kontaktopplysning) {
        return new KontaktInfoDto(kontaktopplysning.getKontaktNavn(), kontaktopplysning.getKontaktOrgnr(), kontaktopplysning.getKontaktTelefon());
    }

    public String getKontaktnavn() {
        return kontaktnavn;
    }

    public String getKontaktorgnr() {
        return kontaktorgnr;
    }

    public String getKontakttelefon() {
        return kontakttelefon;
    }
}
