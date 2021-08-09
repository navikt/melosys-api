package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.Kontaktopplysning;

public record KontaktInfoDto(String kontaktnavn, String kontaktorgnr, String kontakttelefon) {
    public static KontaktInfoDto av(Kontaktopplysning kontaktopplysning) {
        return new KontaktInfoDto(kontaktopplysning.getKontaktNavn(), kontaktopplysning.getKontaktOrgnr(), kontaktopplysning.getKontaktTelefon());
    }
}
