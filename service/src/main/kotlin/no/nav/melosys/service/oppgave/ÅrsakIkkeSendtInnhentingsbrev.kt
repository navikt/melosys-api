package no.nav.melosys.service.oppgave

enum class ÅrsakIkkeSendtInnhentingsbrev(val beskrivelse: String) {
    PERSON_DØD_UTEN_ADRESSE("personen er registrert som død og mangler mottakeradresse"),
    PERSON_UTEN_ADRESSE("personen mangler mottakeradresse"),
    VIRKSOMHET_SLETTET("virksomheten er slettet"),
}
