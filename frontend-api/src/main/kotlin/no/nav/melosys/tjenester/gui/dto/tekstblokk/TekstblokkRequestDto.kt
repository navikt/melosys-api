package no.nav.melosys.tjenester.gui.dto.tekstblokk

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

import no.nav.melosys.domain.brev.tekstblokk.TekstblokkStatus
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkType
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.service.tekstblokk.TekstblokkService

/**
 * Body for POST og PUT. Tags trimmes på server, men beholder bokstavstørrelse.
 * Kontekstavgrensningene tas imot som enum – KodeDeserializer godtar rene koder
 * og gir 400 på ukjente verdier. Utelatt eller tom = «gjelder alle».
 *
 * Utelatt status betyr PUBLISERT ved opprettelse (bulk-seeding fra melosys-console er
 * uendret) og «uendret» ved oppdatering, slik at en redigering aldri publiserer et utkast
 * i vanvare. Admin-UI-et sender status eksplisitt når den skal endres.
 */
data class TekstblokkRequestDto(
    @field:NotBlank @field:Size(max = 200) val tittel: String,
    @field:NotBlank val innhold: String,
    @field:NotNull val type: TekstblokkType,
    val tags: List<@Size(max = 60) String>?,
    // KodeDeserializer gir null for tom streng. Elementtypen er ikke-nullbar, så Jackson
    // avviser den allerede ved deserialisering – nullen når aldri en not null-kolonne.
    val sakstyper: List<Sakstyper>? = null,
    val behandlingstemaer: List<Behandlingstema>? = null,
    val status: TekstblokkStatus? = null,
) {
    fun tilInput(): TekstblokkService.Input =
        TekstblokkService.Input(tittel, innhold, type, tags, sakstyper, behandlingstemaer, status)
}
