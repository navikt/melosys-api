package no.nav.melosys.domain.eessi

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.*

enum class BucType {
    LA_BUC_01,
    LA_BUC_02,
    LA_BUC_03,
    LA_BUC_04,
    LA_BUC_05,
    LA_BUC_06,

    H_BUC_01,
    H_BUC_02a,
    H_BUC_02b,
    H_BUC_02c,
    H_BUC_03a,
    H_BUC_03b,
    H_BUC_04,
    H_BUC_05,
    H_BUC_06,
    H_BUC_07,
    H_BUC_08,
    H_BUC_09,
    H_BUC_10,

    UB_BUC_01;

    companion object {
        @JvmStatic
        fun fraBestemmelse(bestemmelse: LovvalgBestemmelse): BucType =
            when (bestemmelse) {
                is Lovvalgbestemmelser_883_2004 -> hentBucTypeFra883_2004(bestemmelse)
                is Tilleggsbestemmelser_883_2004 -> hentBuctypeFraTilleggsBestemmelser883_2004(bestemmelse)
                is Lovvalgbestemmelser_konv_efta_storbritannia -> hentBucTypeFraKonvEfta(bestemmelse)
                is Tilleggsbestemmelser_konv_efta_storbritannia -> hentBuctypeFraTilleggsBestemmelserKonvEfta(bestemmelse)
                Lovvalgbestemmelser_987_2009.FO_987_2009_ART14_11 -> LA_BUC_02
                else -> throw lagExceptionBestemmelseStøttesIkke(bestemmelse)
            }

        private fun hentBuctypeFraTilleggsBestemmelser883_2004(bestemmelse: Tilleggsbestemmelser_883_2004): BucType =
            when (bestemmelse) {
                Tilleggsbestemmelser_883_2004.FO_883_2004_ART87_8,
                Tilleggsbestemmelser_883_2004.FO_883_2004_ART87A -> LA_BUC_02

                Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5 -> LA_BUC_05
                else -> throw lagExceptionBestemmelseStøttesIkke(bestemmelse)
            }

        private fun hentBuctypeFraTilleggsBestemmelserKonvEfta(bestemmelse: Tilleggsbestemmelser_konv_efta_storbritannia): BucType =
            when (bestemmelse) {
                Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_5 -> LA_BUC_05
                else -> throw lagExceptionBestemmelseStøttesIkke(bestemmelse)
            }

        private fun hentBucTypeFra883_2004(bestemmelse: Lovvalgbestemmelser_883_2004): BucType =
            when (bestemmelse) {
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_1,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3C,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3D,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART15 -> LA_BUC_05

                Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2 -> LA_BUC_04

                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B2,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B3,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B4,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2B,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_4 -> LA_BUC_02

                Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2 -> LA_BUC_01

                else -> throw lagExceptionBestemmelseStøttesIkke(bestemmelse)
            }

        private fun hentBucTypeFraKonvEfta(bestemmelse: Lovvalgbestemmelser_konv_efta_storbritannia): BucType =
            when (bestemmelse) {
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3A,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3B,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3C,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3D,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4_2 -> LA_BUC_05

                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART14_1,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART14_2,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART16_1,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART16_3 -> LA_BUC_04

                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1A,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1B1,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1_B2,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1_B3,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_1_B4,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_2A,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_3,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART15_4 -> LA_BUC_02

                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1,
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_2 -> LA_BUC_01

                else -> throw lagExceptionBestemmelseStøttesIkke(bestemmelse)
            }

        private fun lagExceptionBestemmelseStøttesIkke(bestemmelse: LovvalgBestemmelse): IllegalArgumentException =
            IllegalArgumentException("Bestemmelse $bestemmelse kan ikke mappes til en BucType!")
    }
}
