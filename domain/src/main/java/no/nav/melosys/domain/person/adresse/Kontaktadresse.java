package no.nav.melosys.domain.person.adresse;

import java.time.LocalDate;
import java.time.LocalDateTime;

import no.nav.melosys.domain.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.brev.Postadresse;

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

    public Postadresse tilPostadresse() {
        if (strukturertAdresse != null) {
            return Postadresse.lagPostadresse(coAdressenavn(), strukturertAdresse);
        } else if (semistrukturertAdresse != null) {
            return Postadresse.lagPostadresse(coAdressenavn(), semistrukturertAdresse);
        }
        return null;
    }

    public StrukturertAdresse hentEllerLagStrukturertAdresse() {
        if (strukturertAdresse != null) {
            return strukturertAdresse;
        } else if (semistrukturertAdresse != null) {
            return semistrukturertAdresse.tilStrukturertAdresse();
        }
        return null;
    }
}
