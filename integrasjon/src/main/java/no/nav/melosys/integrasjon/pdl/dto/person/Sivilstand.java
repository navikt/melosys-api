package no.nav.melosys.integrasjon.pdl.dto.person;

import java.time.LocalDate;

import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;

public record Sivilstand(Sivilstandstype type,
                         String relatertVedSivilstand,
                         LocalDate gyldigFraOgMed,
                         Metadata metadata) implements HarMetadata {
}
