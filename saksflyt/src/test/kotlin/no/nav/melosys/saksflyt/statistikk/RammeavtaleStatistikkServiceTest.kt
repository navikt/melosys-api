package no.nav.melosys.saksflyt.statistikk

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class RammeavtaleStatistikkServiceTest {

    @MockK
    private lateinit var rammeavtaleStatistikkRepository: RammeavtaleStatistikkRepository

    private lateinit var service: RammeavtaleStatistikkService

    @BeforeEach
    fun setup() {
        service = RammeavtaleStatistikkService(rammeavtaleStatistikkRepository)
    }

    @Test
    fun `summerer per vedtaksaar og bygger riktig prosesstype, data-monster og resultattype`() {
        val prosessTypeSlot = slot<String>()
        val dataLikePatternSlot = slot<String>()
        val resultatTypeSlot = slot<String>()
        every {
            rammeavtaleStatistikkRepository.finnFerdigbehandledeMedDataLike(
                capture(prosessTypeSlot),
                capture(dataLikePatternSlot),
                capture(resultatTypeSlot),
                any(),
                any(),
            )
        } returns listOf(
            rad("MEL-1", 1, "2024-02-01"),
            rad("MEL-2", 2, "2024-11-30"),
            rad("MEL-3", 3, "2025-01-02"),
        )

        val statistikk = service.hentRammeavtaleFjernarbeidStatistikk(null, null)

        statistikk.antall shouldBe 3
        statistikk.antallPerVedtaksaar shouldBe mapOf("2024" to 2L, "2025" to 1L)
        prosessTypeSlot.captured shouldBe ProsessType.ANMODNING_OM_UNNTAK.kode
        dataLikePatternSlot.captured shouldBe "%${ProsessDataKey.ER_FJERNARBEID_TWFA.kode}=true%"
        resultatTypeSlot.captured shouldBe Behandlingsresultattyper.FASTSATT_LOVVALGSLAND.name
    }

    @Test
    fun `sorterer aarene stigende uavhengig av radrekkefolgen fra spoerringen`() {
        every {
            rammeavtaleStatistikkRepository.finnFerdigbehandledeMedDataLike(any(), any(), any(), any(), any())
        } returns listOf(
            rad("MEL-1", 1, "2026-01-01"),
            rad("MEL-2", 2, "2024-01-01"),
            rad("MEL-3", 3, "2025-01-01"),
        )

        val statistikk = service.hentRammeavtaleFjernarbeidStatistikk(null, null)

        // shouldBe på Map er rekkefølgeuavhengig, så rekkefølgen må sjekkes på nøklene
        statistikk.antallPerVedtaksaar.keys.toList() shouldBe listOf("2024", "2025", "2026")
    }

    @Test
    fun `tar med saksnummer og vedtaksdato per behandling som standard`() {
        every {
            rammeavtaleStatistikkRepository.finnFerdigbehandledeMedDataLike(any(), any(), any(), any(), any())
        } returns listOf(
            rad("MEL-1", 1, "2025-03-04"),
            rad("MEL-2", 2, "2025-06-01"),
        )

        val statistikk = service.hentRammeavtaleFjernarbeidStatistikk(null, null)

        statistikk.saker shouldBe listOf(
            RammeavtaleSak("MEL-1", "2025", LocalDate.of(2025, 3, 4)),
            RammeavtaleSak("MEL-2", "2025", LocalDate.of(2025, 6, 1)),
        )
    }

    @Test
    fun `samme sak med to behandlinger telles to ganger og listes to ganger`() {
        every {
            rammeavtaleStatistikkRepository.finnFerdigbehandledeMedDataLike(any(), any(), any(), any(), any())
        } returns listOf(
            rad("MEL-1", 1, "2025-03-04"),
            rad("MEL-1", 2, "2025-09-09"),
        )

        val statistikk = service.hentRammeavtaleFjernarbeidStatistikk(null, null)

        statistikk.antall shouldBe 2
        statistikk.antallPerVedtaksaar shouldBe mapOf("2025" to 2L)
        statistikk.saker!!.map { it.saksnummer } shouldBe listOf("MEL-1", "MEL-1")
    }

    @Test
    fun `utelater saksnummerlisten men beholder tallene naar inkluderSaksnummer er false`() {
        every {
            rammeavtaleStatistikkRepository.finnFerdigbehandledeMedDataLike(any(), any(), any(), any(), any())
        } returns listOf(
            rad("MEL-1", 1, "2025-03-04"),
            rad("MEL-2", 2, "2025-06-01"),
        )

        val statistikk = service.hentRammeavtaleFjernarbeidStatistikk(null, null, inkluderSaksnummer = false)

        statistikk.saker.shouldBeNull()
        statistikk.antall shouldBe 2
        statistikk.antallPerVedtaksaar shouldBe mapOf("2025" to 2L)
    }

    @Test
    fun `feiler heller enn aa slippe en rad i stillhet`() {
        // Slike rader er umulige gitt WHERE-klausulen og NOT NULL på behandling.saksnummer, men skulle
        // spørringen eller skjemaet endres skal vi ikke underrapportere et tall brukt i offisiell rapportering
        listOf(radMedNull("MEL-2", null), radMedNull(null, "2025-01-01")).forEach { ugyldigRad ->
            every {
                rammeavtaleStatistikkRepository.finnFerdigbehandledeMedDataLike(any(), any(), any(), any(), any())
            } returns listOf(rad("MEL-1", 1, "2025-01-01"), ugyldigRad)

            shouldThrow<IllegalStateException> {
                service.hentRammeavtaleFjernarbeidStatistikk(null, null)
            }
        }
    }

    @Test
    fun `oversetter fom og inklusiv tom til tidspunkt`() {
        val fomSlot = slot<LocalDateTime?>()
        val tomSlot = slot<LocalDateTime?>()
        every {
            rammeavtaleStatistikkRepository.finnFerdigbehandledeMedDataLike(
                any(),
                any(),
                any(),
                captureNullable(fomSlot),
                captureNullable(tomSlot),
            )
        } returns emptyList()

        val statistikk = service.hentRammeavtaleFjernarbeidStatistikk(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 12, 31),
        )

        statistikk.antall shouldBe 0
        statistikk.antallPerVedtaksaar.shouldBeEmpty()
        statistikk.saker shouldBe emptyList()
        fomSlot.captured shouldBe LocalDate.of(2024, 1, 1).atStartOfDay()
        // tom er inklusiv -> oversettes til starten av neste dag (eksklusiv øvre grense)
        tomSlot.captured shouldBe LocalDate.of(2025, 1, 1).atStartOfDay()
    }

    /** Rad slik spørringen leverer den: `[saksnummer, behandlingId, vedtaksdato som ISO-tekst]`. */
    private fun rad(saksnummer: String, behandlingId: Long, vedtaksdato: String): Array<Any> =
        arrayOf(saksnummer, BigDecimal(behandlingId), vedtaksdato)

    /** Rad der en kolonne mangler — Hibernate leverer da null i posisjonen. */
    @Suppress("UNCHECKED_CAST")
    private fun radMedNull(saksnummer: String?, vedtaksdato: String?): Array<Any> =
        arrayOf<Any?>(saksnummer, BigDecimal.ONE, vedtaksdato) as Array<Any>
}
