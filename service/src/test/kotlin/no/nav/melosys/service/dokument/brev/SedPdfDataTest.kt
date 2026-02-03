package no.nav.melosys.service.dokument.brev

import io.getunleash.FakeUnleash
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.eessi.A008Formaal
import no.nav.melosys.domain.eessi.sed.SedDataDto
import org.junit.jupiter.api.Test

class SedPdfDataTest {

    val unleash = FakeUnleash().apply { enableAll() }

    @Test
    fun `utfyllSedDataDto setter a008Formaal på sedDataDto`() {
        val sedPdfData = SedPdfData().apply {
            a008Formaal = "arbeid_flere_land"
        }
        val sedDataDto = SedDataDto()

        sedPdfData.utfyllSedDataDto(unleash, sedDataDto)

        sedDataDto.a008Formaal shouldBe A008Formaal.ARBEID_FLERE_LAND
    }

    @Test
    fun `utfyllSedDataDto setter a008Formaal til null når ikke satt`() {
        val sedPdfData = SedPdfData()
        val sedDataDto = SedDataDto()

        sedPdfData.utfyllSedDataDto(unleash, sedDataDto)

        sedDataDto.a008Formaal.shouldBeNull()
    }

    @Test
    fun `utfyllSedDataDto setter fritekst som ytterligereInformasjon`() {
        val sedPdfData = SedPdfData().apply {
            setFritekst("Tilleggsinformasjon")
        }
        val sedDataDto = SedDataDto()

        sedPdfData.utfyllSedDataDto(unleash, sedDataDto)

        sedDataDto.ytterligereInformasjon shouldBe "Tilleggsinformasjon"
    }

    @Test
    fun `utfyllSedDataDto setter utpekingAvvis med begrunnelse og nyttLovvalgsland`() {
        val sedPdfData = SedPdfData().apply {
            setBegrunnelseUtenlandskMyndighet("Begrunnelse")
            setNyttLovvalgsland("SE")
            setVilSendeAnmodningOmMerInformasjon(true)
        }
        val sedDataDto = SedDataDto()

        sedPdfData.utfyllSedDataDto(unleash, sedDataDto)

        sedDataDto.utpekingAvvis!!.nyttLovvalgsland shouldBe "SE"
        sedDataDto.utpekingAvvis!!.begrunnelseUtenlandskMyndighet shouldBe "Begrunnelse"
        sedDataDto.utpekingAvvis!!.vilSendeAnmodningOmMerInformasjon shouldBe true
    }

    @Test
    fun `utfyllSedDataDto setter vilSendeAnmodningOmMerInformasjon til false som default`() {
        val sedPdfData = SedPdfData()
        val sedDataDto = SedDataDto()

        sedPdfData.utfyllSedDataDto(unleash, sedDataDto)

        sedDataDto.utpekingAvvis!!.vilSendeAnmodningOmMerInformasjon shouldBe false
    }
}
