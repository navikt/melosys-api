package no.nav.melosys.domain.eessi;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_987_2009;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;

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
    H_BUC_10;

    public static BucType fraBestemmelse(LovvalgBestemmelse bestemmelse) {
        if (bestemmelse instanceof Lovvalgbestemmelser_883_2004) {
            return hentBucTypeFra883_2004((Lovvalgbestemmelser_883_2004) bestemmelse);
        } else if (bestemmelse instanceof Tilleggsbestemmelser_883_2004) {
            return hentBuctypeFraTilleggsBestemmelser((Tilleggsbestemmelser_883_2004) bestemmelse);
        } else if (bestemmelse == Lovvalgbestemmelser_987_2009.FO_987_2009_ART14_11) {
            return BucType.LA_BUC_02;
        } else {
            throw lagExceptionBestemmelseStøttesIkke(bestemmelse);
        }
    }

    private static BucType hentBuctypeFraTilleggsBestemmelser(Tilleggsbestemmelser_883_2004 bestemmelse) {
        if (bestemmelse == Tilleggsbestemmelser_883_2004.FO_883_2004_ART87_8 || bestemmelse == Tilleggsbestemmelser_883_2004.FO_883_2004_ART87A) {
            return LA_BUC_02;
        } else if (bestemmelse == Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5) {
            return LA_BUC_05;
        }

        throw lagExceptionBestemmelseStøttesIkke(bestemmelse);
    }

    private static BucType hentBucTypeFra883_2004(Lovvalgbestemmelser_883_2004 bestemmelse) {
        switch (bestemmelse) {
            case FO_883_2004_ART11_1:
            case FO_883_2004_ART11_3A:
            case FO_883_2004_ART11_3B:
            case FO_883_2004_ART11_3C:
            case FO_883_2004_ART11_3D:
            case FO_883_2004_ART11_3E:
            case FO_883_2004_ART11_4_2:
            case FO_883_2004_ART15:
                return BucType.LA_BUC_05;
            case FO_883_2004_ART12_1:
            case FO_883_2004_ART12_2:
                return BucType.LA_BUC_04;
            case FO_883_2004_ART13_1A:
            case FO_883_2004_ART13_1B1:
            case FO_883_2004_ART13_1B2:
            case FO_883_2004_ART13_1B3:
            case FO_883_2004_ART13_1B4:
            case FO_883_2004_ART13_2A:
            case FO_883_2004_ART13_2B:
            case FO_883_2004_ART13_3:
            case FO_883_2004_ART13_4:
                return BucType.LA_BUC_02;
            case FO_883_2004_ART16_1:
            case FO_883_2004_ART16_2:
                return BucType.LA_BUC_01;
            default:
                throw lagExceptionBestemmelseStøttesIkke(bestemmelse);
        }
    }

    private static IllegalArgumentException lagExceptionBestemmelseStøttesIkke(LovvalgBestemmelse bestemmelse) {
        return new IllegalArgumentException("Bestemmelse " + bestemmelse + " kan ikke mappes til en BucType!");
    }
}
