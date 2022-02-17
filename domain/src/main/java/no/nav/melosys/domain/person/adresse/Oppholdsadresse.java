package no.nav.melosys.domain.person.adresse;

import java.time.LocalDateTime;
import java.time.LocalDate;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.brev.Postadresse;

public record Oppholdsadresse(
    StrukturertAdresse strukturertAdresse,
    String coAdressenavn,
    LocalDate gyldigFraOgMed,
    LocalDate gyldigTilOgMed,
    String master,
    String kilde,
    LocalDateTime registrertDato,
    boolean erHistorisk
) implements PersonAdresse {

    public Postadresse tilPostadresse() {
        if (strukturertAdresse != null) {
            return Postadresse.lagPostadresse(coAdressenavn(), strukturertAdresse);
        }
        return null;
    }
}
