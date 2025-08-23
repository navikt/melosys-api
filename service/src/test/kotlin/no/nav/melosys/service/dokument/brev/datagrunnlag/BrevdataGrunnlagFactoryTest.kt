package no.nav.melosys.service.dokument.brev.datagrunnlag

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.brev.DoksysBrevbestilling
import no.nav.melosys.domain.person.Informasjonsbehov.MED_FAMILIERELASJONER
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie
import no.nav.melosys.domain.person.familie.OmfattetFamilie
import no.nav.melosys.service.SaksbehandlingDataFactory
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class BrevdataGrunnlagFactoryTest {
    @MockK
    private lateinit var avklartefaktaService: AvklartefaktaService

    @MockK
    private lateinit var avklarteVirksomheterService: AvklarteVirksomheterService

    @MockK
    private lateinit var kodeverkService: KodeverkService

    @MockK
    private lateinit var persondataFasade: PersondataFasade

    private lateinit var brevdataGrunnlagFactory: BrevdataGrunnlagFactory

    @BeforeEach
    fun setUp() {
        brevdataGrunnlagFactory = BrevdataGrunnlagFactory(
            avklartefaktaService,
            avklarteVirksomheterService,
            kodeverkService,
            persondataFasade
        )

        // Set up default mocks for methods that are called
        every { avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(any()) } returns emptyMap<String, AvklartMaritimtArbeid>()
    }

    @Test
    fun `hentPersondata skal hente person med familierelasjoner når avklarte medfølgende barn finnes`() {
        every { avklartefaktaService.hentAvklarteMedfølgendeBarn(any()) } returns lagMedfolgendeFamilie()
        every { persondataFasade.hentPerson(any(), eq(MED_FAMILIERELASJONER)) } returns PersonopplysningerObjectFactory.lagPersonopplysninger()
        val doksysBrevbestilling = DoksysBrevbestilling.Builder().medBehandling(SaksbehandlingDataFactory.lagBehandling()).build()


        brevdataGrunnlagFactory.av(doksysBrevbestilling)


        verify { persondataFasade.hentPerson(any(), MED_FAMILIERELASJONER) }
    }

    @Test
    fun `hentPersondata skal hente person uten familierelasjoner når avklarte medfølgende barn ikke finnes`() {
        every { avklartefaktaService.hentAvklarteMedfølgendeBarn(any()) } returns ingenMedfolgendeFamilie()
        every { persondataFasade.hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysninger()
        val doksysBrevbestilling = DoksysBrevbestilling.Builder().medBehandling(SaksbehandlingDataFactory.lagBehandling()).build()


        brevdataGrunnlagFactory.av(doksysBrevbestilling)


        verify { persondataFasade.hentPerson(any()) }
    }

    private fun ingenMedfolgendeFamilie() = AvklarteMedfolgendeFamilie(emptySet(), emptySet())

    private fun lagMedfolgendeFamilie() = AvklarteMedfolgendeFamilie(setOf(OmfattetFamilie("adfa")), emptySet())
}
