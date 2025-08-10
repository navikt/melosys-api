package no.nav.melosys.integrasjon.sak

fun interface SakConsumerInterface {
    fun opprettSak(sakDto: SakDto): SakDto
}
