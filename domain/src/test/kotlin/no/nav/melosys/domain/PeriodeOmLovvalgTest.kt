package no.nav.melosys.domain

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PeriodeOmLovvalgTest {

    @Test
    fun harForskjelligMedlID_medLikMedlID_girFalse() {
        val periodeOmLovvalg = PeriodeOmLovvalgMock()
        periodeOmLovvalg.harForskjelligMedlID(MOCK_MEDL_PERIODE_ID).shouldBeFalse()
    }

    @Test
    fun harForskjelligMedlID_medForskjelligMedlID_girTrue() {
        val periodeOmLovvalg = PeriodeOmLovvalgMock()
        periodeOmLovvalg.harForskjelligMedlID(1234L).shouldBeTrue()
    }

    class PeriodeOmLovvalgMock : PeriodeOmLovvalg {

        override fun getMedlPeriodeID(): Long = MOCK_MEDL_PERIODE_ID

        override var fom: LocalDate = LocalDate.of(2024, 1, 1)

        override var tom: LocalDate? = LocalDate.of(2024, 12, 31)

        override fun getBestemmelse(): LovvalgBestemmelse? = null

        override fun getLovvalgsland(): Land_iso2? = null

        override fun getTilleggsbestemmelse(): LovvalgBestemmelse? = null

        override fun getBehandlingsresultat(): Behandlingsresultat? = null

        override fun getDekning(): Trygdedekninger? = null
    }

    companion object {
        private const val MOCK_MEDL_PERIODE_ID = 1L
    }
}
