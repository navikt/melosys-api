package no.nav.melosys.domain.person.adresse;

import no.nav.melosys.domain.adresse.StrukturertAdresse;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Override
    public boolean erGyldig() {
        return !erHistorisk && strukturertAdresse != null && strukturertAdresse.erGyldig();
    }
}
