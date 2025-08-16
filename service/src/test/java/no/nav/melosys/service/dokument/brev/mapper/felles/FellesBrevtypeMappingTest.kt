package no.nav.melosys.service.dokument.brev.mapper.felles

import no.nav.dok.melosysbrev.felles.melosys_felles.LovvalgsbestemmelseKode
import no.nav.dok.melosysbrev.felles.melosys_felles.TilleggsbestemmelseKode
import no.nav.dok.melosysbrev.felles.melosys_felles.YrkesaktivitetsKode
import no.nav.dok.melosysbrev.felles.melosys_felles.YrkesgruppeKode
import no.nav.melosys.domain.kodeverk.Kodeverk
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_987_2009
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class FellesBrevtypeMappingTest {

    @Test
    fun `test lovvalgsbestemmelse kode`() {
        val uimplementerteEllerUgyldigeKoder = listOf(
            "FO_883_2004_ART11_3D", "FO_883_2004_ART11_4", "FO_883_2004_ART15"
        )

        val koder = (hentAlleVerdierFraKodeverk(Lovvalgbestemmelser_883_2004::class) +
                    hentAlleVerdierFraKodeverk(Lovvalgbestemmelser_987_2009::class))
            .filter { it !in uimplementerteEllerUgyldigeKoder }

        koder.forEach { LovvalgsbestemmelseKode.fromValue(it) }
    }

    @Test
    fun `test tilleggsbestemmelse koder`() {
        val uimplementerteEllerUgyldigeKoder = listOf(
            "FO_883_2004_ART87_8"
        )

        val koder = hentAlleVerdierFraKodeverk(Tilleggsbestemmelser_883_2004::class)
            .filter { it !in uimplementerteEllerUgyldigeKoder }

        koder.forEach { TilleggsbestemmelseKode.fromValue(it) }
    }

    @Test
    fun `test yrkesaktivitet koder`() {
        val koder = hentAlleVerdierFraKodeverk(Yrkesaktivitetstyper::class)
        koder.forEach { YrkesaktivitetsKode.fromValue(it) }
    }

    @Test
    fun `test yrkesgruppe koder`() {
        val koder = hentAlleVerdierFraKodeverk(Yrkesgrupper::class)
        koder.forEach { YrkesgruppeKode.fromValue(it) }
    }

    /*@Test todo kommenter tilbake når vi brev har synket med vårt kodeverk
    fun `test behandlingstype kode`() {
        val uimplementerteEllerUgyldigeKoder = listOf(
            "SOEKNAD_IKKE_YRKESAKTIV",
            "ANKE", // Ikke i bruk enda
            "REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING",
            "REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE",
            "BESLUTNING_LOVVALG_ANNET_LAND",
            "BESLUTNING_LOVVALG_NORGE",
            "ANMODNING_OM_UNNTAK_HOVEDREGEL",
            "ØVRIGE_SED",
            "VURDER_TRYGDETID",
            "SOEKNAD_ARBEID_FLERE_LAND",
            "SOEKNAD_ARBEID_NORGE_BOSATT_ANNET_LAND"
        )

        val koder = hentAlleVerdierFraKodeverk(Behandlingstyper::class)
            .filter { it !in uimplementerteEllerUgyldigeKoder }

        koder.forEach { BehandlingstypeKode.fromValue(it) }
    }*/

    companion object {
        fun hentAlleVerdierFraKodeverk(kodeverk: KClass<*>): List<String> {
            val valuesMethod = kodeverk.java.getDeclaredMethod("values")
            val result = valuesMethod.invoke(null)
            return (result as Array<Kodeverk>).map { it.kode }
        }
    }
}
