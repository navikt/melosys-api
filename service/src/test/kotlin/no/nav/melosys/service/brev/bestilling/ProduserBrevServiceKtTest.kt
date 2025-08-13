package no.nav.melosys.service.brev.bestilling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.INNVILGELSE_FOLKETRYGDLOVEN
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_BRUKER
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.dokument.DokumentServiceFasade
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ProduserBrevServiceKtTest {

    @MockK
    private lateinit var dokumentServiceFasade: DokumentServiceFasade

    @InjectMockKs
    private lateinit var produserBrevService: ProduserBrevService

    @Test
    fun `skalBestilleProduseringAvBrev`() {
        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = MANGELBREV_BRUKER
        }
        every { dokumentServiceFasade.produserDokument(any(), any()) } returns Unit


        produserBrevService.produserBrev(333L, brevbestillingDto)


        verify { dokumentServiceFasade.produserDokument(any(), any()) }
    }

    @Test
    fun `produserBrev InnvilgelseFtrl skalIkkeTillates`() {
        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = INNVILGELSE_FOLKETRYGDLOVEN
        }


        val exception = shouldThrow<FunksjonellException> {
            produserBrevService.produserBrev(333L, brevbestillingDto)
        }


        exception.message shouldContain "Manuell bestilling av INNVILGELSE_FOLKETRYGDLOVEN er ikke støttet."
    }
}
