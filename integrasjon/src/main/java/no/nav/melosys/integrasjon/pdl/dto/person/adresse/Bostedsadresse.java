package no.nav.melosys.integrasjon.pdl.dto.person.adresse;

import java.time.LocalDateTime;

import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;

public record Bostedsadresse(
    LocalDateTime gyldigFraOgMed,
    LocalDateTime gyldigTilOgMed,
    String coAdressenavn,
    Vegadresse vegadresse,
    Matrikkeladresse matrikkeladresse,
    UtenlandskAdresse utenlandskAdresse,
    UkjentBosted ukjentBosted,
    Metadata metadata) implements HarMetadata {
}
