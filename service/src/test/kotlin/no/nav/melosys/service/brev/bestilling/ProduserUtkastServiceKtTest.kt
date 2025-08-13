package no.nav.melosys.service.brev.bestilling

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.FRITEKSTBREV
import no.nav.melosys.service.dokument.DokgenService
import no.nav.melosys.service.dokument.DokumentService
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ProduserUtkastServiceKtTest {

    @MockK
    private lateinit var dokgenService: DokgenService

    @MockK
    private lateinit var dokumentService: DokumentService

    @InjectMockKs
    private lateinit var produserUtkastService: ProduserUtkastService

    @Test
    fun `produserUtkast medTilgjengeligDokgenmal forventerViBrukerVårDokgen`() {
        every { dokgenService.erTilgjengeligDokgenmal(Produserbaredokumenter.FRITEKSTBREV) } returns true
        every { dokgenService.produserUtkast(any(), any()) } returns ByteArray(0)
        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = FRITEKSTBREV
        }


        produserUtkastService.produserUtkast(333L, brevbestillingDto)


        verify { dokgenService.produserUtkast(333L, brevbestillingDto) }
        verify(exactly = 0) { dokumentService.produserUtkast(any(), any()) }
    }

    @Test
    fun `produserUtkast medIngenTilgjengeligDokgenmal forventerViBrukerDokumentService`() {
        every { dokgenService.erTilgjengeligDokgenmal(Produserbaredokumenter.FRITEKSTBREV) } returns false
        every { dokumentService.produserUtkast(any(), any()) } returns ByteArray(0)
        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = FRITEKSTBREV
        }


        produserUtkastService.produserUtkast(333L, brevbestillingDto)


        verify { dokumentService.produserUtkast(333L, brevbestillingDto) }
        verify(exactly = 0) { dokgenService.produserUtkast(any(), any()) }
    }
}
