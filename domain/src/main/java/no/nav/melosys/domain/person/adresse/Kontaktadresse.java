package no.nav.melosys.domain.person.adresse;

import no.nav.melosys.domain.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.adresse.StrukturertAdresse;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Override
    public boolean erGyldig() {
        StrukturertAdresse adresse = hentEllerLagStrukturertAdresse();
        return !erHistorisk && adresse != null && adresse.erGyldig();
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
