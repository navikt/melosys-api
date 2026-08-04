package no.nav.melosys.service.tekstblokk

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.brev.tekstblokk.Tekstblokk
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkOversikt
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkType
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.repository.tekstblokk.TekstblokkRepository
import no.nav.melosys.service.bruker.SaksbehandlerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.AuditorAware
import java.time.Instant
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

    private fun opprett(
        tittel: String = "Tittel",
        innhold: String = "<p>Innhold</p>",
        tags: List<String>? = null,
        sakstyper: List<Sakstyper>? = null,
        behandlingstemaer: List<Behandlingstema>? = null,
    ) = service.opprett(
        TekstblokkService.Input(
            tittel = tittel,
            innhold = innhold,
            type = TekstblokkType.TEKSTBLOKK,
            tags = tags,
            sakstyper = sakstyper,
            behandlingstemaer = behandlingstemaer,
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
    fun `lagrer kontekstavgrensning`() {
        opprett(
            sakstyper = listOf(Sakstyper.EU_EOS, Sakstyper.TRYGDEAVTALE),
            behandlingstemaer = listOf(Behandlingstema.ARBEID_FLERE_LAND),
        )

        lagret.captured.sakstyper.shouldContainExactlyInAnyOrder(setOf(Sakstyper.EU_EOS, Sakstyper.TRYGDEAVTALE))
        lagret.captured.behandlingstemaer shouldContainExactly setOf(Behandlingstema.ARBEID_FLERE_LAND)
    }

    @Test
    fun `uten avgrensning er blokken tom - altsaa gjelder alle`() {
        opprett()

        lagret.captured.sakstyper.shouldBeEmpty()
        lagret.captured.behandlingstemaer.shouldBeEmpty()
    }

    @Test
    fun `oppdatering uten avgrensning fjerner en tidligere avgrensning`() {
        val eksisterende = Tekstblokk(id = 1).apply {
            sakstyper += Sakstyper.EU_EOS
            behandlingstemaer += Behandlingstema.ARBEID_FLERE_LAND
        }
        every { tekstblokkRepository.findByIdAndSlettetDatoIsNull(1) } returns Optional.of(eksisterende)

        service.oppdater(1, TekstblokkService.Input("Tittel", "<p>Tekst</p>", TekstblokkType.TEKSTBLOKK, null))

        lagret.captured.sakstyper.shouldBeEmpty()
        lagret.captured.behandlingstemaer.shouldBeEmpty()
    }

    @Test
    fun `oversikten faar avgrensningene slaatt sammen per blokk`() {
        val oversikt = TekstblokkOversikt(1, "Tittel", "<p>Tekst</p>", TekstblokkType.TEKSTBLOKK, NÅ, IDENT, NAVN)
        every { tekstblokkRepository.finnOversikt(null) } returns listOf(oversikt)
        every { tekstblokkRepository.finnTagsForIds(listOf(1L)) } returns emptyList()
        every { tekstblokkRepository.finnSakstyperForIds(listOf(1L)) } returns listOf(arrayOf(1L, Sakstyper.FTRL))
        every { tekstblokkRepository.finnBehandlingstemaerForIds(listOf(1L)) } returns
            listOf(arrayOf(1L, Behandlingstema.YRKESAKTIV))

        service.hentAlleOversikter(null)

        oversikt.sakstyper shouldContainExactly setOf(Sakstyper.FTRL)
        oversikt.behandlingstemaer shouldContainExactly setOf(Behandlingstema.YRKESAKTIV)
    }

    @Test
    fun `bulk uten avgrensningsfelter lagrer blokker uten avgrensning`() {
        service.opprettBulk(listOf(TekstblokkService.Input("Tittel", "<p>Innhold</p>", TekstblokkType.TEKSTBLOKK, null)))

        lagret.captured.sakstyper.shouldBeEmpty()
        lagret.captured.behandlingstemaer.shouldBeEmpty()
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

    @Test
    fun `slaar opp navn kun en gang for hele bulken`() {
        val input = TekstblokkService.Input("Tittel", "<p>Innhold</p>", TekstblokkType.TEKSTBLOKK, null)

        service.opprettBulk(List(20) { input })

        verify(exactly = 1) { saksbehandlerService.finnNavnForIdent(IDENT) }
    }

    @Test
    fun `sletting markerer raden i stedet for aa fjerne den`() {
        val tekstblokk = Tekstblokk(id = 1)
        every { tekstblokkRepository.findByIdAndSlettetDatoIsNull(1) } returns Optional.of(tekstblokk)

        service.slett(1)

        lagret.captured.slettetDato.shouldNotBeNull()
        verify(exactly = 0) { tekstblokkRepository.delete(any()) }
    }

    @Test
    fun `sletting av en allerede slettet blokk gir IkkeFunnet`() {
        every { tekstblokkRepository.findByIdAndSlettetDatoIsNull(1) } returns Optional.empty()

        shouldThrow<IkkeFunnetException> { service.slett(1) }
    }

    @Test
    fun `oppdatering av en slettet blokk gir IkkeFunnet`() {
        every { tekstblokkRepository.findByIdAndSlettetDatoIsNull(1) } returns Optional.empty()

        shouldThrow<IkkeFunnetException> {
            service.oppdater(1, TekstblokkService.Input("Tittel", "<p>Tekst</p>", TekstblokkType.TEKSTBLOKK, null))
        }
    }

    private companion object {
        const val IDENT = "A146170"
        const val NAVN = "Margareth Bjørgum"
        val NÅ: Instant = Instant.parse("2026-07-31T10:00:00Z")
    }
}
