package no.nav.melosys.domain.eessi;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.*;

public enum BucType {
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

    public static BucType fraBestemmelse(LovvalgBestemmelse bestemmelse) {
        if (bestemmelse instanceof Lovvalgbestemmelser_883_2004 lovvalgbestemmelse883_2004) {
            return hentBucTypeFra883_2004(lovvalgbestemmelse883_2004);
        } else if (bestemmelse instanceof Tilleggsbestemmelser_883_2004 tilleggsbestemmelse883_2004) {
            return hentBuctypeFraTilleggsBestemmelser883_2004(tilleggsbestemmelse883_2004);
        } else if (bestemmelse instanceof Lovvalgbestemmelser_konv_efta_storbritannia lovvalgbestemmelseKonvEfta) {
            return hentBucTypeFraKonvEfta(lovvalgbestemmelseKonvEfta);
        } else if (bestemmelse instanceof Tilleggsbestemmelser_konv_efta_storbritannia tilleggsbestemmelseKonvEfta) {
            return hentBuctypeFraTilleggsBestemmelserKonvEfta(tilleggsbestemmelseKonvEfta);
        } else if (bestemmelse == Lovvalgbestemmelser_987_2009.FO_987_2009_ART14_11) {
            return BucType.LA_BUC_02;
        } else {
            throw lagExceptionBestemmelseStøttesIkke(bestemmelse);
        }
    }

    private static BucType hentBuctypeFraTilleggsBestemmelser883_2004(Tilleggsbestemmelser_883_2004 bestemmelse) {
        if (bestemmelse == Tilleggsbestemmelser_883_2004.FO_883_2004_ART87_8 || bestemmelse == Tilleggsbestemmelser_883_2004.FO_883_2004_ART87A) {
            return LA_BUC_02;
        } else if (bestemmelse == Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5) {
            return LA_BUC_05;
        }
        throw lagExceptionBestemmelseStøttesIkke(bestemmelse);
    }

    private static BucType hentBuctypeFraTilleggsBestemmelserKonvEfta(Tilleggsbestemmelser_konv_efta_storbritannia bestemmelse) {
        if (bestemmelse == Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_5) {
            return LA_BUC_05;
        }
        throw lagExceptionBestemmelseStøttesIkke(bestemmelse);
    }

    private static BucType hentBucTypeFra883_2004(Lovvalgbestemmelser_883_2004 bestemmelse) {
        return switch (bestemmelse) {
            case FO_883_2004_ART11_1,
                FO_883_2004_ART11_3A,
                FO_883_2004_ART11_3B,
                FO_883_2004_ART11_3C,
                FO_883_2004_ART11_3D,
                FO_883_2004_ART11_3E,
                FO_883_2004_ART11_4_2,
                FO_883_2004_ART15 -> BucType.LA_BUC_05;
            case FO_883_2004_ART12_1,
                FO_883_2004_ART12_2 -> BucType.LA_BUC_04;
            case FO_883_2004_ART13_1A,
                FO_883_2004_ART13_1B1,
                FO_883_2004_ART13_1B2,
                FO_883_2004_ART13_1B3,
                FO_883_2004_ART13_1B4,
                FO_883_2004_ART13_2A,
                FO_883_2004_ART13_2B,
                FO_883_2004_ART13_3,
                FO_883_2004_ART13_4 -> BucType.LA_BUC_02;
            case FO_883_2004_ART16_1,
                FO_883_2004_ART16_2 -> BucType.LA_BUC_01;
            default -> throw lagExceptionBestemmelseStøttesIkke(bestemmelse);
        };
    }

    private static BucType hentBucTypeFraKonvEfta(Lovvalgbestemmelser_konv_efta_storbritannia bestemmelse) {
        return switch (bestemmelse) {
            case KONV_EFTA_STORBRITANNIA_ART13_3A,
                KONV_EFTA_STORBRITANNIA_ART13_3B,
                KONV_EFTA_STORBRITANNIA_ART13_3C,
                KONV_EFTA_STORBRITANNIA_ART13_3D,
                KONV_EFTA_STORBRITANNIA_ART13_4_2 -> BucType.LA_BUC_05;
            case KONV_EFTA_STORBRITANNIA_ART14_1,
                KONV_EFTA_STORBRITANNIA_ART14_2,
                KONV_EFTA_STORBRITANNIA_ART16_1,
                KONV_EFTA_STORBRITANNIA_ART16_3 -> BucType.LA_BUC_04;
            case KONV_EFTA_STORBRITANNIA_ART15_1A,
                KONV_EFTA_STORBRITANNIA_ART15_1B1,
                KONV_EFTA_STORBRITANNIA_ART15_1_B2,
                KONV_EFTA_STORBRITANNIA_ART15_1_B3,
                KONV_EFTA_STORBRITANNIA_ART15_1_B4,
                KONV_EFTA_STORBRITANNIA_ART15_2A,
                KONV_EFTA_STORBRITANNIA_ART15_3,
                KONV_EFTA_STORBRITANNIA_ART15_4 -> BucType.LA_BUC_02;
            case KONV_EFTA_STORBRITANNIA_ART18_1,
                KONV_EFTA_STORBRITANNIA_ART18_2 -> BucType.LA_BUC_01;
            default -> throw lagExceptionBestemmelseStøttesIkke(bestemmelse);
        };
    }

    private static IllegalArgumentException lagExceptionBestemmelseStøttesIkke(LovvalgBestemmelse bestemmelse) {
        return new IllegalArgumentException("Bestemmelse " + bestemmelse + " kan ikke mappes til en BucType!");
    }
}
