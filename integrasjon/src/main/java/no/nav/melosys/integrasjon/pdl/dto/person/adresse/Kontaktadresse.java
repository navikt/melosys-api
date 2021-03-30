package no.nav.melosys.integrasjon.pdl.dto.person.adresse;

import java.time.LocalDateTime;

public record Kontaktadresse(
    KontaktadresseType type,
    LocalDateTime gyldigFraOgMed,
    LocalDateTime gyldigTilOgMed,
    String coAdressenavn,
    Postboksadresse postboksadresse,
    PostadresseIFrittFormat postadresseIFrittFormat,
    UtenlandskAdresse utenlandskAdresse,
    UtenlandskAdresseIFrittFormat utenlandskAdresseIFrittFormat,
    Vegadresse vegadresse
) {
}
