package no.nav.melosys.service.kontroll.feature.ferdigbehandling.data

import no.nav.melosys.domain.Medlemskapsperiode

data class MedlemskapsperiodeData(
    val nyeMedlemskapsperioder: List<Medlemskapsperiode> = emptyList(),
    val tidligereMedlemskapsperioder: List<Medlemskapsperiode> = emptyList(),
    val nyeMedlemskapsperioderMedAvgift: List<Medlemskapsperiode> = emptyList(),
    val tidligereMedlemskapsperioderForBukerMedAvgift: List<Medlemskapsperiode> = emptyList()
) {

    fun harNyeMedlemskapsperioder(): Boolean {
        return nyeMedlemskapsperioder.isNotEmpty()
    }

    fun medlIdEksistererPåTidligereMedlemskapsperiode(medlId: Long): Boolean {
        return tidligereMedlemskapsperioder.any { medlemskapsperiode -> medlemskapsperiode.medlPeriodeID == (medlId) }
    }

    fun medlIdEksistererPåTidligereMedlemskapsperiodeMedAvgift(medlId: Long): Boolean {
        return tidligereMedlemskapsperioderForBukerMedAvgift.any { medlemskapsperiode -> medlemskapsperiode.medlPeriodeID == (medlId) }
    }
}


