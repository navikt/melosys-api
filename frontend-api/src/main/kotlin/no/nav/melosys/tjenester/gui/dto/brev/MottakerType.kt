package no.nav.melosys.tjenester.gui.dto.brev

enum class MottakerType(@JvmField val beskrivelse: String) {
    BRUKER_ELLER_BRUKERS_FULLMEKTIG("Bruker eller brukers fullmektig"),
    VIRKSOMHET("Virksomheten saken er tilknyttet"),
    ARBEIDSGIVER_ELLER_ARBEIDSGIVERS_FULLMEKTIG("Arbeidsgiver eller arbeidsgivers fullmektig"),
    ANNEN_ORGANISASJON("Annen organisasjon"),
    NORSK_MYNDIGHET("Norske myndigheter"),
    UTENLANDSK_TRYGDEMYNDIGHET("Utenlandsk trygdemyndighet i avtaleland")
}
