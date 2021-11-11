package no.nav.melosys.domain.person.adresse;

import java.time.LocalDateTime;
import java.time.LocalDate;

import no.nav.melosys.domain.adresse.StrukturertAdresse;

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
}
