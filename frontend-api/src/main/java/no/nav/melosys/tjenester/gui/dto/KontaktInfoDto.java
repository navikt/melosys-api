package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.Kontaktopplysning;

public class KontaktInfoDto {
    private String kontaktnavn;
    private String kontaktorgnr;

    public KontaktInfoDto(String kontaktnavn, String kontaktorgnr) {
        this.kontaktnavn = kontaktnavn;
        this.kontaktorgnr = kontaktorgnr;
    }

    public static KontaktInfoDto av(Kontaktopplysning kontaktopplysning) {
        return new KontaktInfoDto(kontaktopplysning.getKontaktNavn(), kontaktopplysning.getKontaktOrgnr());
    }

    public String getKontaktnavn() {
        return kontaktnavn;
    }

    public String getKontaktorgnr() {
        return kontaktorgnr;
    }
}
