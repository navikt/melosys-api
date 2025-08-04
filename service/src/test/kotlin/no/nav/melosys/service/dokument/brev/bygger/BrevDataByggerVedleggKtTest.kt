package no.nav.melosys.service.dokument.brev.bygger

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.service.dokument.brev.BrevDataA001
import no.nav.melosys.service.dokument.brev.BrevDataA1
import no.nav.melosys.service.dokument.brev.BrevDataVedlegg
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BrevDataByggerVedleggKtTest {

    private lateinit var brevDatabyggerA1: BrevDataByggerA1
    private lateinit var brevDatabyggerA001: BrevDataByggerA001

    private lateinit var brevDataA1: BrevDataA1
    private lateinit var brevDataA001: BrevDataA001

    @BeforeEach
    fun setup() {
        brevDatabyggerA1 = mockk()
        brevDatabyggerA001 = mockk()

        brevDataA1 = BrevDataA1()
        brevDataA001 = BrevDataA001()

        every { brevDatabyggerA1.lag(any(), any()) } returns brevDataA1
        every { brevDatabyggerA001.lag(any(), any()) } returns brevDataA001
    }

    @Test
    fun testByggA1() {
        val brevDataByggerVedlegg = BrevDataByggerVedlegg(brevDatabyggerA1, null)
        val brevData = brevDataByggerVedlegg.lag(mockk<BrevDataGrunnlag>(), "Z123456")
        brevData.shouldBeInstanceOf<BrevDataVedlegg>()
        brevData.brevDataA1 shouldBe brevDataA1
    }

    @Test
    fun testByggA001() {
        val brevDataByggerVedlegg = BrevDataByggerVedlegg(brevDatabyggerA001, null)
        val brevData = brevDataByggerVedlegg.lag(mockk<BrevDataGrunnlag>(), "Z123456")
        brevData.shouldBeInstanceOf<BrevDataVedlegg>()
        brevData.brevDataA001 shouldBe brevDataA001
    }

    @Test
    fun testByggA1FraForhåndsvisning() {
        val brevbestillingDto = BrevbestillingDto()
        brevbestillingDto.mottaker = Mottakerroller.BRUKER
        brevbestillingDto.fritekst = "FRITEKST"
        brevbestillingDto.begrunnelseKode = "tom"

        val brevDataByggerVedlegg = BrevDataByggerVedlegg(brevDatabyggerA001, brevbestillingDto)
        val brevData = brevDataByggerVedlegg.lag(mockk<BrevDataGrunnlag>(), "Z123456")
        brevData.shouldBeInstanceOf<BrevDataVedlegg>()
        brevData.begrunnelseKode shouldBe brevbestillingDto.begrunnelseKode
        brevData.fritekst shouldBe brevbestillingDto.fritekst
    }
}
