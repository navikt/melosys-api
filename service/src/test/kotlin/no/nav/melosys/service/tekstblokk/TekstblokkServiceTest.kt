package no.nav.melosys.service.tekstblokk

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import no.nav.melosys.domain.brev.tekstblokk.Tekstblokk
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkType
import no.nav.melosys.repository.tekstblokk.TekstblokkRepository
import no.nav.melosys.service.bruker.SaksbehandlerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.AuditorAware
import java.util.Optional

@ExtendWith(MockKExtension::class)
class TekstblokkServiceTest {

    @MockK
    private lateinit var tekstblokkRepository: TekstblokkRepository

    @MockK
    private lateinit var saksbehandlerService: SaksbehandlerService

    @MockK
    private lateinit var auditorAware: AuditorAware<String>

    private lateinit var service: TekstblokkService
    private lateinit var lagret: CapturingSlot<Tekstblokk>

    @BeforeEach
    fun setup() {
        service = TekstblokkService(tekstblokkRepository, TekstblokkHtmlSanitizer(), saksbehandlerService, auditorAware)
        lagret = slot()
        every { tekstblokkRepository.save(capture(lagret)) } answers { lagret.captured }
        every { auditorAware.currentAuditor } returns Optional.of(IDENT)
        every { saksbehandlerService.finnNavnForIdent(IDENT) } returns Optional.of(NAVN)
    }

    private fun opprett(tittel: String = "Tittel", innhold: String = "<p>Innhold</p>", tags: List<String>? = null) =
        service.opprett(
            TekstblokkService.Input(
                tittel = tittel,
                innhold = innhold,
                type = TekstblokkType.TEKSTBLOKK,
                tags = tags,
            ),
        )

    private fun opprettMedTags(vararg tags: String): List<String> {
        opprett(tags = tags.toList())
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
        opprett()

        lagret.captured.tags.shouldBeEmpty()
    }

    @Test
    fun `sanitererer innhold ved lagring`() {
        opprett(tittel = "  Tittel med mellomrom  ", innhold = "<p>Tekst</p><script>alert('x')</script>")

        lagret.captured.tittel shouldBe "Tittel med mellomrom"
        lagret.captured.innhold shouldBe "<p>Tekst</p>"
    }

    @Test
    fun `lagrer navnet til den som endrer`() {
        opprett()

        lagret.captured.endretAvNavn shouldBe NAVN
    }

    @Test
    fun `lagrer uten navn naar identen ikke har treff`() {
        every { saksbehandlerService.finnNavnForIdent(IDENT) } returns Optional.empty()

        opprett()

        lagret.captured.endretAvNavn.shouldBeNull()
    }

    @Test
    fun `lagrer uten navn naar oppslaget feiler`() {
        every { saksbehandlerService.finnNavnForIdent(IDENT) } throws RuntimeException("Azure AD er nede")

        opprett()

        lagret.captured.endretAvNavn.shouldBeNull()
    }

    @Test
    fun `lagrer uten navn naar ingen bruker er innlogget`() {
        every { auditorAware.currentAuditor } returns Optional.empty()

        opprett()

        lagret.captured.endretAvNavn.shouldBeNull()
    }

    private companion object {
        const val IDENT = "A146170"
        const val NAVN = "Margareth Bjørgum"
    }
}
