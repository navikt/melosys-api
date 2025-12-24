package no.nav.melosys.saksflyt.steg.saksopplysninger

import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.person.Informasjonsbehov
import no.nav.melosys.domain.person.PersonMedHistorikk
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class LagrePersonopplysningerTest {

    @RelaxedMockK
    lateinit var behandlingService: BehandlingService

    @RelaxedMockK
    lateinit var saksopplysningerService: SaksopplysningerService

    @RelaxedMockK
    lateinit var persondataFasade: PersondataFasade

    @RelaxedMockK
    lateinit var avklartefaktaService: AvklartefaktaService

    private lateinit var lagrePersonopplysninger: LagrePersonopplysninger

    private val behandlingId = 123L
    private val aktørId = "12345678901"
    private val prosessinstans = Prosessinstans.forTest()
    private lateinit var behandling: Behandling

    @BeforeEach
    fun setup() {
        lagrePersonopplysninger = LagrePersonopplysninger(
            behandlingService,
            saksopplysningerService,
            persondataFasade,
            avklartefaktaService
        )

        behandling = Behandling.forTest {
            id = behandlingId
            fagsak = Fagsak.forTest { medBruker() }
        }

        prosessinstans.behandling = behandling
    }

    @Test
    fun `inngangsSteg returnerer LAGRE_PERSONOPPLYSNINGER`() {
        lagrePersonopplysninger.inngangsSteg() shouldBe ProsessSteg.LAGRE_PERSONOPPLYSNINGER
    }

    @Test
    fun `utfør skips when hovedpartRolle is not BRUKER`() {
        val virksomhetBehandling = Behandling.forTest {
            id = behandlingId
            fagsak = Fagsak.forTest { medVirksomhet() }
        }

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns virksomhetBehandling


        lagrePersonopplysninger.utfør(prosessinstans)


        verify { saksopplysningerService wasNot Called }
        verify { persondataFasade wasNot Called }
    }

    @Test
    fun `utfør saves PDL_PERSOPL when missing, without familierelasjoner`() {
        val freshBehandling = mockBehandlingMissingPdlPersopl()
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns freshBehandling

        val persondata = mockk<Persondata>()
        every { persondataFasade.hentPerson(aktørId) } returns persondata

        mockNoMedfølgendeFamilie()


        lagrePersonopplysninger.utfør(prosessinstans)


        verify { saksopplysningerService.lagrePersonopplysninger(freshBehandling, persondata) }
        verify { persondataFasade.hentPerson(aktørId) }
        verify(exactly = 0) { persondataFasade.hentPerson(aktørId, Informasjonsbehov.MED_FAMILIERELASJONER) }
    }

    @Test
    fun `utfør saves PDL_PERSOPL with familierelasjoner when medfølgendeBarn finnes`() {
        val freshBehandling = mockBehandlingMissingPdlPersopl()
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns freshBehandling

        val persondata = mockk<Persondata>()
        every { persondataFasade.hentPerson(aktørId, Informasjonsbehov.MED_FAMILIERELASJONER) } returns persondata

        mockMedfølgendeBarnFinnes()


        lagrePersonopplysninger.utfør(prosessinstans)


        verify { saksopplysningerService.lagrePersonopplysninger(freshBehandling, persondata) }
        verify { persondataFasade.hentPerson(aktørId, Informasjonsbehov.MED_FAMILIERELASJONER) }
    }

    @Test
    fun `utfør saves PDL_PERSOPL with familierelasjoner when medfølgendeEktefelle finnes`() {
        val freshBehandling = mockBehandlingMissingPdlPersopl()
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns freshBehandling

        val persondata = mockk<Persondata>()
        every { persondataFasade.hentPerson(aktørId, Informasjonsbehov.MED_FAMILIERELASJONER) } returns persondata

        mockMedfølgendeEktefelleFinnes()


        lagrePersonopplysninger.utfør(prosessinstans)


        verify { saksopplysningerService.lagrePersonopplysninger(freshBehandling, persondata) }
        verify { persondataFasade.hentPerson(aktørId, Informasjonsbehov.MED_FAMILIERELASJONER) }
    }

    @Test
    fun `utfør saves PDL_PERS_SAKS when missing`() {
        val freshBehandling = mockBehandlingMissingPdlPersSaks()
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns freshBehandling

        val personMedHistorikk = mockk<PersonMedHistorikk>()
        every { persondataFasade.hentPersonMedHistorikk(aktørId) } returns personMedHistorikk


        lagrePersonopplysninger.utfør(prosessinstans)


        verify { saksopplysningerService.lagrePersonMedHistorikk(freshBehandling, personMedHistorikk) }
    }

    @Test
    fun `utfør does not save PDL_PERSOPL when already present`() {
        val freshBehandling = mockBehandlingWithAllSaksopplysninger()
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns freshBehandling


        lagrePersonopplysninger.utfør(prosessinstans)


        verify(exactly = 0) { saksopplysningerService.lagrePersonopplysninger(any(), any()) }
    }

    @Test
    fun `utfør does not save PDL_PERS_SAKS when already present`() {
        val freshBehandling = mockBehandlingWithAllSaksopplysninger()
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns freshBehandling


        lagrePersonopplysninger.utfør(prosessinstans)


        verify(exactly = 0) { saksopplysningerService.lagrePersonMedHistorikk(any(), any()) }
    }

    @Test
    fun `utfør reloads behandling fresh from service`() {
        val freshBehandling = mockBehandlingWithAllSaksopplysninger()
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) } returns freshBehandling


        lagrePersonopplysninger.utfør(prosessinstans)


        verify { behandlingService.hentBehandlingMedSaksopplysninger(behandlingId) }
    }

    // --- Helper methods ---

    private fun mockBehandlingMissingPdlPersopl(): Behandling {
        val behandling = mockk<Behandling>()
        val fagsak = mockk<Fagsak>()

        every { behandling.id } returns behandlingId
        every { behandling.fagsak } returns fagsak
        every { fagsak.hovedpartRolle } returns no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER
        every { fagsak.hentBrukersAktørID() } returns aktørId
        every { behandling.manglerSaksopplysningerAvType(listOf(SaksopplysningType.PDL_PERSOPL)) } returns true
        every { behandling.manglerSaksopplysningerAvType(listOf(SaksopplysningType.PDL_PERS_SAKS)) } returns false

        return behandling
    }

    private fun mockBehandlingMissingPdlPersSaks(): Behandling {
        val behandling = mockk<Behandling>()
        val fagsak = mockk<Fagsak>()

        every { behandling.id } returns behandlingId
        every { behandling.fagsak } returns fagsak
        every { fagsak.hovedpartRolle } returns no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER
        every { fagsak.hentBrukersAktørID() } returns aktørId
        every { behandling.manglerSaksopplysningerAvType(listOf(SaksopplysningType.PDL_PERSOPL)) } returns false
        every { behandling.manglerSaksopplysningerAvType(listOf(SaksopplysningType.PDL_PERS_SAKS)) } returns true

        return behandling
    }

    private fun mockBehandlingWithAllSaksopplysninger(): Behandling {
        val behandling = mockk<Behandling>()
        val fagsak = mockk<Fagsak>()

        every { behandling.id } returns behandlingId
        every { behandling.fagsak } returns fagsak
        every { fagsak.hovedpartRolle } returns no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER
        every { behandling.manglerSaksopplysningerAvType(listOf(SaksopplysningType.PDL_PERSOPL)) } returns false
        every { behandling.manglerSaksopplysningerAvType(listOf(SaksopplysningType.PDL_PERS_SAKS)) } returns false

        return behandling
    }

    private fun mockNoMedfølgendeFamilie() {
        val noFamily = mockk<AvklarteMedfolgendeFamilie>()
        every { noFamily.finnes() } returns false
        every { avklartefaktaService.hentAvklarteMedfølgendeBarn(behandlingId) } returns noFamily
        every { avklartefaktaService.hentAvklarteMedfølgendeEktefelle(behandlingId) } returns noFamily
    }

    private fun mockMedfølgendeBarnFinnes() {
        val withBarn = mockk<AvklarteMedfolgendeFamilie>()
        every { withBarn.finnes() } returns true
        every { avklartefaktaService.hentAvklarteMedfølgendeBarn(behandlingId) } returns withBarn

        val noEktefelle = mockk<AvklarteMedfolgendeFamilie>()
        every { noEktefelle.finnes() } returns false
        every { avklartefaktaService.hentAvklarteMedfølgendeEktefelle(behandlingId) } returns noEktefelle
    }

    private fun mockMedfølgendeEktefelleFinnes() {
        val noBarn = mockk<AvklarteMedfolgendeFamilie>()
        every { noBarn.finnes() } returns false
        every { avklartefaktaService.hentAvklarteMedfølgendeBarn(behandlingId) } returns noBarn

        val withEktefelle = mockk<AvklarteMedfolgendeFamilie>()
        every { withEktefelle.finnes() } returns true
        every { avklartefaktaService.hentAvklarteMedfølgendeEktefelle(behandlingId) } returns withEktefelle
    }
}
