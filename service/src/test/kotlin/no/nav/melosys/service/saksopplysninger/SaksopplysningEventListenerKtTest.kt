package no.nav.melosys.service.saksopplysninger

import io.mockk.Called
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.BehandlingEndretStatusEvent
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakTestFactory.BRUKER_AKTØR_ID
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.person.Informasjonsbehov
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie
import no.nav.melosys.domain.person.familie.OmfattetFamilie
import no.nav.melosys.service.SaksbehandlingDataFactory
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SaksopplysningEventListenerKtTest {

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var persondataFasade: PersondataFasade

    @MockK
    private lateinit var saksopplysningerService: SaksopplysningerService

    @MockK
    private lateinit var avklartefaktaService: AvklartefaktaService

    private lateinit var saksoppplysningEventListener: SaksoppplysningEventListener

    @BeforeEach
    fun setUp() {
        saksoppplysningEventListener = SaksoppplysningEventListener(
            saksopplysningerService,
            behandlingService,
            persondataFasade,
            avklartefaktaService
        )
    }

    @Test
    fun `lagrePersonopplysninger skal lagre personopplysning med familie når behandling er iverksatt med familie`() {
        val behandling = SaksbehandlingDataFactory.lagBehandling()
        behandling.status = Behandlingsstatus.IVERKSETTER_VEDTAK

        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns behandling
        every { avklartefaktaService.hentAvklarteMedfølgendeBarn(any()) } returns ingenMedfolgendeFamilie()
        every { avklartefaktaService.hentAvklarteMedfølgendeEktefelle(any()) } returns lagMedfolgendeFamilie()
        val personopplysninger = PersonopplysningerObjectFactory.lagPersonopplysninger()
        every { persondataFasade.hentPerson(BRUKER_AKTØR_ID, Informasjonsbehov.MED_FAMILIERELASJONER) } returns personopplysninger
        val personMedHistorikk = PersonopplysningerObjectFactory.lagPersonMedHistorikk()
        every { persondataFasade.hentPersonMedHistorikk(BRUKER_AKTØR_ID) } returns personMedHistorikk
        every { saksopplysningerService.lagrePersonopplysninger(any(), any()) } returns Unit
        every { saksopplysningerService.lagrePersonMedHistorikk(any(), any()) } returns Unit

        val event = BehandlingEndretStatusEvent(Behandlingsstatus.IVERKSETTER_VEDTAK, behandling)
        saksoppplysningEventListener.lagrePersonopplysninger(event)

        verify { persondataFasade.hentPerson(any(), eq(Informasjonsbehov.MED_FAMILIERELASJONER)) }
        verify { saksopplysningerService.lagrePersonopplysninger(behandling, personopplysninger) }
        verify { saksopplysningerService.lagrePersonMedHistorikk(behandling, personMedHistorikk) }
    }

    @Test
    fun `lagrePersonopplysninger skal lagre personopplysning uten familie når behandling er iverksatt uten familie`() {
        val behandling = SaksbehandlingDataFactory.lagBehandling()
        behandling.status = Behandlingsstatus.IVERKSETTER_VEDTAK

        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns behandling
        every { avklartefaktaService.hentAvklarteMedfølgendeBarn(any()) } returns ingenMedfolgendeFamilie()
        every { avklartefaktaService.hentAvklarteMedfølgendeEktefelle(any()) } returns ingenMedfolgendeFamilie()
        val personopplysninger = PersonopplysningerObjectFactory.lagPersonopplysninger()
        every { persondataFasade.hentPerson(BRUKER_AKTØR_ID) } returns personopplysninger
        val personMedHistorikk = PersonopplysningerObjectFactory.lagPersonMedHistorikk()
        every { persondataFasade.hentPersonMedHistorikk(BRUKER_AKTØR_ID) } returns personMedHistorikk
        every { saksopplysningerService.lagrePersonopplysninger(any(), any()) } returns Unit
        every { saksopplysningerService.lagrePersonMedHistorikk(any(), any()) } returns Unit

        val event = BehandlingEndretStatusEvent(Behandlingsstatus.IVERKSETTER_VEDTAK, behandling)
        saksoppplysningEventListener.lagrePersonopplysninger(event)

        verify { persondataFasade.hentPerson(any()) }
        verify { saksopplysningerService.lagrePersonopplysninger(behandling, personopplysninger) }
        verify { saksopplysningerService.lagrePersonMedHistorikk(behandling, personMedHistorikk) }
    }

    @Test
    fun `lagrePersonopplysning skal ikke lagre opplysninger når behandling har ikke relevant status`() {
        val behandling = SaksbehandlingDataFactory.lagBehandling()
        behandling.status = Behandlingsstatus.TIDSFRIST_UTLOEPT
        val event = BehandlingEndretStatusEvent(Behandlingsstatus.TIDSFRIST_UTLOEPT, behandling)

        // Mock the service call that might be made even in the negative test case
        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns behandling

        saksoppplysningEventListener.lagrePersonopplysninger(event)

        verify { saksopplysningerService wasNot Called }
        verify { persondataFasade wasNot Called }
    }

    @Test
    fun `lagrePersonopplysning skal ikke lagre opplysninger når hovedpart er virksomhet`() {
        val behandling = SaksbehandlingDataFactory.lagBehandling().apply {
            status = Behandlingsstatus.IVERKSETTER_VEDTAK
            fagsak = Fagsak.forTest { medVirksomhet() }
        }
        val event = BehandlingEndretStatusEvent(Behandlingsstatus.IVERKSETTER_VEDTAK, behandling)
        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns behandling

        saksoppplysningEventListener.lagrePersonopplysninger(event)

        verify { saksopplysningerService wasNot Called }
        verify { persondataFasade wasNot Called }
    }

    private fun ingenMedfolgendeFamilie(): AvklarteMedfolgendeFamilie = AvklarteMedfolgendeFamilie(emptySet(), emptySet())

    private fun lagMedfolgendeFamilie(): AvklarteMedfolgendeFamilie = AvklarteMedfolgendeFamilie(setOf(OmfattetFamilie("adfa")), emptySet())
}
