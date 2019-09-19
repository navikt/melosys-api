package no.nav.melosys.domain;

import no.nav.melosys.domain.eessi.BucType;

public enum MedlemsperiodeType {
    LOVVALGSPERIODE,
    ANMODNINGSPERIODE,
    INGEN;

    public static MedlemsperiodeType fraBucType(BucType bucType) {
        if (bucType == BucType.LA_BUC_01) {
            return MedlemsperiodeType.ANMODNINGSPERIODE;
        } else if (bucType == BucType.LA_BUC_02 || bucType == BucType.LA_BUC_04 || bucType == BucType.LA_BUC_05) {
            return MedlemsperiodeType.LOVVALGSPERIODE;
        }

        return MedlemsperiodeType.INGEN;
    }
}
