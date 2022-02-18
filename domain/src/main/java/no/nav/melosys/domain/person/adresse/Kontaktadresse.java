package no.nav.melosys.domain.person.adresse;

import java.time.LocalDate;
import java.time.LocalDateTime;

import no.nav.melosys.domain.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.adresse.StrukturertAdresse;

public record Kontaktadresse(
    StrukturertAdresse strukturertAdresse,
    SemistrukturertAdresse semistrukturertAdresse,
    String coAdressenavn,
    LocalDate gyldigFraOgMed,
    LocalDate gyldigTilOgMed,
    String master,
    String kilde,
    LocalDateTime registrertDato,
    boolean erHistorisk
) implements PersonAdresse {

    public StrukturertAdresse hentEllerLagStrukturertAdresse() {
        if (strukturertAdresse != null) {
            return strukturertAdresse;
        } else if (semistrukturertAdresse != null) {
            return semistrukturertAdresse.tilStrukturertAdresse();
        }
        return null;
    }
}
