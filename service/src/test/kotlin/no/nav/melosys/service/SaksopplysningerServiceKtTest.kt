package no.nav.melosys.service

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.repository.SaksopplysningRepository
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class SaksopplysningerServiceKtTest {

    @RelaxedMockK
    lateinit var saksopplysningRepository: SaksopplysningRepository

    private lateinit var saksopplysningerService: SaksopplysningerService

    @BeforeEach
    fun setUp() {
        saksopplysningerService = SaksopplysningerService(saksopplysningRepository)
    }

    @Test
    fun `lagre personopplysninger ingen saksopplysninger ok`() {
        val personopplysninger = PersonopplysningerObjectFactory.lagPersonopplysninger()
        val behandling = SaksbehandlingDataFactory.lagBehandling()
        every { saksopplysningRepository.save(any<Saksopplysning>()) } answers { firstArg<Saksopplysning>() }

        saksopplysningerService.lagrePersonopplysninger(behandling, personopplysninger)

        verify { saksopplysningRepository.save(any<Saksopplysning>()) }
    }

    @Test
    fun `lagre person med historikk ingen saksopplysninger ok`() {
        val personMedHistorikk = PersonopplysningerObjectFactory.lagPersonMedHistorikk()
        val behandling = SaksbehandlingDataFactory.lagBehandling()
        every { saksopplysningRepository.save(any<Saksopplysning>()) } answers { firstArg<Saksopplysning>() }

        saksopplysningerService.lagrePersonMedHistorikk(behandling, personMedHistorikk)

        verify { saksopplysningRepository.save(any<Saksopplysning>()) }
    }

    @Test
    fun `lagre personopplysninger PERSOPL eksisterer lagres PERSOPL fjernes`() {
        val personopplysninger = PersonopplysningerObjectFactory.lagPersonopplysninger()
        val behandling = SaksbehandlingDataFactory.lagBehandling()
        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.PERSOPL
        }
        behandling.saksopplysninger.add(saksopplysning)
        every { saksopplysningRepository.save(any<Saksopplysning>()) } answers { firstArg<Saksopplysning>() }

        saksopplysningerService.lagrePersonopplysninger(behandling, personopplysninger)

        verify { saksopplysningRepository.save(any<Saksopplysning>()) }
        behandling.saksopplysninger.shouldBeEmpty()
    }

    @Test
    fun `lagre person med historikk PERSHIST eksisterer lagres PERSHIST fjernes`() {
        val personMedHistorikk = PersonopplysningerObjectFactory.lagPersonMedHistorikk()
        val behandling = SaksbehandlingDataFactory.lagBehandling()
        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.PERSHIST
        }
        behandling.saksopplysninger.add(saksopplysning)
        every { saksopplysningRepository.save(any<Saksopplysning>()) } answers { firstArg<Saksopplysning>() }

        saksopplysningerService.lagrePersonMedHistorikk(behandling, personMedHistorikk)

        verify { saksopplysningRepository.save(any<Saksopplysning>()) }
        behandling.saksopplysninger.shouldBeEmpty()
    }

    @Test
    fun `hent personhistorikk PDL PERS SAKS eksisterer ikke optional empty`() {
        every { saksopplysningRepository.findByBehandling_IdAndType(1L, SaksopplysningType.PDL_PERS_SAKS) } returns Optional.empty()

        val personMedHistorikk = saksopplysningerService.finnPdlPersonhistorikkTilSaksbehandler(1L)

        personMedHistorikk.isPresent shouldBe false
    }

    @Test
    fun `hent PDL personopplysninger eksisterer fra db returneres`() {
        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.PERSOPL
            dokument = PersonopplysningerObjectFactory.lagPersonopplysninger()
        }
        every { saksopplysningRepository.findByBehandling_IdAndType(1L, SaksopplysningType.PDL_PERSOPL) } returns Optional.of(saksopplysning)

        val persondata = saksopplysningerService.hentPdlPersonopplysninger(1L)

        persondata.shouldNotBeNull()
    }
}
