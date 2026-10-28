package no.nav.melosys.service.oppgave

sealed class ÅrsakIkkeSendtInnhentingsbrev(val beskrivelse: String) {
    data object PersonDødUtenAdresse : ÅrsakIkkeSendtInnhentingsbrev("personen er registrert som død og mangler mottakeradresse")
    data object PersonUtenAdresse : ÅrsakIkkeSendtInnhentingsbrev("personen mangler mottakeradresse")
    data class VirksomhetSlettet(val orgnr: String) : ÅrsakIkkeSendtInnhentingsbrev("virksomheten $orgnr er slettet")
}
