package no.nav.melosys.service

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.repository.SaksopplysningRepository
import no.nav.melosys.service.SaksbehandlingDataFactory.lagBehandling
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class SaksopplysningerServiceKtTest {

    @MockK
    private lateinit var saksopplysningRepository: SaksopplysningRepository

    private lateinit var saksopplysningerService: SaksopplysningerService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        saksopplysningerService = SaksopplysningerService(saksopplysningRepository)
    }

    @Test
    fun lagrePersonopplysninger_ingenSaksopplysninger_ok() {
        val personopplysninger = PersonopplysningerObjectFactory.lagPersonopplysninger()
        val behandling = lagBehandling()
        val captor = slot<Saksopplysning>()
        every { saksopplysningRepository.save(capture(captor)) } returns Saksopplysning()

        saksopplysningerService.lagrePersonopplysninger(behandling, personopplysninger)

        verify { saksopplysningRepository.save(any()) }
        captor.captured.type shouldBe SaksopplysningType.PDL_PERSOPL
    }

    @Test
    fun lagrePersonMedHistorikk_ingenSaksopplysninger_ok() {
        val personMedHistorikk = PersonopplysningerObjectFactory.lagPersonMedHistorikk()
        val behandling = lagBehandling()
        val captor = slot<Saksopplysning>()
        every { saksopplysningRepository.save(capture(captor)) } returns Saksopplysning()

        saksopplysningerService.lagrePersonMedHistorikk(behandling, personMedHistorikk)

        verify { saksopplysningRepository.save(any()) }
        captor.captured.type shouldBe SaksopplysningType.PDL_PERS_SAKS
    }

    @Test
    fun lagrePersonopplysninger_PERSOPLeksisterer_lagresPERSOPLfjernes() {
        val personopplysninger = PersonopplysningerObjectFactory.lagPersonopplysninger()
        val behandling = lagBehandling()
        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.PERSOPL
        }
        behandling.saksopplysninger.add(saksopplysning)
        val captor = slot<Saksopplysning>()
        every { saksopplysningRepository.save(capture(captor)) } returns Saksopplysning()

        saksopplysningerService.lagrePersonopplysninger(behandling, personopplysninger)

        verify { saksopplysningRepository.save(any()) }
        captor.captured.type shouldBe SaksopplysningType.PDL_PERSOPL
        behandling.saksopplysninger.shouldBeEmpty()
    }

    @Test
    fun lagrePersonMedHistorikk_PERSHISTeksisterer_lagresPERSHISTfjernes() {
        val personMedHistorikk = PersonopplysningerObjectFactory.lagPersonMedHistorikk()
        val behandling = lagBehandling()
        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.PERSHIST
        }
        behandling.saksopplysninger.add(saksopplysning)
        val captor = slot<Saksopplysning>()
        every { saksopplysningRepository.save(capture(captor)) } returns Saksopplysning()

        saksopplysningerService.lagrePersonMedHistorikk(behandling, personMedHistorikk)

        verify { saksopplysningRepository.save(any()) }
        captor.captured.type shouldBe SaksopplysningType.PDL_PERS_SAKS
        behandling.saksopplysninger.shouldBeEmpty()
    }

    @Test
    fun hentPersonhistorikk_PDL_PERS_SAKSeksistererIkke_optionalEmpty() {
        every { saksopplysningRepository.findByBehandling_IdAndType(1L, SaksopplysningType.PDL_PERS_SAKS) } returns Optional.empty()

        val personMedHistorikk = saksopplysningerService.finnPdlPersonhistorikkTilSaksbehandler(1L)

        personMedHistorikk shouldBe Optional.empty()
    }

    @Test
    fun hentPDLpersonopplysninger_eksistererFraDb_returneres() {
        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.PERSOPL
            dokument = PersonopplysningerObjectFactory.lagPersonopplysninger()
        }
        every { saksopplysningRepository.findByBehandling_IdAndType(1L, SaksopplysningType.PDL_PERSOPL) } returns Optional.of(saksopplysning)

        val persondata = saksopplysningerService.hentPdlPersonopplysninger(1L)

        persondata.shouldNotBeNull()
    }
}
