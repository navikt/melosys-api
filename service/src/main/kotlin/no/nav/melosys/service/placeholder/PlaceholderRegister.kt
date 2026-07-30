package no.nav.melosys.service.placeholder

import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
            eksempel = { "2024/123456" },
            resolver = { it.saksnummer },
        ),
        PlaceholderDefinisjon(
            nokkel = "dagens-dato",
            visningsnavn = "Dagens dato",
            beskrivelse = "Dagens dato på formatet dd.MM.yyyy",
            eksempel = { iDag() },
            resolver = { iDag() },
        ),
        PlaceholderDefinisjon(
            nokkel = "fornavn",
            visningsnavn = "Fornavn",
            beskrivelse = "Brukerens fornavn",
            eksempel = { "Ola" },
            resolver = { it.persondata.fornavn },
        ),
        PlaceholderDefinisjon(
            nokkel = "etternavn",
            visningsnavn = "Etternavn",
            beskrivelse = "Brukerens etternavn",
            eksempel = { "Nordmann" },
            resolver = { it.persondata.etternavn },
        ),
        PlaceholderDefinisjon(
            nokkel = "fodselsdato",
            visningsnavn = "Fødselsdato",
            beskrivelse = "Brukerens fødselsdato på formatet dd.MM.yyyy",
            eksempel = { "15.03.2024" },
            resolver = { it.persondata.fødselsdato?.format(DATOFORMAT) },
        ),
        PlaceholderDefinisjon(
            nokkel = "fodselsnummer",
            visningsnavn = "Fødselsnummer",
            beskrivelse = "Brukerens fødselsnummer",
            eksempel = { "12345678901" },
            resolver = { it.persondata.hentFolkeregisterident() },
        ),
    )

    private companion object {
        private val DATOFORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        private fun iDag(): String = LocalDate.now().format(DATOFORMAT)
    }
}
