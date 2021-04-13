package no.nav.melosys.integrasjon.pdl.dto.person.adresse;

import java.time.LocalDateTime;

public record Oppholdsadresse(
    LocalDateTime gyldigFraOgMed,
    LocalDateTime gyldigTilOgMed,
    String coAdressenavn,
    UtenlandskAdresse utenlandskAdresse,
    Vegadresse vegadresse,
    Matrikkeladresse matrikkeladresse,
    String oppholdAnnetSted
) {
}
