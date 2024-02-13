package no.nav.melosys.domain.dokument.person

import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.domain.dokument.felles.KodeverkHjelper


enum class Personstatus(private val kode: String) : KodeverkHjelper {
    ADNR("ADNR"),
    UTPE("UTPE"),
    BOSA("BOSA"),
    UREG("UREG"),
    ABNR("ABNR"),
    UFUL("UFUL"),
    UTVA("UTVA"),
    FOSV("FOSV"),
    DØDD("DØDD"),
    DØD("DØD"),
    UTAN("UTAN"),
    FØDR("FØDR");

    override fun getKode(): String {
        return kode
    }

    override fun hentKodeverkNavn(): FellesKodeverk {
        return FellesKodeverk.PERSONSTATUSER
    }
}
