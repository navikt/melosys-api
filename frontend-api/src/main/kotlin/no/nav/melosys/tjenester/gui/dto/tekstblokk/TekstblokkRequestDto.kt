package no.nav.melosys.tjenester.gui.dto.tekstblokk

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

import no.nav.melosys.domain.tekstblokk.TekstblokkType

/**
 * Body for POST og PUT. Tags lagres normalisert (lowercase + trim) på server.
 */
data class TekstblokkRequestDto(
    @field:NotBlank @field:Size(max = 200) val tittel: String,
    @field:NotBlank val innhold: String,
    @field:NotNull val type: TekstblokkType,
    val tags: List<@Size(max = 60) String>?,
)
