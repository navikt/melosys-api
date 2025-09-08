package no.nav.melosys.saksflyt.kontroll

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.fagsak
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflyt.ProsessinstansBehandlerDelegate
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockKExtension::class)
class ProsessinstansAdminServiceTest {

    @MockK
    private lateinit var prosessinstansRepository: ProsessinstansRepository

    @MockK
    private lateinit var prosessinstansBehandlerDelegate: ProsessinstansBehandlerDelegate

    private lateinit var prosessinstansAdminService: ProsessinstansAdminService

    @BeforeEach
    fun setup() {
        every { prosessinstansBehandlerDelegate.behandleProsessinstans(any()) } just Runs
        every { prosessinstansRepository.save(any<Prosessinstans>()) } returnsArgument 0
        every { prosessinstansRepository.saveAll(any<List<Prosessinstans>>()) } returnsArgument 0
        prosessinstansAdminService = ProsessinstansAdminService(prosessinstansBehandlerDelegate, prosessinstansRepository)
    }

    @Test
    fun `hentFeiledeProsessinstanser en prosessinstans flere hendelser viser feilmelding siste hendelse`() {
        val sisteFeilmelding = "siste feilmelding"
        val prosessinstans = lagProsessinstans().apply {
            hendelser.add(ProsessinstansHendelse(this, LocalDateTime.now().minusDays(1), null, null, "første"))
            hendelser.add(ProsessinstansHendelse(this, LocalDateTime.now(), null, null, sisteFeilmelding))
        }

        every { prosessinstansRepository.findAllByStatus(ProsessStatus.FEILET) } returns listOf(prosessinstans)


        val prosessinstanser = prosessinstansAdminService.hentFeiledeProsessinstanser()


        prosessinstanser.map { dto ->
            listOf(
                dto.id(),
                dto.behandlingId(),
                dto.saksnummer(),
                dto.endretDato(),
                dto.registrertDato(),
                dto.prosessType(),
                dto.feiletSteg(),
                dto.sisteFeilmelding(),
                dto.correlationId()
            )
        }.flatten().shouldContainExactly(
            prosessinstans.id,
            prosessinstans.hentBehandling.id,
            prosessinstans.hentBehandling.fagsak.saksnummer,
            prosessinstans.endretDato,
            prosessinstans.registrertDato,
            prosessinstans.type.kode,
            CURRENT_PROSESS_STEG.kode,
            sisteFeilmelding,
            prosessinstans.getData(ProsessDataKey.CORRELATION_ID_SAKSFLYT)
        )
    }

    @Test
    fun `hentFeiletSteg forrige er null gir første steg`() {
        val prosessinstans = lagProsessinstans {
            sistFullførtSteg = null
        }

        every { prosessinstansRepository.findAllByStatus(ProsessStatus.FEILET) } returns listOf(prosessinstans)


        val prosessinstanser = prosessinstansAdminService.hentFeiledeProsessinstanser()


        prosessinstanser
            .shouldHaveSize(1)
            .single()
            .feiletSteg() shouldBe FORSTE_PROSSESS_STEG.kode
    }

    @Test
    fun `restartAlleFeiledeProsessinstanser tre feilet restarter i rekkefølge`() {
        val tidligstFeilet = lagProsessinstans {
            registrertDato = LocalDateTime.now().minusDays(3)
        }
        val nestTidligstFeilet = lagProsessinstans {
            registrertDato = LocalDateTime.now().minusDays(2)
        }
        val senestFeilet = lagProsessinstans {
            registrertDato = LocalDateTime.now()
        }


        every { prosessinstansRepository.findAllByStatus(ProsessStatus.FEILET) } returns setOf(tidligstFeilet, nestTidligstFeilet, senestFeilet)
        every { prosessinstansRepository.saveAll(any<Collection<Prosessinstans>>()) } answers { firstArg<Collection<Prosessinstans>>().toList() }


        prosessinstansAdminService.restartAlleFeiledeProsessinstanser()


        verifyOrder {
            prosessinstansBehandlerDelegate.behandleProsessinstans(tidligstFeilet)
            prosessinstansBehandlerDelegate.behandleProsessinstans(nestTidligstFeilet)
            prosessinstansBehandlerDelegate.behandleProsessinstans(senestFeilet)
        }
    }

    @Test
    fun `restartProsessinstans prosessinstans har status ferdig kan ikke gjenstartes`() {
        val prosessinstans = lagProsessinstans {
            status = ProsessStatus.FERDIG
        }
        val uuid = prosessinstans.id!!

        every { prosessinstansRepository.findAllById(any<List<UUID>>()) } returns listOf(prosessinstans)


        shouldThrow<FunksjonellException> {
            prosessinstansAdminService.restartProsessinstanser(listOf(uuid))
        }.message shouldContain "har status"
    }

    @Test
    fun `restartProsessinstans prosessinstans er ny og har status klar kaster feil`() {
        val prosessinstans = lagProsessinstans {
            status = ProsessStatus.KLAR
        }
        val uuid = prosessinstans.id!!

        every { prosessinstansRepository.findAllById(any<List<UUID>>()) } returns listOf(prosessinstans)


        shouldThrow<FunksjonellException> {
            prosessinstansAdminService.restartProsessinstanser(listOf(uuid))
        }.message shouldContain "for mindre enn"
    }

    @Test
    fun `restartProsessinstans prosessinstans er gammel og har status klar blir restartet`() {
        val prosessinstans = lagProsessinstans {
            registrertDato = LocalDateTime.now().minusDays(2)
            status = ProsessStatus.KLAR
        }
        every { prosessinstansRepository.findAllById(setOf(prosessinstans.id)) } returns listOf(prosessinstans)


        prosessinstansAdminService.restartProsessinstanser(setOf(prosessinstans.id))


        prosessinstans.status shouldBe ProsessStatus.RESTARTET
        verify { prosessinstansRepository.saveAll(listOf(prosessinstans)) }
        verify { prosessinstansBehandlerDelegate.behandleProsessinstans(prosessinstans) }
    }

    @Test
    fun `restartProsessinstans prosessinstans har status feilet blir restartet`() {
        val prosessinstans = lagProsessinstans()
        val uuid = prosessinstans.id!!

        every { prosessinstansRepository.findAllById(any<List<UUID>>()) } returns listOf(prosessinstans)
        every { prosessinstansRepository.saveAll(any<List<Prosessinstans>>()) } returnsArgument 0


        prosessinstansAdminService.restartProsessinstanser(listOf(uuid))


        prosessinstans.status shouldBe ProsessStatus.RESTARTET
        verify { prosessinstansRepository.saveAll(listOf(prosessinstans)) }
        verify { prosessinstansBehandlerDelegate.behandleProsessinstans(prosessinstans) }
    }

    @Test
    fun `hoppOverStegProsessinstans hopper til neste steg`() {
        val prosessinstans = lagProsessinstans()
        val uuid = prosessinstans.id!!

        every { prosessinstansRepository.findById(uuid) } returns Optional.of(prosessinstans)


        prosessinstansAdminService.hoppOverStegProsessinstans(uuid)


        prosessinstans.sistFullførtSteg shouldBe CURRENT_PROSESS_STEG
        verify { prosessinstansRepository.save(prosessinstans) }
    }

    private fun lagProsessinstans(init: Prosessinstans.Builder.() -> Unit = {}) = Prosessinstans.forTest {
        type = PROSESS_TYPE
        status = ProsessStatus.FEILET
        behandling {
            id = 1L
            fagsak {
                medBruker()
            }
        }
        sistFullførtSteg = FORRIGE_PROSSESS_STEG
        medData(ProsessDataKey.CORRELATION_ID_SAKSFLYT, "correlation-id")
        init()
    }

    companion object {
        // Viktig at forrige og current er steg som kommer rett etter hverandre i samme prosess(type)
        private val PROSESS_TYPE = ProsessType.JFR_NY_SAK_BRUKER
        private val FORSTE_PROSSESS_STEG = ProsessSteg.OPPRETT_SAK_OG_BEH
        private val FORRIGE_PROSSESS_STEG = ProsessSteg.OPPRETT_MOTTATTEOPPLYSNINGER
        private val CURRENT_PROSESS_STEG = ProsessSteg.OPPRETT_ARKIVSAK
    }
}
