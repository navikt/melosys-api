package no.nav.melosys.integrasjon.sak

fun interface SakClientInterface {
    fun opprettSak(sakDto: SakDto): SakDto
}
