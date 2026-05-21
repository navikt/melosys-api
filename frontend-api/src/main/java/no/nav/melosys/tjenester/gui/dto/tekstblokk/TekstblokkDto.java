package no.nav.melosys.tjenester.gui.dto.tekstblokk;

import java.time.Instant;
import java.util.List;

import no.nav.melosys.domain.tekstblokk.TekstblokkType;

/**
 * Full DTO med innhold. Brukes ved henting av enkelt tekstblokk for visning eller redigering.
 */
public record TekstblokkDto(
    Long id,
    String tittel,
    String innhold,
    TekstblokkType type,
    List<String> tags,
    Instant registrertDato,
    String registrertAv,
    Instant endretDato,
    String endretAv
) {}
