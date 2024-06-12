package no.nav.melosys.service.brev.felles

import no.nav.dok.melosysbrev.felles.melosys_felles.TilleggsbestemmelseKode
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_konv_efta_storbritannia
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.brev.felles.LovvalgsbestemmelseKodeMapper.GB_KONV_BESTEMMELSER

object TilleggsbestemmelseKodeMapper {
    @JvmStatic
    fun map(bestemmelse: LovvalgBestemmelse): TilleggsbestemmelseKode {
        if (bestemmelse !in GB_KONV_BESTEMMELSER) {
            return TilleggsbestemmelseKode.fromValue(bestemmelse.kode)
        }

        val eøsBestemmelseForGBKonv: Tilleggsbestemmelser_883_2004 = when (bestemmelse) {
            Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_2 -> Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_2
            Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4_1 -> Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
            Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_5 -> Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5

            else -> throw FunksjonellException("Klarer ikke finne TilleggsbestemmelseKode fra bestemmelse $bestemmelse")
        }

        return TilleggsbestemmelseKode.fromValue(eøsBestemmelseForGBKonv.kode)
    }
}
