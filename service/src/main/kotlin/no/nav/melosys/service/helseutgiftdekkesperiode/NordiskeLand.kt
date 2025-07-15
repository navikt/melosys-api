package no.nav.melosys.service.helseutgiftdekkesperiode

import no.nav.melosys.domain.kodeverk.Land_iso2

class NordiskeLand {
    companion object {
        val NORDISKE_LAND = listOf(
            Land_iso2.DK,
            Land_iso2.FI,
            Land_iso2.IS,
            Land_iso2.NO,
            Land_iso2.SE,
            Land_iso2.FO,
            Land_iso2.GL,
            Land_iso2.AX
        )

        fun erNordiskLand(land: Land_iso2): Boolean = NORDISKE_LAND.any { it.kode == land.kode }
    }
}
