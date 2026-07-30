package no.nav.melosys.service.placeholder

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.Sakstyper

/**
 * Én placeholder i registeret. Tom sakstyper-liste betyr at feltet gjelder alle sakstyper.
 */
data class PlaceholderDefinisjon(
    val nokkel: String,
    val visningsnavn: String,
    val beskrivelse: String,
    val eksempel: String,
    val sakstyper: List<Sakstyper> = emptyList(),
    val resolver: (Behandling) -> String?,
)
