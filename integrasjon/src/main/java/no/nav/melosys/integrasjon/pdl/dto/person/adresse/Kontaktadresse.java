package no.nav.melosys.integrasjon.pdl.dto.person.adresse;

import java.time.LocalDateTime;

import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;

public record Kontaktadresse(
    LocalDateTime gyldigFraOgMed,
    LocalDateTime gyldigTilOgMed,
    String coAdressenavn,
    Postboksadresse postboksadresse,
    PostadresseIFrittFormat postadresseIFrittFormat,
    UtenlandskAdresse utenlandskAdresse,
    UtenlandskAdresseIFrittFormat utenlandskAdresseIFrittFormat,
    Vegadresse vegadresse,
    Metadata metadata) implements HarMetadata {
}
