package no.nav.melosys.domain

import no.nav.melosys.domain.kodeverk.Kodeverk

/**
 * https://confluence.adeo.no/display/APPKAT/Applikasjons-ID
 */
enum class Fagsystem(
    private val kode: String,
    private val beskrivelse: String
) : Kodeverk {
    // GSAK brukes som referanse til sakstilhørende fagsystem, men av historiske årsaker har det feil kode i Joark.
    GSAK_I_JOARK("FS22", "GSAK i Joark"), // Bruker Gosys appID
    INTET("", ""),
    MELOSYS("FS38", "Melosys");

    override fun getKode(): String = kode
    override fun getBeskrivelse(): String = beskrivelse
}
