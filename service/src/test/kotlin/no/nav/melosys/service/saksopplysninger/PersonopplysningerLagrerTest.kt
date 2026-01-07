package no.nav.melosys.service.saksopplysninger

import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.person.Informasjonsbehov
import no.nav.melosys.domain.person.PersonMedHistorikk
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.persondata.PersondataFasade
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class PersonopplysningerLagrerTest {

    @RelaxedMockK
    lateinit var saksopplysningerService: SaksopplysningerService

    @MockK
    lateinit var persondataFasade: PersondataFasade

    @MockK
    lateinit var avklartefaktaService: AvklartefaktaService

    private lateinit var personopplysningerLagrer: PersonopplysningerLagrer

    private val behandlingId = 123L
    private val aktørId = "12345678901"

    @BeforeEach
    fun setup() {
        personopplysningerLagrer = PersonopplysningerLagrer(
            saksopplysningerService,
            persondataFasade,
            avklartefaktaService
        )
    }

    @Nested
    inner class `Når hovedpart ikke er BRUKER` {
        @Test
        fun `returnerer false og lagrer ingenting`() {
            val behandling = Behandling.forTest {
                id = behandlingId
                fagsak = Fagsak.forTest { medVirksomhet() }
            }

            val result = personopplysningerLagrer.lagreHvisMangler(behandling)

            result shouldBe false
            verify { saksopplysningerService wasNot Called }
            verify { persondataFasade wasNot Called }
        }
    }

    @Nested
    inner class `Når hovedpart er BRUKER` {

        @Test
        fun `lagrer PDL_PERSOPL uten familierelasjoner når det mangler og ingen medfølgende familie`() {
            val behandling = mockBehandlingMissingPdlPersopl()
            val persondata = mockk<Persondata>()
            every { persondataFasade.hentPerson(aktørId) } returns persondata
            mockNoMedfølgendeFamilie()

            val result = personopplysningerLagrer.lagreHvisMangler(behandling)

            result shouldBe true
            verify { saksopplysningerService.lagrePersonopplysninger(behandling, persondata) }
            verify { persondataFasade.hentPerson(aktørId) }
            verify(exactly = 0) { persondataFasade.hentPerson(aktørId, Informasjonsbehov.MED_FAMILIERELASJONER) }
        }

        @Test
        fun `lagrer PDL_PERSOPL med familierelasjoner når medfølgende barn finnes`() {
            val behandling = mockBehandlingMissingPdlPersopl()
            val persondata = mockk<Persondata>()
            every { persondataFasade.hentPerson(aktørId, Informasjonsbehov.MED_FAMILIERELASJONER) } returns persondata
            mockMedfølgendeBarnFinnes()

            val result = personopplysningerLagrer.lagreHvisMangler(behandling)

            result shouldBe true
            verify { saksopplysningerService.lagrePersonopplysninger(behandling, persondata) }
            verify { persondataFasade.hentPerson(aktørId, Informasjonsbehov.MED_FAMILIERELASJONER) }
        }

        @Test
        fun `lagrer PDL_PERSOPL med familierelasjoner når medfølgende ektefelle finnes`() {
            val behandling = mockBehandlingMissingPdlPersopl()
            val persondata = mockk<Persondata>()
            every { persondataFasade.hentPerson(aktørId, Informasjonsbehov.MED_FAMILIERELASJONER) } returns persondata
            mockMedfølgendeEktefelleFinnes()

            val result = personopplysningerLagrer.lagreHvisMangler(behandling)

            result shouldBe true
            verify { saksopplysningerService.lagrePersonopplysninger(behandling, persondata) }
            verify { persondataFasade.hentPerson(aktørId, Informasjonsbehov.MED_FAMILIERELASJONER) }
        }

        @Test
        fun `lagrer PDL_PERS_SAKS når det mangler`() {
            val behandling = mockBehandlingMissingPdlPersSaks()
            val personMedHistorikk = mockk<PersonMedHistorikk>()
            every { persondataFasade.hentPersonMedHistorikk(aktørId) } returns personMedHistorikk

            val result = personopplysningerLagrer.lagreHvisMangler(behandling)

            result shouldBe true
            verify { saksopplysningerService.lagrePersonMedHistorikk(behandling, personMedHistorikk) }
        }

        @Test
        fun `lagrer ikke PDL_PERSOPL når det allerede finnes`() {
            val behandling = mockBehandlingWithAllSaksopplysninger()

            personopplysningerLagrer.lagreHvisMangler(behandling)

            verify(exactly = 0) { saksopplysningerService.lagrePersonopplysninger(any(), any()) }
        }

        @Test
        fun `lagrer ikke PDL_PERS_SAKS når det allerede finnes`() {
            val behandling = mockBehandlingWithAllSaksopplysninger()

            personopplysningerLagrer.lagreHvisMangler(behandling)

            verify(exactly = 0) { saksopplysningerService.lagrePersonMedHistorikk(any(), any()) }
        }

        @Test
        fun `returnerer false når ingenting ble lagret`() {
            val behandling = mockBehandlingWithAllSaksopplysninger()

            val result = personopplysningerLagrer.lagreHvisMangler(behandling)

            result shouldBe false
        }

        @Test
        fun `lagrer begge saksopplysningstyper når begge mangler`() {
            val behandling = mockBehandlingMissingBoth()
            val persondata = mockk<Persondata>()
            val personMedHistorikk = mockk<PersonMedHistorikk>()
            every { persondataFasade.hentPerson(aktørId) } returns persondata
            every { persondataFasade.hentPersonMedHistorikk(aktørId) } returns personMedHistorikk
            mockNoMedfølgendeFamilie()

            val result = personopplysningerLagrer.lagreHvisMangler(behandling)

            result shouldBe true
            verify { saksopplysningerService.lagrePersonopplysninger(behandling, persondata) }
            verify { saksopplysningerService.lagrePersonMedHistorikk(behandling, personMedHistorikk) }
        }
    }

    // --- Helper methods ---

    private fun mockBehandlingMissingPdlPersopl(): Behandling {
        val behandling = mockk<Behandling>()
        val fagsak = mockk<Fagsak>()

        every { behandling.id } returns behandlingId
        every { behandling.fagsak } returns fagsak
        every { fagsak.hovedpartRolle } returns Aktoersroller.BRUKER
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
        every { fagsak.hovedpartRolle } returns Aktoersroller.BRUKER
        every { fagsak.hentBrukersAktørID() } returns aktørId
        every { behandling.manglerSaksopplysningerAvType(listOf(SaksopplysningType.PDL_PERSOPL)) } returns false
        every { behandling.manglerSaksopplysningerAvType(listOf(SaksopplysningType.PDL_PERS_SAKS)) } returns true

        return behandling
    }

    private fun mockBehandlingMissingBoth(): Behandling {
        val behandling = mockk<Behandling>()
        val fagsak = mockk<Fagsak>()

        every { behandling.id } returns behandlingId
        every { behandling.fagsak } returns fagsak
        every { fagsak.hovedpartRolle } returns Aktoersroller.BRUKER
        every { fagsak.hentBrukersAktørID() } returns aktørId
        every { behandling.manglerSaksopplysningerAvType(listOf(SaksopplysningType.PDL_PERSOPL)) } returns true
        every { behandling.manglerSaksopplysningerAvType(listOf(SaksopplysningType.PDL_PERS_SAKS)) } returns true

        return behandling
    }

    private fun mockBehandlingWithAllSaksopplysninger(): Behandling {
        val behandling = mockk<Behandling>()
        val fagsak = mockk<Fagsak>()

        every { behandling.id } returns behandlingId
        every { behandling.fagsak } returns fagsak
        every { fagsak.hovedpartRolle } returns Aktoersroller.BRUKER
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
