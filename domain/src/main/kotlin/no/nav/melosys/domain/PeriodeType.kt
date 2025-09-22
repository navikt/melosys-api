package no.nav.melosys.domain

import no.nav.melosys.domain.eessi.BucType

enum class PeriodeType {
    LOVVALGSPERIODE,
    ANMODNINGSPERIODE,
    UTPEKINGSPERIODE,
    INGEN;

    companion object {
        @JvmStatic
        fun fraBucType(bucType: BucType, behandlingsresultat: Behandlingsresultat): PeriodeType = when (bucType) {
            BucType.LA_BUC_01 -> ANMODNINGSPERIODE
            BucType.LA_BUC_04, BucType.LA_BUC_05 -> LOVVALGSPERIODE
            BucType.LA_BUC_02 -> if (behandlingsresultat.finnValidertUtpekingsperiode().isPresent) UTPEKINGSPERIODE else LOVVALGSPERIODE
            else -> INGEN
        }
    }
}
