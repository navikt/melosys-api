package no.nav.melosys.service.placeholder

import org.springframework.stereotype.Component

/**
 * Registeret over placeholdere til brev. Registeret er kode, ikke database: én definisjon per felt.
 */
@Component
class PlaceholderRegister {

    val definisjoner: List<PlaceholderDefinisjon> = listOf(
        PlaceholderDefinisjon(
            nokkel = "saksnummer",
            visningsnavn = "Saksnummer",
            beskrivelse = "Sakens saksnummer i Melosys",
            eksempel = "2024/123456",
            resolver = { it.fagsak.saksnummer },
        ),
    )
}
