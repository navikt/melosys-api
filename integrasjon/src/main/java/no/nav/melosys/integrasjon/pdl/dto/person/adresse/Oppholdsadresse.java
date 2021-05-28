package no.nav.melosys.integrasjon.pdl.dto.person.adresse;

import java.time.LocalDateTime;

import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;

public record Oppholdsadresse(
    LocalDateTime gyldigFraOgMed,
    LocalDateTime gyldigTilOgMed,
    String coAdressenavn,
    UtenlandskAdresse utenlandskAdresse,
    Vegadresse vegadresse,
    Matrikkeladresse matrikkeladresse,
    Metadata metadata) implements HarMetadata {
}
