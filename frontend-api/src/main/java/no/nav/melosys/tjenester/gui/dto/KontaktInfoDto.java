package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.Kontaktopplysning;

public class KontaktInfoDto {
    private String kontaktnavn;
    private String kontaktorgnr;
    private String kontakttelefon;

    public KontaktInfoDto(String kontaktnavn, String kontaktorgnr, String kontakttelefon) {
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
