package no.nav.melosys.domain.person.adresse;

import java.time.LocalDateTime;

import no.nav.melosys.domain.adresse.StrukturertAdresse;

public record Bostedsadresse(
    StrukturertAdresse strukturertAdresse,
    String coAdressenavn,
    LocalDateTime gyldigFraOgMed,
    LocalDateTime gyldigTilOgMed,
    String master,
    String kilde,
    boolean erHistorisk
) implements PersonAdresse {
}
