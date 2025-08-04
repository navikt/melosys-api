package no.nav.melosys.service.dokument.brev.bygger

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
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
        brevDatabyggerA1 = mockk<BrevDataByggerA1>()
        brevDatabyggerA001 = mockk<BrevDataByggerA001>()

        brevDataA1 = BrevDataA1()
        brevDataA001 = BrevDataA001()

        every { brevDatabyggerA1.lag(any(), any()) } returns brevDataA1
        every { brevDatabyggerA001.lag(any(), any()) } returns brevDataA001
    }

    @Test
    fun `testByggA1`() {
        val brevDataByggerVedlegg = BrevDataByggerVedlegg(brevDatabyggerA1, null)
        val brevData = brevDataByggerVedlegg.lag(mockk<BrevDataGrunnlag>(), "Z123456") as BrevDataVedlegg
        brevData.brevDataA1.shouldNotBeNull() shouldBe brevDataA1
    }

    @Test
    fun `testByggA001`() {
        val brevDataByggerVedlegg = BrevDataByggerVedlegg(brevDatabyggerA001, null)
        val brevData = brevDataByggerVedlegg.lag(mockk<BrevDataGrunnlag>(), "Z123456") as BrevDataVedlegg
        brevData.brevDataA001.shouldNotBeNull() shouldBe brevDataA001
    }

    @Test
    fun `testByggA1FraForhåndsvisning`() {
        val brevbestillingDto = BrevbestillingDto().apply {
            mottaker = Mottakerroller.BRUKER
            fritekst = "FRITEKST"
            begrunnelseKode = "tom"
        }

        val brevDataByggerVedlegg = BrevDataByggerVedlegg(brevDatabyggerA001, brevbestillingDto)
        val brevData = brevDataByggerVedlegg.lag(mockk<BrevDataGrunnlag>(), "Z123456") as BrevDataVedlegg
        brevData.begrunnelseKode shouldBe brevbestillingDto.begrunnelseKode
        brevData.fritekst shouldBe brevbestillingDto.fritekst
    }
}
