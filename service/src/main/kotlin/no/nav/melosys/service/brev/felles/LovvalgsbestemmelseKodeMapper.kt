package no.nav.melosys.service.brev.felles

import no.nav.dok.melosysbrev.felles.melosys_felles.LovvalgsbestemmelseKode
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_konv_efta_storbritannia
import no.nav.melosys.exception.FunksjonellException

object LovvalgsbestemmelseKodeMapper {
    val GB_KONV_BESTEMMELSER: List<LovvalgBestemmelse> =
        listOf(*Lovvalgbestemmelser_konv_efta_storbritannia.values(), *Tilleggsbestemmelser_konv_efta_storbritannia.values())

    @JvmStatic
    fun map(bestemmelse: LovvalgBestemmelse): LovvalgsbestemmelseKode {
        if (bestemmelse !in GB_KONV_BESTEMMELSER) {
            return LovvalgsbestemmelseKode.fromValue(bestemmelse.kode)
        }

        val eøsBestemmelseForGBKonv: LovvalgBestemmelse = when (bestemmelse) {
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3A -> Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3B -> Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3C -> Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3D
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3D -> Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4 -> Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4_2 -> Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2

            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART14_1,
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART16_1 -> Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1

            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART14_2,
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART16_3 -> Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2

            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1A -> Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1B1 -> Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1_B2 -> Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B2
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1_B3 -> Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B3
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1_B4 -> Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B4
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_2A -> Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_2B -> Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2B
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_3 -> Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_4 -> Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_4
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_5 -> Lovvalgbestemmelser_883_2004.FO_883_2004_ANNET
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1 -> Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_2 -> Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2

            Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_2 -> Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_2
            Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4_1 -> Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1
            Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_5 -> Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5
            else -> throw FunksjonellException("Klarer ikke finne LovvalgsbestemmelseKode fra bestemmelse $bestemmelse")
        }

        return LovvalgsbestemmelseKode.fromValue(eøsBestemmelseForGBKonv.kode)
    }
}
