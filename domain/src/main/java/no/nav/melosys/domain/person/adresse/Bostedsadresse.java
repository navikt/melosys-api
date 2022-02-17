package no.nav.melosys.domain.person.adresse;

import java.time.LocalDate;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.brev.Postadresse;

public record Bostedsadresse(
    StrukturertAdresse strukturertAdresse,
    String coAdressenavn,
    LocalDate gyldigFraOgMed,
    LocalDate gyldigTilOgMed,
    String master,
    String kilde,
    boolean erHistorisk
) implements PersonAdresse {

    public Postadresse tilPostadresse() {
        if (strukturertAdresse != null) {
            return Postadresse.lagPostadresse(strukturertAdresse, coAdressenavn());
        }
        return null;
    }
}
