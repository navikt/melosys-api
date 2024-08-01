package no.nav.melosys.integrasjon.kodeverk

interface KodeverkRegister {
    fun hentKodeverk(kodeverkNavn: String): Kodeverk
}
