package no.nav.melosys.integrasjon.sak.dto

data class SakSearchRequest(
    var aktørId: String? = null, // Filtrering på saker opprettet for en aktør (person)
    var orgnr: String? = null, // Filtrering på saker opprettet for en organisasjon
    var applikasjon: String? = null, // Filtrering på applikasjon (iht felles kodeverk)
    var tema: String? = null, // Filtrering på tema (iht felles kodeverk)
    var fagsakNr: String? = null // Filtrering på fagsakNr")
)
