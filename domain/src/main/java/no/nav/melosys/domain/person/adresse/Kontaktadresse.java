package no.nav.melosys.domain.person.adresse;

import java.time.LocalDateTime;

import no.nav.melosys.domain.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.brev.Postadresse;

public record Kontaktadresse(
    StrukturertAdresse strukturertAdresse,
    SemistrukturertAdresse semistrukturertAdresse,
    String coAdressenavn,
    LocalDateTime gyldigFraOgMed,
    LocalDateTime gyldigTilOgMed,
    String master,
    String kilde,
    LocalDateTime registrertDato,
    boolean erHistorisk
) implements PersonAdresse {
    public Postadresse tilPostadresse() {
        if (strukturertAdresse != null) {
            return Postadresse.lagPostadresse(strukturertAdresse);
        } else {
            return Postadresse.lagPostadresse(semistrukturertAdresse);
        }
    }
}
