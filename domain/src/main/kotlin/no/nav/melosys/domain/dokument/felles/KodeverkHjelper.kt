package no.nav.melosys.domain.dokument.felles

import no.nav.melosys.domain.FellesKodeverk

interface KodeverkHjelper {
    val kode: String?
    fun hentKodeverkNavn(): FellesKodeverk

    fun hentKode() = kode ?: error("kode er påkrevd for ${this::class.simpleName}}")
}
