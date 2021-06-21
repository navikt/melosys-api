package no.nav.melosys.integrasjon.pdl.dto.person;

import java.time.LocalDate;

import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;

public record Statsborgerskap(String land,
                              LocalDate bekreftelsesdato,
                              LocalDate gyldigFraOgMed,
                              LocalDate gyldigTilOgMed,
                              Metadata metadata) implements HarMetadata {
}
