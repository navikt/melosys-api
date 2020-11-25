package no.nav.melosys.domain;

import no.nav.melosys.domain.eessi.BucType;

public enum PeriodeType {
    LOVVALGSPERIODE,
    ANMODNINGSPERIODE,
    UTPEKINGSPERIODE,
    INGEN;

    public static PeriodeType fraBucType(BucType bucType, Behandlingsresultat behandlingsresultat) {
        if (bucType == BucType.LA_BUC_01) {
            return ANMODNINGSPERIODE;
        } else if (bucType == BucType.LA_BUC_04 || bucType == BucType.LA_BUC_05) {
            return LOVVALGSPERIODE;
        } else if (bucType == BucType.LA_BUC_02) {
            return behandlingsresultat.finnValidertUtpekingsperiode().isPresent() ? UTPEKINGSPERIODE : LOVVALGSPERIODE;
        }

        return INGEN;
    }
}
