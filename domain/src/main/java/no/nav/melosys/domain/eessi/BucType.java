package no.nav.melosys.domain.eessi;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_987_2009;

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
        } else if (bestemmelse instanceof Lovvalgbestemmelser_987_2009) {
            return BucType.LA_BUC_02;
        } else {
            throw new IllegalArgumentException("Bestemmelse " + bestemmelse.name() + " er ikke støttet enda!");
        }
    }

    private static BucType hentBucTypeFra883_2004(Lovvalgbestemmelser_883_2004 bestemmelse) {
        switch (bestemmelse) {
            case FO_883_2004_ART11_1:
            case FO_883_2004_ART11_3A:
            case FO_883_2004_ART11_3B:
            case FO_883_2004_ART11_3C:
            case FO_883_2004_ART11_3E:
            case FO_883_2004_ART11_4_2:
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
                throw new IllegalArgumentException("Bestemmelse " + bestemmelse.name() + " er ikke støttet enda!");
        }
    }
}
