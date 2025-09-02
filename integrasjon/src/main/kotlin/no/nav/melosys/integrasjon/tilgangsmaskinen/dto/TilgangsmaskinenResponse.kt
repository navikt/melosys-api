package no.nav.melosys.integrasjon.tilgangsmaskinen.dto

/**
 * ProblemDetail response for Tilgangsmaskinen ved feil (403 Forbidden)
 * Følger RFC7807 standard for feilresponser.
 */
data class TilgangsmaskinenProblemDetail(
    val type: String,
    val title: AvvisningsKode,
    val status: Int,
    val instance: String,
    val brukerIdent: String,
    val navIdent: String,
    val begrunnelse: String,
    val traceId: String,
    val kanOverstyres: Boolean
)

/**
 * Enum for avvisningskoder som returneres i 'title' feltet fra Tilgangsmaskinen
 * Basert på offisiell OpenAPI spesifikasjon.
 */
enum class AvvisningsKode {
    AVVIST_STRENGT_FORTROLIG_ADRESSE,
    AVVIST_STRENGT_FORTROLIG_UTLAND,
    AVVIST_AVDØD,
    AVVIST_PERSON_UTLAND,
    AVVIST_SKJERMING,
    AVVIST_FORTROLIG_ADRESSE,
    AVVIST_UKJENT_BOSTED,
    AVVIST_GEOGRAFISK,
    AVVIST_HABILITET
}
