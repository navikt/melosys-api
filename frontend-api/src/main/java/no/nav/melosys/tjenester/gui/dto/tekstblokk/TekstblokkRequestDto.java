package no.nav.melosys.tjenester.gui.dto.tekstblokk;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import no.nav.melosys.domain.tekstblokk.TekstblokkType;

public record TekstblokkRequestDto(
    @NotBlank @Size(max = 200) String tittel,
    @NotBlank String innhold,
    @NotNull TekstblokkType type,
    List<String> tags
) {}
