package no.nav.melosys.tjenester.gui.dto.tekstblokk;

import java.util.List;

import no.nav.melosys.domain.tekstblokk.TekstblokkType;

/**
 * Body for POST og PUT. Tags lagres normalisert (lowercase + trim) på server.
 */
public record TekstblokkRequestDto(
    String tittel,
    String innhold,
    TekstblokkType type,
    List<String> tags
) {}
