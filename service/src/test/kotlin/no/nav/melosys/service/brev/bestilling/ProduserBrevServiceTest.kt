package no.nav.melosys.service.brev.bestilling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.kodeverk.Mottakerroller.BRUKER
import no.nav.melosys.domain.kodeverk.Mottakerroller.FULLMEKTIG
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.INNVILGELSE_FOLKETRYGDLOVEN
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_BRUKER
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.brev.BrevAdresse
import no.nav.melosys.service.dokument.DokumentServiceFasade
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.KopiMottakerDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ProduserBrevServiceTest {

    @MockK
    private lateinit var dokumentServiceFasade: DokumentServiceFasade

    @MockK
    private lateinit var hentBrevAdresseTilMottakereService: HentBrevAdresseTilMottakereService

    @InjectMockKs
    private lateinit var produserBrevService: ProduserBrevService

    @Test
    fun `skal bestille produsering av brev`() {
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

    @Test
    fun `produserBrev kopi til bruker uten gyldig adresse skalIkkeTillates`() {
        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = MANGELBREV_ARBEIDSGIVER
            kopiMottakere = listOf(KopiMottakerDto(BRUKER, null, "aktørId", null))
        }
        every { hentBrevAdresseTilMottakereService.hentBrevAdresseTilMottakere(333L, BRUKER) } returns
            listOf(BrevAdresse("Bruker", null, null, null, null, null, "NO"))


        val exception = shouldThrow<FunksjonellException> {
            produserBrevService.produserBrev(333L, brevbestillingDto)
        }


        exception.message shouldBe "Bruker/brukers fullmektig mangler en gyldig adresse."
        verify(exactly = 0) { dokumentServiceFasade.produserDokument(any(), any()) }
    }

    @Test
    fun `produserBrev kopi til privat fullmektig uten gyldig adresse skalIkkeTillates`() {
        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = MANGELBREV_ARBEIDSGIVER
            kopiMottakere = listOf(KopiMottakerDto(FULLMEKTIG, null, null, null))
        }
        every { hentBrevAdresseTilMottakereService.hentBrevAdresseTilMottakere(333L, FULLMEKTIG) } returns
            listOf(BrevAdresse("Fullmektig", null, null, null, null, null, "SE"))


        shouldThrow<FunksjonellException> {
            produserBrevService.produserBrev(333L, brevbestillingDto)
        }


        verify(exactly = 0) { dokumentServiceFasade.produserDokument(any(), any()) }
    }

    @Test
    fun `produserBrev kopi til bruker med gyldig adresse skal bestilles`() {
        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = MANGELBREV_ARBEIDSGIVER
            kopiMottakere = listOf(KopiMottakerDto(BRUKER, null, "aktørId", null))
        }
        every { hentBrevAdresseTilMottakereService.hentBrevAdresseTilMottakere(333L, BRUKER) } returns
            listOf(BrevAdresse("Bruker", null, listOf("Gate 1"), "0123", "Oslo", null, "NO"))
        every { dokumentServiceFasade.produserDokument(any(), any()) } returns Unit


        produserBrevService.produserBrev(333L, brevbestillingDto)


        verify { dokumentServiceFasade.produserDokument(333L, brevbestillingDto) }
    }

    @Test
    fun `produserBrev kopi til organisasjonsfullmektig skal ikke sjekke adresse`() {
        val brevbestillingDto = BrevbestillingDto().apply {
            produserbardokument = MANGELBREV_ARBEIDSGIVER
            kopiMottakere = listOf(KopiMottakerDto(FULLMEKTIG, "123456789", null, null))
        }
        every { dokumentServiceFasade.produserDokument(any(), any()) } returns Unit


        produserBrevService.produserBrev(333L, brevbestillingDto)


        verify(exactly = 0) { hentBrevAdresseTilMottakereService.hentBrevAdresseTilMottakere(any(), any()) }
        verify { dokumentServiceFasade.produserDokument(333L, brevbestillingDto) }
    }
}
