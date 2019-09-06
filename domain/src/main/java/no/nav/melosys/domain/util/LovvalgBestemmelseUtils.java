package no.nav.melosys.domain.util;

import no.nav.melosys.domain.dokument.sed.BucType;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_987_2009;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;

public final class LovvalgBestemmelseUtils {

    private LovvalgBestemmelseUtils() {
        throw new IllegalStateException("Utility");
    }

    public static LovvalgBestemmelse dbDataTilLovvalgBestemmelse(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return Lovvalgbestemmelser_883_2004.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            // Bevisst NOOP for å fortsette oppslaget i andre oppramstyper.
        }
        try {
            return Lovvalgbestemmelser_987_2009.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            // Bevisst NOOP for å fortsette oppslaget i andre oppramstyper.
        }
        return Tilleggsbestemmelser_883_2004.valueOf(dbData);
    }

    public static BucType hentBucTypeFraBestemmelse(LovvalgBestemmelse bestemmelse) {
        if (bestemmelse instanceof Lovvalgbestemmelser_883_2004) {
            return hentBucTypeFra883_2004((Lovvalgbestemmelser_883_2004)bestemmelse);
        } else if(bestemmelse instanceof  Lovvalgbestemmelser_987_2009) {
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
            case FO_883_2004_ART13_1_B2:
            case FO_883_2004_ART13_1_B3:
            case FO_883_2004_ART13_1_B4:
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
