package no.nav.melosys.integrasjon.sak

data class SakSearchRequest(
    var aktørId: String?, // Filtrering på saker opprettet for en aktør (person)
    var orgnr: String?, // Filtrering på saker opprettet for en organisasjon
    var applikasjon: String?, // Filtrering på applikasjon (iht felles kodeverk)
    var tema: String?, // Filtrering på tema (iht felles kodeverk)
    var fagsakNr: String? // Filtrering på fagsakNr")
)
