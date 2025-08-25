package no.nav.melosys.service.kontroll.feature.ufm.kontroll

import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.service.kontroll.feature.ufm.data.UfmKontrollData

object UfmKontrollsett {

    fun hentRegelsettForSedType(sedType: SedType): Set<(UfmKontrollData) -> Kontroll_begrunnelser?> =
        when (sedType) {
            SedType.A001 -> REGELSETT_A001
            SedType.A003 -> REGELSETT_A003
            SedType.A009 -> REGELSETT_A009
            SedType.A010 -> REGELSETT_A010
            else -> throw UnsupportedOperationException("SedType: $sedType er ikke støttet for automatiske kontroller")
        }

    private val REGELSETT_A001 = setOf<(UfmKontrollData) -> Kontroll_begrunnelser?>(
        UfmKontroll::periodeErÅpen,
        UfmKontroll::periodeStarterFørFørsteJuni2012,
        UfmKontroll::periodeOver5År,
        UfmKontroll::periodeOver1ÅrFremITid,
        UfmKontroll::overlappendeMedlemsperiode,
        UfmKontroll::statsborgerskapIkkeMedlemsland,
        UfmKontroll::personDød,
        UfmKontroll::personBosattINorge,
        UfmKontroll::utbetaltYtelserFraOffentligIPeriode,
        UfmKontroll::arbeidsland
    )

    private val REGELSETT_A003 = setOf(
        UfmKontroll::periodeErÅpen,
        UfmKontroll::periodeStarterFørFørsteJuni2012,
        UfmKontroll::periodeOver5År,
        UfmKontroll::periodeOver1ÅrFremITid,
        UfmKontroll::overlappendeMedlemsperiodeForA003,
        UfmKontroll::statsborgerskapIkkeMedlemsland,
        UfmKontroll::personDød,
        UfmKontroll::personBosattINorgeIPerioden,
        UfmKontroll::utbetaltYtelserFraOffentligIPeriode,
        UfmKontroll::arbeidsland,
        UfmKontroll::unntakForA003
    )

    private val REGELSETT_A009 = setOf(
        UfmKontroll::periodeErÅpen,
        UfmKontroll::periodeStarterFørFørsteJuni2012,
        UfmKontroll::periodeOver24MånederOgEnDag,
        UfmKontroll::periodeOver1ÅrFremITid,
        UfmKontroll::overlappendeMedlemsperiodeMerEnn1Dag,
        UfmKontroll::lovvalgslandErNorge,
        UfmKontroll::statsborgerskapIkkeMedlemsland,
        UfmKontroll::personDød,
        UfmKontroll::utbetaltYtelserFraOffentligIPeriode,
        UfmKontroll::arbeidsland
    )

    private val REGELSETT_A010 = setOf(
        UfmKontroll::periodeErÅpen,
        UfmKontroll::periodeStarterFørFørsteJuni2012,
        UfmKontroll::periodeOver5År,
        UfmKontroll::periodeOver1ÅrFremITid,
        UfmKontroll::overlappendeMedlemsperiodeMerEnn1Dag,
        UfmKontroll::lovvalgslandErNorge,
        UfmKontroll::statsborgerskapIkkeMedlemsland,
        UfmKontroll::personDød,
        UfmKontroll::utbetaltYtelserFraOffentligIPeriode,
        UfmKontroll::arbeidsland
    )
}
