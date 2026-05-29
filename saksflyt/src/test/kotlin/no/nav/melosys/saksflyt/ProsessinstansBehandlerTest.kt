package no.nav.melosys.saksflyt

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.UtførendeProsessKontekst
import no.nav.melosys.saksflytapi.domain.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.ApplicationEventPublisher
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockKExtension::class)
class ProsessinstansBehandlerTest {

    @MockK
    private lateinit var prosessinstansRepository: ProsessinstansRepository

    @MockK
    private lateinit var stegbehandler: StegBehandler

    @MockK
    private lateinit var applicationEventPublisher: ApplicationEventPublisher

    @MockK(relaxed = true)
    private lateinit var meterRegistry: MeterRegistry

    private lateinit var prosessinstansBehandler: ProsessinstansBehandler

    private val prosessinstans = Prosessinstans.forTest {
        type = ProsessType.MOTTAK_SED
        status = ProsessStatus.KLAR
    }

    @BeforeEach
    fun setup() {
        every { stegbehandler.inngangsSteg() } returns ProsessSteg.SED_MOTTAK_RUTING
        every { stegbehandler.utfør(any()) } just Runs
        every { prosessinstansRepository.save(any<Prosessinstans>()) } returnsArgument 0
        every { applicationEventPublisher.publishEvent(any()) } just Runs
        prosessinstansBehandler = ProsessinstansBehandler(
            setOf(stegbehandler),
            prosessinstansRepository,
            applicationEventPublisher,
            meterRegistry,
            Duration.ofHours(2)
        )

        prosessinstans.type = ProsessType.MOTTAK_SED
        prosessinstans.status = ProsessStatus.KLAR
    }

    @Test
    fun `behandleProsessinstansNå ny prosessinstans steg null blir behandlet`() {
        every { prosessinstansRepository.save(any<Prosessinstans>()) } returnsArgument 0


        prosessinstansBehandler.behandleProsessinstansNå(prosessinstans)


        prosessinstans.run {
            sistFullførtSteg shouldBe ProsessSteg.SED_MOTTAK_RUTING
            status shouldBe ProsessStatus.FERDIG
        }
        verify { stegbehandler.utfør(prosessinstans) }
        verify(exactly = 3) { prosessinstansRepository.save(prosessinstans) }
    }

    @Test
    fun `behandleProsessinstansNå ny prosessinstans steg null stegbehandler kaster feil status feilet blir lagret med hendelse`() {
        every { prosessinstansRepository.save(any<Prosessinstans>()) } returnsArgument 0
        every { stegbehandler.utfør(prosessinstans) } throws FunksjonellException("FEIL!")


        prosessinstansBehandler.behandleProsessinstansNå(prosessinstans)


        prosessinstans.run {
            sistFullførtSteg.shouldBeNull()
            status shouldBe ProsessStatus.FEILET
            hendelser.shouldHaveSize(1).single().run {
                steg shouldBe ProsessSteg.SED_MOTTAK_RUTING
                prosessinstans shouldBe this@ProsessinstansBehandlerTest.prosessinstans
            }
        }
        verify(exactly = 2) { prosessinstansRepository.save(prosessinstans) }
    }

    @Test
    fun `behandleProsessinstansNå prosessinstans med status feilet blir ikke behandlet`() {
        prosessinstans.status = ProsessStatus.FEILET


        prosessinstansBehandler.behandleProsessinstansNå(prosessinstans)


        verify(exactly = 0) { stegbehandler.utfør(any()) }
        verify(exactly = 0) { prosessinstansRepository.save(any()) }
    }

    @Test
    fun `gjenopprett prosesser som henger ved oppstart - kun de eldre enn gjenopprettelsesvinduet`() {
        val ferskProsessinstans = lagProsessinstans(LocalDateTime.now().minusMinutes(30)) // innenfor 2t-vinduet
        val hengendeProsessinstans = lagProsessinstans(LocalDateTime.MIN)
        every { prosessinstansRepository.findAllByStatusIn(any<Set<ProsessStatus>>()) } returns setOf(ferskProsessinstans, hengendeProsessinstans)


        prosessinstansBehandler.gjenopprettProsesserSomHengerVedOppstart(null)


        verify { prosessinstansRepository.save(hengendeProsessinstans) }
        verify(exactly = 0) { prosessinstansRepository.save(ferskProsessinstans) }
        verify(exactly = 1) { applicationEventPublisher.publishEvent(any()) }
    }

    @Test
    fun `utførSteg eksponerer prosessinstansens prioritet i UtførendeProsessKontekst under steget og rydder opp etterpå`() {
        prosessinstans.prioritet = ProsessPrioritet.HØY // typisk løftet via parent-propagering
        var prioritetUnderSteg: ProsessPrioritet? = null
        every { stegbehandler.utfør(any()) } answers { prioritetUnderSteg = UtførendeProsessKontekst.gjeldendePrioritet() }

        prosessinstansBehandler.behandleProsessinstansNå(prosessinstans)

        prioritetUnderSteg shouldBe ProsessPrioritet.HØY // sub-prosesser opprettet under steget arver denne
        UtførendeProsessKontekst.gjeldendePrioritet().shouldBeNull() // ryddet opp etter steget
    }

    private fun lagProsessinstans(endretDato: LocalDateTime) = Prosessinstans.forTest {
        id = UUID.randomUUID()
        behandling = null
        status = ProsessStatus.FEILET
        type = ProsessType.MOTTAK_SED
        sistFullførtSteg = null
        registrertDato = LocalDateTime.MIN
        this.endretDato = endretDato
    }
}
