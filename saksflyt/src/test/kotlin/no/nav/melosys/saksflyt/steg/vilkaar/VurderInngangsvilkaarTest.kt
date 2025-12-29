package no.nav.melosys.saksflyt.steg.vilkaar

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.mottatteOpplysningerForTest
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.vilkaar.InngangsvilkaarService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class VurderInngangsvilkaarTest {

    @MockK
    private lateinit var inngangsvilkaarService: InngangsvilkaarService

    @MockK
    private lateinit var behandlingService: BehandlingService

    private lateinit var vurderInngangsvilkaar: VurderInngangsvilkaar

    private val behandlingID = 143L

    @BeforeEach
    fun setUp() {
        every { inngangsvilkaarService.vurderOgLagreInngangsvilkår(any(), any(), any(), any()) } returns true
        vurderInngangsvilkaar = VurderInngangsvilkaar(inngangsvilkaarService, behandlingService)
    }

    @Test
    fun utfoerSteg_funker() {
        val mottatteOpplysningerData = MottatteOpplysningerData().apply {
            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1L))
            soeknadsland.setLandkoder(listOf(Landkoder.NO.kode, Landkoder.SE.kode))
        }

        val behandling = Behandling.forTest {
            id = behandlingID
            fagsak { }
            mottatteOpplysninger = mottatteOpplysningerForTest {
                this.mottatteOpplysningerData = mottatteOpplysningerData
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling

        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling { id = behandlingID }
        }

        every { inngangsvilkaarService.skalVurdereInngangsvilkår(any()) } returns true
        every {
            inngangsvilkaarService.vurderOgLagreInngangsvilkår(
                behandlingID,
                mottatteOpplysningerData.soeknadsland.landkoder,
                false,
                mottatteOpplysningerData.periode
            )
        } returns true


        vurderInngangsvilkaar.utfør(prosessinstans)


        verify { inngangsvilkaarService.vurderOgLagreInngangsvilkår(any<Long>(), any(), any<Boolean>(), any()) }
    }

    @Test
    fun `utfoerSteg skalIkkeVurdereInngangsvilkår vurdererIkkeInngangsvilkår`() {
        val behandling = Behandling.forTest {
            id = behandlingID
            fagsak { }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling

        every { inngangsvilkaarService.skalVurdereInngangsvilkår(any()) } returns false

        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling { id = behandlingID }
        }


        vurderInngangsvilkaar.utfør(prosessinstans)


        verify(exactly = 0) { inngangsvilkaarService.vurderOgLagreInngangsvilkår(any<Long>(), any(), any<Boolean>(), any()) }
    }
}
