package no.nav.melosys.saksflyt.steg.behandling

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AvsluttFagsakOgBehandlingTest {

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var saksbehandlingRegler: SaksbehandlingRegler

    private lateinit var avsluttFagsakOgBehandling: AvsluttFagsakOgBehandling
    private lateinit var behandling: Behandling
    private lateinit var fagsak: Fagsak
    private lateinit var lovvalgsperiode: Lovvalgsperiode
    private lateinit var prosessinstans: Prosessinstans

    @BeforeEach
    fun setUp() {
        avsluttFagsakOgBehandling = AvsluttFagsakOgBehandling(
            fagsakService,
            behandlingService,
            behandlingsresultatService,
            saksbehandlingRegler
        )


        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.IVERKSETT_VEDTAK_EOS
            status = ProsessStatus.KLAR
            behandling {
                id = 123L
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.YRKESAKTIV
                fagsak { }
            }
        }
        behandling = prosessinstans.hentBehandling
        fagsak = behandling.fagsak

        lovvalgsperiode = Lovvalgsperiode().apply {
            lovvalgsland = Land_iso2.NO
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }

        val behandlingsresultat = Behandlingsresultat().apply {
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            lovvalgsperioder = mutableSetOf(lovvalgsperiode)
            behandling = prosessinstans.behandling
        }

        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns false
    }

    @Test
    fun `utfør skal avslutte behandling og fagsak når er artikkel 12`() {
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { fagsakService.avsluttFagsakOgBehandling(any(), any<Saksstatuser>()) } just Runs
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1


        avsluttFagsakOgBehandling.utfør(prosessinstans)


        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.LOVVALG_AVKLART) }
    }

    @Test
    fun `utfør skal sette behandlingsstatus til midlertidig lovvalgsbeslutning når er artikkel 13`() {
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { behandlingService.endreStatus(any<Long>(), any<Behandlingsstatus>()) } just Runs
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A


        avsluttFagsakOgBehandling.utfør(prosessinstans)


        verify { behandlingService.endreStatus(behandling.id, Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING) }
    }

    @Test
    fun `utfør skal avslutte behandling og fagsak når er artikkel 13 og behandlingstema A1 anmodning om unntak papir`() {
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A
        behandling.tema = Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR
        fagsak.tema = Sakstemaer.UNNTAK
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { fagsakService.avsluttFagsakOgBehandling(any(), any<Saksstatuser>()) } just Runs
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns true


        avsluttFagsakOgBehandling.utfør(prosessinstans)


        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.LOVVALG_AVKLART) }
    }

    @Test
    fun `utfør skal sette behandlingsstatus når saksstatus i prosess data`() {
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { fagsakService.avsluttFagsakOgBehandling(any(), any<Saksstatuser>()) } just Runs
        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.IVERKSETT_VEDTAK_EOS
            status = ProsessStatus.KLAR
            behandling = this@AvsluttFagsakOgBehandlingTest.behandling
            medData(ProsessDataKey.SAKSSTATUS, Saksstatuser.AVSLUTTET)
        }


        avsluttFagsakOgBehandling.utfør(prosessinstans)


        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.AVSLUTTET) }
    }

    @Test
    fun `utfør skal kun avslutte behandling når fatt iverksett vedtak årsavregning prosess med flere enn en behandling`() {
        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.IVERKSETT_VEDTAK_AARSAVREGNING
            status = ProsessStatus.KLAR
            behandling = this@AvsluttFagsakOgBehandlingTest.behandling
        }
        behandling.type = Behandlingstyper.ÅRSAVREGNING

        val behandling2 = Behandling.forTest {
            id = 1234L
            type = Behandlingstyper.ÅRSAVREGNING
        }
        fagsak.behandlinger.add(behandling2)

        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { behandlingService.avsluttBehandling(any()) } just Runs


        avsluttFagsakOgBehandling.utfør(prosessinstans)


        verify { behandlingService.avsluttBehandling(behandling.id) }
    }

    @Test
    fun `utfør skal avslutte sak og behandling når fatt iverksett vedtak årsavregning prosess med kun en behandling`() {
        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.IVERKSETT_VEDTAK_AARSAVREGNING
            status = ProsessStatus.KLAR
            behandling = this@AvsluttFagsakOgBehandlingTest.behandling
        }
        behandling.type = Behandlingstyper.ÅRSAVREGNING

        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { fagsakService.avsluttFagsakOgBehandling(any(), any<Behandling>(), any<Saksstatuser>()) } just Runs


        avsluttFagsakOgBehandling.utfør(prosessinstans)


        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, Saksstatuser.AVSLUTTET) }
    }
}
