package no.nav.melosys.service.eessi

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.forTest
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
import java.util.*

@ExtendWith(MockKExtension::class)
class ArbeidFlereLandSedRuterKtTest {

    @RelaxedMockK
    private lateinit var prosessinstansService: ProsessinstansService

    @RelaxedMockK
    private lateinit var fagsakService: FagsakService

    @RelaxedMockK
    private lateinit var behandlingService: BehandlingService

    @RelaxedMockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    private lateinit var oppgaveService: OppgaveService

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
            prosessinstansService, fagsakService, behandlingService,
            behandlingsresultatService, oppgaveService
        )

        behandling = Behandling.forTest {
            id = behandlingID
        }

        fagsak = Fagsak.forTest {
            behandlinger(behandling)
        }
        behandling.fagsak = fagsak

        melosysEessiMelding = MelosysEessiMelding().apply {
            bucType = BucType.LA_BUC_02.name
            sedType = SedType.A003.name
            aktoerId = "aktørID"
        }
        prosessinstans = Prosessinstans()

        behandlingsresultat = Behandlingsresultat().apply {
            behandling = this@ArbeidFlereLandSedRuterKtTest.behandling
        }
    }

    @Test
    fun finnsakOgBestemRuting_utenArkivsaknummer_forventNySakRuting() {
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, null)

        verify {
            prosessinstansService.opprettProsessinstansNySakArbeidFlereLand(
                melosysEessiMelding, Sakstemaer.UNNTAK,
                Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
                melosysEessiMelding.aktoerId
            )
        }
    }

    @Test
    fun finnsakOgBestemRuting_fagsakEksistererIkke_kasterException() {
        val testProsessinstans = Prosessinstans()

        every { fagsakService.finnFagsakFraArkivsakID(0L) } returns Optional.empty()

        val exception = shouldThrow<FunksjonellException> {
            arbeidFlereLandSedRuter.rutSedTilBehandling(testProsessinstans, 0L)
        }
        exception.message shouldContain "Finner ingen sak tilknyttet"
    }

    @Test
    fun finnSakOgBestemRuting_norgeUtpektNyttTemaAnnetLandUtpektVedtakIkkeFattet_forventNyBehandling() {
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        melosysEessiMelding.lovvalgsland = Landkoder.SE.kode
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(gsakSaksnummer) } returns Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer)

        verify {
            prosessinstansService.opprettProsessinstansNyBehandlingArbeidFlereLand(
                melosysEessiMelding, Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, gsakSaksnummer
            )
        }
    }

    @Test
    fun finnSakOgBestemRuting_norgeUtpektNyttTemaAnnetLandUtpektVedtakFattet_kasterException() {
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        behandlingsresultat.vedtakMetadata = VedtakMetadata()
        melosysEessiMelding.lovvalgsland = Landkoder.SE.kode
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(gsakSaksnummer) } returns Optional.of(fagsak)

        val exception = shouldThrow<FunksjonellException> {
            arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer)
        }
        exception.message shouldContain "Det er allerede fattet vedtak på behandling"
    }

    @Test
    fun finnSakOgBestemRuting_annetLandUtpektNyttTemaNorgeUtpekt_forventNyBehandling() {
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
        melosysEessiMelding.lovvalgsland = Landkoder.NO.kode
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(gsakSaksnummer) } returns Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer)

        verify {
            prosessinstansService.opprettProsessinstansNyBehandlingArbeidFlereLand(
                melosysEessiMelding, Behandlingstema.BESLUTNING_LOVVALG_NORGE, gsakSaksnummer
            )
        }
    }

    @Test
    fun finnSakOgBestemRuting_norgeUtpektNyttTemaNorgeUtpektBehandlingInaktiv_forventOppgaveOpprettetOgProsessinstansNyBehandlingArbeidFlereLand() {
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        behandling.status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
        melosysEessiMelding.lovvalgsland = Landkoder.NO.kode
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(gsakSaksnummer) } returns Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer)

        verify {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(eq(behandling), any(), any(), any(), any())
            prosessinstansService.opprettProsessinstansNyBehandlingArbeidFlereLand(
                melosysEessiMelding, Behandlingstema.BESLUTNING_LOVVALG_NORGE, gsakSaksnummer
            )
        }
    }

    @Test
    fun finnSakOgBestemRuting_norgeUtpektNyttTemaNorgeUtpektBehandlingAktiv_forventIngenBehandlingStatusVurderDokumentOppdaterOppgave() {
        val oppgaveID = "4231432"
        val oppgaveOppdateringSlot = slot<OppgaveOppdatering>()

        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        melosysEessiMelding.lovvalgsland = Landkoder.NO.kode
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        every {
            oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(fagsak.saksnummer)
        } returns Optional.of(Oppgave.Builder().setOppgaveId(oppgaveID).build())
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(gsakSaksnummer) } returns Optional.of(fagsak)
        every { oppgaveService.oppdaterOppgave(any(), capture(oppgaveOppdateringSlot)) } just Runs

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer)

        verify {
            behandlingService.endreStatus(behandlingID, Behandlingsstatus.VURDER_DOKUMENT)
            oppgaveService.oppdaterOppgave(eq(oppgaveID), any<OppgaveOppdatering>())
        }

        oppgaveOppdateringSlot.captured.beskrivelse shouldBe "Mottatt SED A003"
    }

    @Test
    fun finnSakOgBestemRuting_annetLandUtpektNyttTemaAnnetLandUtpektSammePeriode_forventIngenBehandling() {
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
        every { fagsakService.finnFagsakFraArkivsakID(gsakSaksnummer) } returns Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer)

        verify {
            prosessinstansService.opprettProsessinstansSedJournalføring(behandling, melosysEessiMelding)
        }
    }

    @Test
    fun finnSakOgBestemRuting_annetLandUtpektNyttTemaAnnetLandUtpektEndretPeriode_forventNyBehandling() {
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
        every { fagsakService.finnFagsakFraArkivsakID(gsakSaksnummer) } returns Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer)

        verify {
            prosessinstansService.opprettProsessinstansNyBehandlingArbeidFlereLand(
                melosysEessiMelding, Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, gsakSaksnummer
            )
        }
    }

    @Test
    fun finnSakOgBestemRuting_SakstemaUnntakMedNorskLovvalg_endresTilMedlemskapLovvalg() {
        fagsak = Fagsak.forTest {
            behandlinger(behandling)
        }
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        behandling.fagsak = fagsak
        behandlingsresultat = Behandlingsresultat().apply {
            behandling = this@ArbeidFlereLandSedRuterKtTest.behandling
        }
        melosysEessiMelding.lovvalgsland = Landkoder.SE.kode

        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(gsakSaksnummer) } returns Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer)

        verify {
            prosessinstansService.opprettProsessinstansNyBehandlingArbeidFlereLand(
                melosysEessiMelding, Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, gsakSaksnummer
            )
        }
    }

    @Test
    fun finnSakOgBestemRuting_SakstemaMedlemskapLovvalgMedUtenlandskLovvalg_endresTilUnntak() {
        fagsak = Fagsak.forTest {
            tema(Sakstemaer.UNNTAK)
            behandlinger(behandling)
        }
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
        behandling.fagsak = fagsak
        behandlingsresultat = Behandlingsresultat().apply {
            behandling = this@ArbeidFlereLandSedRuterKtTest.behandling
        }
        melosysEessiMelding.lovvalgsland = Landkoder.NO.kode

        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(gsakSaksnummer) } returns Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, gsakSaksnummer)

        verify {
            prosessinstansService.opprettProsessinstansNyBehandlingArbeidFlereLand(
                melosysEessiMelding, Behandlingstema.BESLUTNING_LOVVALG_NORGE, gsakSaksnummer
            )
        }
    }
}
