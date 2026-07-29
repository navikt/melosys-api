package no.nav.melosys.service.tekstblokk

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import no.nav.melosys.domain.brev.tekstblokk.Tekstblokk
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkType
import no.nav.melosys.repository.tekstblokk.TekstblokkRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class TekstblokkServiceTest {

    @MockK
    private lateinit var tekstblokkRepository: TekstblokkRepository

    private lateinit var service: TekstblokkService
    private lateinit var lagret: CapturingSlot<Tekstblokk>

    @BeforeEach
    fun setup() {
        service = TekstblokkService(tekstblokkRepository, TekstblokkHtmlSanitizer())
        lagret = slot()
        every { tekstblokkRepository.save(capture(lagret)) } answers { lagret.captured }
    }

    private fun opprettMedTags(vararg tags: String): List<String> {
        service.opprett(
            TekstblokkService.Input(
                tittel = "Tittel",
                innhold = "<p>Innhold</p>",
                type = TekstblokkType.TEKSTBLOKK,
                tags = tags.toList(),
            ),
        )
        return lagret.captured.tags.toList()
    }

    @Test
    fun `bevarer mellomrom i tags i stedet for aa lage bindestrek-slug`() {
        opprettMedTags("ny vurdering") shouldContainExactly listOf("ny vurdering")
    }

    @Test
    fun `bevarer store bokstaver i tags`() {
        opprettMedTags("USA-avtale", "Sør-Korea")
            .shouldContainExactlyInAnyOrder(listOf("USA-avtale", "Sør-Korea"))
    }

    @Test
    fun `trimmer ytterkanter og slaar sammen gjentatt blanktegn`() {
        opprettMedTags("  ny    vurdering  ") shouldContainExactly listOf("ny vurdering")
    }

    @Test
    fun `fjerner tomme tags`() {
        opprettMedTags("yrkesaktiv", "   ", "") shouldContainExactly listOf("yrkesaktiv")
    }

    @Test
    fun `fjerner naer-duplikater som kun skiller seg i bokstavstoerrelse og beholder foerste`() {
        opprettMedTags("USA-avtale", "usa-avtale") shouldContainExactly listOf("USA-avtale")
    }

    @Test
    fun `tags kan vaere null`() {
        service.opprett(
            TekstblokkService.Input(
                tittel = "Tittel",
                innhold = "<p>Innhold</p>",
                type = TekstblokkType.TEKSTBLOKK,
                tags = null,
            ),
        )

        lagret.captured.tags.shouldBeEmpty()
    }

    @Test
    fun `sanitererer innhold ved lagring`() {
        service.opprett(
            TekstblokkService.Input(
                tittel = "  Tittel med mellomrom  ",
                innhold = "<p>Tekst</p><script>alert('x')</script>",
                type = TekstblokkType.TEKSTBLOKK,
                tags = null,
            ),
        )

        lagret.captured.tittel shouldBe "Tittel med mellomrom"
        lagret.captured.innhold shouldBe "<p>Tekst</p>"
    }
}
