package no.nav.melosys.service.dokument.sed

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.person.Informasjonsbehov.MED_FAMILIERELASJONER
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie
import no.nav.melosys.domain.person.familie.OmfattetFamilie
import no.nav.melosys.service.SaksbehandlingDataFactory
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SedDataGrunnlagFactoryKtTest {
    private val avklartefaktaService: AvklartefaktaService = mockk()
    private val avklarteVirksomheterService: AvklarteVirksomheterService = mockk()
    private val kodeverkService: KodeverkService = mockk()
    private val persondataFasade: PersondataFasade = mockk()

    private lateinit var sedDataGrunnlagFactory: SedDataGrunnlagFactory

    @BeforeEach
    fun setUp() {
        sedDataGrunnlagFactory = SedDataGrunnlagFactory(
            avklartefaktaService, avklarteVirksomheterService, kodeverkService,
            persondataFasade
        )
    }

    @Test
    fun hentPersondata_avklarteMedfølgendeBarnFinnes_henterFamilie() {
        every { avklartefaktaService.hentAvklarteMedfølgendeBarn(any()) } returns lagMedfolgendeFamilie()
        every { avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(any()) } returns emptyMap()
        every { persondataFasade.hentPerson(any(), eq(MED_FAMILIERELASJONER)) } returns PersonopplysningerObjectFactory.lagPersonopplysninger()
        sedDataGrunnlagFactory.av(SaksbehandlingDataFactory.lagBehandling())
        verify { persondataFasade.hentPerson(any(), eq(MED_FAMILIERELASJONER)) }
    }

    @Test
    fun hentPersondata_avklarteMedfølgendeBarnFinnesIkke_henterIkkeFamilie() {
        every { avklartefaktaService.hentAvklarteMedfølgendeBarn(any()) } returns ingenMedfolgendeFamilie()
        every { avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(any()) } returns emptyMap()
        every { persondataFasade.hentPerson(any()) } returns PersonopplysningerObjectFactory.lagPersonopplysninger()
        sedDataGrunnlagFactory.av(SaksbehandlingDataFactory.lagBehandling())
        verify { persondataFasade.hentPerson(any()) }
    }

    private fun ingenMedfolgendeFamilie(): AvklarteMedfolgendeFamilie =
        AvklarteMedfolgendeFamilie(emptySet(), emptySet())

    private fun lagMedfolgendeFamilie(): AvklarteMedfolgendeFamilie =
        AvklarteMedfolgendeFamilie(setOf(OmfattetFamilie("adfa")), emptySet())
}
