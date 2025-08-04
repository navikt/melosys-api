package no.nav.melosys.service.eessi

import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.eessi.Periode
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.eessi.ruting.ArbeidFlereLandSedRuter
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class ArbeidFlereLandSedRuterKtTest {

    @RelaxedMockK
    lateinit var prosessinstansService: ProsessinstansService

    @RelaxedMockK
    lateinit var fagsakService: FagsakService

    @RelaxedMockK
    lateinit var behandlingService: BehandlingService

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    lateinit var oppgaveService: OppgaveService

    private lateinit var arbeidFlereLandSedRuter: ArbeidFlereLandSedRuter

    private val behandlingID = 123L
    private val gsakSaksnummer = 1111L

    private lateinit var behandling: Behandling
    private lateinit var behandlingsresultat: Behandlingsresultat
    private lateinit var fagsak: Fagsak
    private lateinit var melosysEessiMelding: MelosysEessiMelding
    private lateinit var prosessinstans: Prosessinstans

    @BeforeEach
    fun setup() {
        arbeidFlereLandSedRuter = ArbeidFlereLandSedRuter(
            prosessinstansService,
            fagsakService,
            behandlingService,
            behandlingsresultatService,
            oppgaveService
        )

        behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(behandlingID)
            .build()

        fagsak = FagsakTestFactory.builder().behandlinger(behandling).build()
        behandling.fagsak = fagsak

        melosysEessiMelding = MelosysEessiMelding().apply {
            bucType = BucType.LA_BUC_02.name
            sedType = SedType.A003.name
            aktoerId = "aktørID"
        }
        prosessinstans = Prosessinstans()

        behandlingsresultat = Behandlingsresultat().apply {
            this.behandling = behandling
        }
    }

    @Test
    fun `finnsakOgBestemRuting utenArkivsaknummer forventNySakRuting`() {
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, null)

        verify {
            prosessinstansService.opprettProsessinstansNySakArbeidFlereLand(
                melosysEessiMelding,
                Sakstemaer.UNNTAK,
                Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
                melosysEessiMelding.aktoerId
            )
        }
    }

    @Test
    fun `finnsakOgBestemRuting fagsakEksistererIkke kasterException`() {
        val prosessinstans = Prosessinstans()

        // Explicitly mock the service to return empty optional
        every { fagsakService.finnFagsakFraArkivsakID(0L) } returns java.util.Optional.empty()

        val exception = org.junit.jupiter.api.assertThrows<FunksjonellException> {
            arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, 0L)
        }

        exception.message shouldContain "Finner ingen sak tilknyttet"
    }

    @Test
    fun `finnSakOgBestemRuting norgeUtpektNyttTemaAnnetLandUtpektVedtakIkkeFattet forventNyBehandling`() {
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        melosysEessiMelding.lovvalgsland = Landkoder.SE.kode
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(gsakSaksnummer) } returns java.util.Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer)

        verify {
            prosessinstansService.opprettProsessinstansNyBehandlingArbeidFlereLand(
                melosysEessiMelding,
                Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
                gsakSaksnummer
            )
        }
    }

    @Test
    fun `finnSakOgBestemRuting norgeUtpektNyttTemaAnnetLandUtpektVedtakFattet kasterException`() {
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        behandlingsresultat.vedtakMetadata = VedtakMetadata()
        melosysEessiMelding.lovvalgsland = Landkoder.SE.kode
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(gsakSaksnummer) } returns java.util.Optional.of(fagsak)

        val exception = org.junit.jupiter.api.assertThrows<FunksjonellException> {
            arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer)
        }

        exception.message shouldContain "Det er allerede fattet vedtak på behandling"
    }

    @Test
    fun `finnSakOgBestemRuting annetLandUtpektNyttTemaNorgeUtpekt forventNyBehandling`() {
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
        melosysEessiMelding.lovvalgsland = Landkoder.NO.kode
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(gsakSaksnummer) } returns java.util.Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer)

        verify {
            prosessinstansService.opprettProsessinstansNyBehandlingArbeidFlereLand(
                melosysEessiMelding,
                Behandlingstema.BESLUTNING_LOVVALG_NORGE,
                gsakSaksnummer
            )
        }
    }

    @Test
    fun `finnSakOgBestemRuting norgeUtpektNyttTemaNorgeUtpektBehandlingInaktiv forventOppgaveOpprettetOgProsessinstansNyBehandlingArbeidFlereLand`() {
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        behandling.status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
        melosysEessiMelding.lovvalgsland = Landkoder.NO.kode
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(gsakSaksnummer) } returns java.util.Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer)

        verify {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(eq(behandling), any(), any(), any(), any())
        }
        verify {
            prosessinstansService.opprettProsessinstansNyBehandlingArbeidFlereLand(
                melosysEessiMelding,
                Behandlingstema.BESLUTNING_LOVVALG_NORGE,
                gsakSaksnummer
            )
        }
    }

    @Test
    fun `finnSakOgBestemRuting norgeUtpektNyttTemaNorgeUtpektBehandlingAktiv forventIngenBehandlingStatusVurderDokumentOppdaterOppgave`() {
        val oppgaveID = "4231432"

        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        melosysEessiMelding.lovvalgsland = Landkoder.NO.kode
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        every {
            oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(fagsak.saksnummer)
        } returns java.util.Optional.of(Oppgave.Builder().setOppgaveId(oppgaveID).build())
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(gsakSaksnummer) } returns java.util.Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer)

        verify { behandlingService.endreStatus(behandlingID, Behandlingsstatus.VURDER_DOKUMENT) }
        verify { oppgaveService.oppdaterOppgave(eq(oppgaveID), any<OppgaveOppdatering>()) }
    }

    @Test
    fun `finnSakOgBestemRuting annetLandUtpektNyttTemaAnnetLandUtpektSammePeriode forventIngenBehandling`() {
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND

        val lovvalgsperiode = Lovvalgsperiode().apply {
            fom = LocalDate.now()
            tom = LocalDate.now().plusYears(1)
            lovvalgsland = Land_iso2.SE
        }
        behandlingsresultat.lovvalgsperioder.add(lovvalgsperiode)

        melosysEessiMelding.lovvalgsland = Landkoder.SE.kode
        melosysEessiMelding.periode = Periode(lovvalgsperiode.fom, lovvalgsperiode.tom)
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(gsakSaksnummer) } returns java.util.Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer)

        verify { prosessinstansService.opprettProsessinstansSedJournalføring(behandling, melosysEessiMelding) }
    }

    @Test
    fun `finnSakOgBestemRuting annetLandUtpektNyttTemaAnnetLandUtpektEndretPeriode forventNyBehandling`() {
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND

        val lovvalgsperiode = Lovvalgsperiode().apply {
            lovvalgsland = Land_iso2.SE
            fom = LocalDate.now()
            tom = LocalDate.now().plusYears(1)
        }
        behandlingsresultat.lovvalgsperioder.add(lovvalgsperiode)

        melosysEessiMelding.lovvalgsland = Landkoder.SE.kode
        melosysEessiMelding.periode = Periode(lovvalgsperiode.fom, lovvalgsperiode.tom.plusDays(1))
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(gsakSaksnummer) } returns java.util.Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer)

        verify {
            prosessinstansService.opprettProsessinstansNyBehandlingArbeidFlereLand(
                melosysEessiMelding,
                Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
                gsakSaksnummer
            )
        }
    }

    @Test
    fun `finnSakOgBestemRuting SakstemaUnntakMedNorskLovvalg endresTilMedlemskapLovvalg`() {
        fagsak = FagsakTestFactory.builder().behandlinger(behandling).build()
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        behandling.fagsak = fagsak
        behandlingsresultat = Behandlingsresultat().apply {
            this.behandling = behandling
        }
        melosysEessiMelding.lovvalgsland = Landkoder.SE.kode

        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(gsakSaksnummer) } returns java.util.Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer)

        verify {
            prosessinstansService.opprettProsessinstansNyBehandlingArbeidFlereLand(
                melosysEessiMelding,
                Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
                gsakSaksnummer
            )
        }
    }

    @Test
    fun `finnSakOgBestemRuting SakstemaMedlemskapLovvalgMedUtenlandskLovvalg endresTilUnntak`() {
        fagsak = FagsakTestFactory.builder().tema(Sakstemaer.UNNTAK).behandlinger(behandling).build()
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
        behandling.fagsak = fagsak
        behandlingsresultat = Behandlingsresultat().apply {
            this.behandling = behandling
        }
        melosysEessiMelding.lovvalgsland = Landkoder.NO.kode

        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(gsakSaksnummer) } returns java.util.Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer)

        verify {
            prosessinstansService.opprettProsessinstansNyBehandlingArbeidFlereLand(
                melosysEessiMelding,
                Behandlingstema.BESLUTNING_LOVVALG_NORGE,
                gsakSaksnummer
            )
        }
    }
}
