package no.nav.melosys.tjenester.gui.dto.tekstblokk;

import java.time.Instant;
import java.util.List;

import no.nav.melosys.domain.tekstblokk.TekstblokkType;

/**
 * Lett DTO uten innhold. Brukes for liste-visning og søk i frontend.
 */
public record TekstblokkOversiktDto(
    Long id,
    String tittel,
    TekstblokkType type,
    List<String> tags,
    Instant endretDato,
    String endretAv
) {}
