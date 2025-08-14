package no.nav.melosys.service

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.repository.SaksopplysningRepository
import no.nav.melosys.domain.forTest
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
    fun `lagrePersonopplysninger skal lagre personopplysninger når ingen saksopplysninger eksisterer`() {
        val personopplysninger = PersonopplysningerObjectFactory.lagPersonopplysninger()
        val behandling = Behandling.forTest { }
        val captor = slot<Saksopplysning>()
        every { saksopplysningRepository.save(capture(captor)) } returns Saksopplysning()


        saksopplysningerService.lagrePersonopplysninger(behandling, personopplysninger)


        verify { saksopplysningRepository.save(any()) }
        captor.captured.type shouldBe SaksopplysningType.PDL_PERSOPL
    }

    @Test
    fun `lagrePersonMedHistorikk skal lagre person med historikk når ingen saksopplysninger eksisterer`() {
        val personMedHistorikk = PersonopplysningerObjectFactory.lagPersonMedHistorikk()
        val behandling = Behandling.forTest { }
        val captor = slot<Saksopplysning>()
        every { saksopplysningRepository.save(capture(captor)) } returns Saksopplysning()


        saksopplysningerService.lagrePersonMedHistorikk(behandling, personMedHistorikk)


        verify { saksopplysningRepository.save(any()) }
        captor.captured.type shouldBe SaksopplysningType.PDL_PERS_SAKS
    }

    @Test
    fun `lagrePersonopplysninger skal lagre PDL personopplysninger og fjerne eksisterende PERSOPL når PERSOPL eksisterer`() {
        val personopplysninger = PersonopplysningerObjectFactory.lagPersonopplysninger()
        val behandling = Behandling.forTest { }
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
    fun `lagrePersonMedHistorikk skal lagre PDL person med sakshistorikk og fjerne eksisterende PERSHIST når PERSHIST eksisterer`() {
        val personMedHistorikk = PersonopplysningerObjectFactory.lagPersonMedHistorikk()
        val behandling = Behandling.forTest { }
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
    fun `finnPdlPersonhistorikkTilSaksbehandler skal returnere Optional empty når PDL_PERS_SAKS ikke eksisterer`() {
        every { saksopplysningRepository.findByBehandling_IdAndType(1L, SaksopplysningType.PDL_PERS_SAKS) } returns Optional.empty()


        val personMedHistorikk = saksopplysningerService.finnPdlPersonhistorikkTilSaksbehandler(1L)


        personMedHistorikk shouldBe Optional.empty()
    }

    @Test
    fun `hentPdlPersonopplysninger skal returnere persondata når PDL personopplysninger eksisterer fra database`() {
        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.PERSOPL
            dokument = PersonopplysningerObjectFactory.lagPersonopplysninger()
        }
        every { saksopplysningRepository.findByBehandling_IdAndType(1L, SaksopplysningType.PDL_PERSOPL) } returns Optional.of(saksopplysning)


        val persondata = saksopplysningerService.hentPdlPersonopplysninger(1L)


        persondata.shouldNotBeNull()
    }
}
