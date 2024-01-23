package no.nav.melosys.service.kontroll.feature.ferdigbehandling.data

import no.nav.melosys.domain.Medlemskapsperiode

data class MedlemskapsperiodeData(
    val nyeMedlemskapsperioder: List<Medlemskapsperiode>,
    val tidligereMedlemskapsperioder: List<Medlemskapsperiode> = emptyList()
) {

    fun harNyeMedlemskapsperioder(): Boolean {
        return nyeMedlemskapsperioder.isNotEmpty()
    }

    fun medlIdEksistererPåTidligereMedlemskapsperiode(medlId: Long): Boolean {
        return tidligereMedlemskapsperioder.any { medlemskapsperiode -> medlemskapsperiode.medlPeriodeID == (medlId) }
    }
}


