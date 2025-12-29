package no.nav.melosys.service.eessi

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
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
import no.nav.melosys.saksflytapi.domain.forTest
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
class ArbeidFlereLandSedRuterTest {

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

    companion object {
        private const val BEHANDLING_ID = 123L
        private const val GSAK_SAKSNUMMER = 1111L
        private const val AKTØR_ID = "aktørID"
    }

    @BeforeEach
    fun setup() {
        arbeidFlereLandSedRuter = ArbeidFlereLandSedRuter(
            prosessinstansService,
            fagsakService,
            behandlingService,
            behandlingsresultatService,
            oppgaveService
        )
    }

    private fun lagMelosysEessiMelding(
        lovvalgsland: String? = null
    ) = MelosysEessiMelding().apply {
        bucType = BucType.LA_BUC_02.name
        sedType = SedType.A003.name
        aktoerId = AKTØR_ID
        lovvalgsland?.let { this.lovvalgsland = it }
    }

    private fun lagFagsakMedBehandling(
        tema: Sakstemaer = Sakstemaer.UNNTAK,
        behandlingTema: Behandlingstema = Behandlingstema.BESLUTNING_LOVVALG_NORGE,
        behandlingStatus: Behandlingsstatus = Behandlingsstatus.OPPRETTET
    ): Pair<Fagsak, Behandling> {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
            this.tema = behandlingTema
            status = behandlingStatus
        }
        val fagsak = Fagsak.forTest {
            tema(tema)
            behandlinger(behandling)
        }
        return fagsak to behandling
    }

    @Test
    fun finnsakOgBestemRuting_utenArkivsaknummer_forventNySakRuting() {
        val melosysEessiMelding = lagMelosysEessiMelding()
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        }

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
    fun finnsakOgBestemRuting_fagsakEksistererIkke_kasterException() {
        val prosessinstans = Prosessinstans.forTest()

        every { fagsakService.finnFagsakFraArkivsakID(0L) } returns Optional.empty()

        val exception = shouldThrow<FunksjonellException> {
            arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, 0L)
        }
        exception.message shouldContain "Finner ingen sak tilknyttet"
    }

    @Test
    fun finnSakOgBestemRuting_norgeUtpektNyttTemaAnnetLandUtpektVedtakIkkeFattet_forventNyBehandling() {
        val (fagsak, behandling) = lagFagsakMedBehandling(
            behandlingTema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        )
        val melosysEessiMelding = lagMelosysEessiMelding(lovvalgsland = Landkoder.SE.kode)
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        }
        val behandlingsresultat = Behandlingsresultat.forTest {
            this.behandling = behandling
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER) } returns Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER)

        verify {
            prosessinstansService.opprettProsessinstansNyBehandlingArbeidFlereLand(
                melosysEessiMelding,
                Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
                GSAK_SAKSNUMMER
            )
        }
    }

    @Test
    fun finnSakOgBestemRuting_norgeUtpektNyttTemaAnnetLandUtpektVedtakFattet_kasterException() {
        val (fagsak, behandling) = lagFagsakMedBehandling(
            behandlingTema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        )
        val melosysEessiMelding = lagMelosysEessiMelding(lovvalgsland = Landkoder.SE.kode)
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        }
        val behandlingsresultat = Behandlingsresultat.forTest {
            this.behandling = behandling
            vedtakMetadata { }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER) } returns Optional.of(fagsak)

        val exception = shouldThrow<FunksjonellException> {
            arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER)
        }
        exception.message shouldContain "Det er allerede fattet vedtak på behandling"
    }

    @Test
    fun finnSakOgBestemRuting_annetLandUtpektNyttTemaNorgeUtpekt_forventNyBehandling() {
        val (fagsak, behandling) = lagFagsakMedBehandling(
            behandlingTema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
        )
        val melosysEessiMelding = lagMelosysEessiMelding(lovvalgsland = Landkoder.NO.kode)
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        }
        val behandlingsresultat = Behandlingsresultat.forTest {
            this.behandling = behandling
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER) } returns Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER)

        verify {
            prosessinstansService.opprettProsessinstansNyBehandlingArbeidFlereLand(
                melosysEessiMelding,
                Behandlingstema.BESLUTNING_LOVVALG_NORGE,
                GSAK_SAKSNUMMER
            )
        }
    }

    @Test
    fun finnSakOgBestemRuting_norgeUtpektNyttTemaNorgeUtpektBehandlingInaktiv_forventOppgaveOpprettetOgProsessinstansNyBehandlingArbeidFlereLand() {
        val (fagsak, behandling) = lagFagsakMedBehandling(
            behandlingTema = Behandlingstema.BESLUTNING_LOVVALG_NORGE,
            behandlingStatus = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
        )
        val melosysEessiMelding = lagMelosysEessiMelding(lovvalgsland = Landkoder.NO.kode)
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        }
        val behandlingsresultat = Behandlingsresultat.forTest {
            this.behandling = behandling
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER) } returns Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER)

        verify {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(eq(behandling), any(), any(), any(), any())
            prosessinstansService.opprettProsessinstansNyBehandlingArbeidFlereLand(
                melosysEessiMelding,
                Behandlingstema.BESLUTNING_LOVVALG_NORGE,
                GSAK_SAKSNUMMER
            )
        }
    }

    @Test
    fun finnSakOgBestemRuting_norgeUtpektNyttTemaNorgeUtpektBehandlingAktiv_forventIngenBehandlingStatusVurderDokumentOppdaterOppgave() {
        val oppgaveID = "4231432"
        val oppgaveOppdateringSlot = slot<OppgaveOppdatering>()

        val (fagsak, behandling) = lagFagsakMedBehandling(
            behandlingTema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        )
        val melosysEessiMelding = lagMelosysEessiMelding(lovvalgsland = Landkoder.NO.kode)
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        }
        val behandlingsresultat = Behandlingsresultat.forTest {
            this.behandling = behandling
        }

        every {
            oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(fagsak.saksnummer)
        } returns Optional.of(Oppgave.Builder().setOppgaveId(oppgaveID).build())
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER) } returns Optional.of(fagsak)
        every { oppgaveService.oppdaterOppgave(any(), capture(oppgaveOppdateringSlot)) } just Runs

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER)

        verify {
            behandlingService.endreStatus(BEHANDLING_ID, Behandlingsstatus.VURDER_DOKUMENT)
            oppgaveService.oppdaterOppgave(eq(oppgaveID), any<OppgaveOppdatering>())
        }

        oppgaveOppdateringSlot.captured.beskrivelse shouldBe "Mottatt SED A003"
    }

    @Test
    fun finnSakOgBestemRuting_annetLandUtpektNyttTemaAnnetLandUtpektSammePeriode_forventIngenBehandling() {
        val periodeFom = LocalDate.now()
        val periodeTom = LocalDate.now().plusYears(1)

        val (fagsak, behandling) = lagFagsakMedBehandling(
            behandlingTema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
        )
        val melosysEessiMelding = lagMelosysEessiMelding(lovvalgsland = Landkoder.SE.kode).apply {
            periode = Periode(periodeFom, periodeTom)
        }
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        }
        val behandlingsresultat = Behandlingsresultat.forTest {
            this.behandling = behandling
            lovvalgsperiode {
                fom = periodeFom
                tom = periodeTom
                lovvalgsland = Land_iso2.SE
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER) } returns Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER)

        verify {
            prosessinstansService.opprettProsessinstansSedJournalføring(behandling, melosysEessiMelding)
        }
    }

    @Test
    fun finnSakOgBestemRuting_annetLandUtpektNyttTemaAnnetLandUtpektEndretPeriode_forventNyBehandling() {
        val periodeFom = LocalDate.now()
        val periodeTom = LocalDate.now().plusYears(1)

        val (fagsak, behandling) = lagFagsakMedBehandling(
            behandlingTema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
        )
        val melosysEessiMelding = lagMelosysEessiMelding(lovvalgsland = Landkoder.SE.kode).apply {
            periode = Periode(periodeFom, periodeTom.plusDays(1))
        }
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        }
        val behandlingsresultat = Behandlingsresultat.forTest {
            this.behandling = behandling
            lovvalgsperiode {
                fom = periodeFom
                tom = periodeTom
                lovvalgsland = Land_iso2.SE
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER) } returns Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER)

        verify {
            prosessinstansService.opprettProsessinstansNyBehandlingArbeidFlereLand(
                melosysEessiMelding,
                Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
                GSAK_SAKSNUMMER
            )
        }
    }

    @Test
    fun finnSakOgBestemRuting_SakstemaUnntakMedNorskLovvalg_endresTilMedlemskapLovvalg() {
        val (fagsak, behandling) = lagFagsakMedBehandling(
            behandlingTema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        )
        val melosysEessiMelding = lagMelosysEessiMelding(lovvalgsland = Landkoder.SE.kode)
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        }
        val behandlingsresultat = Behandlingsresultat.forTest {
            this.behandling = behandling
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER) } returns Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER)

        verify {
            prosessinstansService.opprettProsessinstansNyBehandlingArbeidFlereLand(
                melosysEessiMelding,
                Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
                GSAK_SAKSNUMMER
            )
        }
    }

    @Test
    fun finnSakOgBestemRuting_SakstemaMedlemskapLovvalgMedUtenlandskLovvalg_endresTilUnntak() {
        val (fagsak, behandling) = lagFagsakMedBehandling(
            tema = Sakstemaer.UNNTAK,
            behandlingTema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
        )
        val melosysEessiMelding = lagMelosysEessiMelding(lovvalgsland = Landkoder.NO.kode)
        val prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)
        }
        val behandlingsresultat = Behandlingsresultat.forTest {
            this.behandling = behandling
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { fagsakService.finnFagsakFraArkivsakID(GSAK_SAKSNUMMER) } returns Optional.of(fagsak)

        arbeidFlereLandSedRuter.rutSedTilBehandling(prosessinstans, GSAK_SAKSNUMMER)

        verify {
            prosessinstansService.opprettProsessinstansNyBehandlingArbeidFlereLand(
                melosysEessiMelding,
                Behandlingstema.BESLUTNING_LOVVALG_NORGE,
                GSAK_SAKSNUMMER
            )
        }
    }
}
