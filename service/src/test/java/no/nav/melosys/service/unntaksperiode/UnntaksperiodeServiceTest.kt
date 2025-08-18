package no.nav.melosys.service.unntaksperiode

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikke_godkjent_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.kontroll.feature.unntaksperiode.UnntaksperiodeKontrollService
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class UnntaksperiodeServiceTest {

    @MockK
    private lateinit var prosessinstansService: ProsessinstansService

    @MockK
    private lateinit var oppgaveService: OppgaveService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var lovvalgsperiodeService: LovvalgsperiodeService

    @MockK
    private lateinit var unntaksperiodeKontrollService: UnntaksperiodeKontrollService

    private lateinit var unntaksperiodeService: UnntaksperiodeService
    private lateinit var behandling: Behandling

    @BeforeEach
    fun setUp() {
        unntaksperiodeService = UnntaksperiodeService(
            behandlingService,
            behandlingsresultatService,
            lovvalgsperiodeService,
            oppgaveService,
            prosessinstansService,
            unntaksperiodeKontrollService
        )

        behandling = Behandling.forTest {
            id = 1L
            fagsak = FagsakTestFactory.lagFagsak()
            tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
        }

        every { behandlingService.hentBehandling(any()) } returns behandling
    }

    @Test
    fun `godkjennPeriode behandling avsluttet forvent exception`() {
        behandling.status = Behandlingsstatus.AVSLUTTET
        val unntaksperiodeGodkjenning = UnntaksperiodeGodkjenning.builder().build()

        val exception = shouldThrow<FunksjonellException> {
            unntaksperiodeService.godkjennPeriode(1L, unntaksperiodeGodkjenning)
        }
        exception.message shouldContain "er inaktiv"
    }

    @Test
    fun `godkjennPeriode feil behandlingstype forvent exception`() {
        behandling.tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        val unntaksperiodeGodkjenning = UnntaksperiodeGodkjenning.builder().build()

        val exception = shouldThrow<FunksjonellException> {
            unntaksperiodeService.godkjennPeriode(1L, unntaksperiodeGodkjenning)
        }
        exception.message shouldContain "ikke av tema"
    }

    @Test
    fun `godkjennPeriode sed dokument har opp ned periode forvent exception`() {
        val sedSaksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.SEDOPPL
            dokument = SedDokument().apply {
                lovvalgsperiode = PERIODE_BAD
            }
        }
        behandling.saksopplysninger.add(sedSaksopplysning)
        val unntaksperiodeGodkjenning = UnntaksperiodeGodkjenning.builder().build()

        val exception = shouldThrow<FunksjonellException> {
            unntaksperiodeService.godkjennPeriode(1L, unntaksperiodeGodkjenning)
        }
        exception.message shouldContain "har feil i perioden"
    }

    @Test
    fun `godkjennPeriode endret periode uten feil verifiser kall`() {
        val sedSaksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.SEDOPPL
            dokument = SedDokument().apply {
                lovvalgsperiode = PERIODE_BAD
            }
        }
        behandling.saksopplysninger.add(sedSaksopplysning)

        val unntaksperiode = Unntaksperiode(LocalDate.of(2000, 1, 1), LocalDate.of(2001, 1, 1))
        val endretUnntaksperiodeGodkjenning = UnntaksperiodeGodkjenning.builder()
            .varsleUtland(false)
            .fritekst(null)
            .endretPeriode(unntaksperiode)
            .lovvalgsbestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1)
            .build()

        every { behandlingsresultatService.settUtfallRegistreringUnntakOgType(any(), any()) } just Runs
        every { lovvalgsperiodeService.lagreLovvalgsperioder(any(), any()) } returns emptyList()
        every { prosessinstansService.opprettProsessinstansGodkjennUnntaksperiode(any(), any(), any(), any()) } just Runs
        every { oppgaveService.ferdigstillOppgaveMedBehandlingID(any()) } just Runs
        every { unntaksperiodeKontrollService.kontrollPeriode(any<SedDokument>(), any()) } just Runs

        unntaksperiodeService.godkjennPeriode(1L, endretUnntaksperiodeGodkjenning)

        val lovvalgsperiodeSlot = slot<Collection<Lovvalgsperiode>>()
        verify { lovvalgsperiodeService.lagreLovvalgsperioder(1L, capture(lovvalgsperiodeSlot)) }
        verify { prosessinstansService.opprettProsessinstansGodkjennUnntaksperiode(any(), eq(false), isNull(), isNull()) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(behandling.id) }
        verify {
            unntaksperiodeKontrollService.kontrollPeriode(
                any<SedDokument>(),
                any<Periode>()
            )
        }
    }

    @Test
    fun `godkjennPeriode endret periode er opp ned forvent exception`() {
        val unntaksperiode = Unntaksperiode(LocalDate.of(2001, 1, 1), LocalDate.of(2000, 1, 1))
        val endretUnntaksperiodeGodkjenning = UnntaksperiodeGodkjenning.builder()
            .endretPeriode(unntaksperiode)
            .build()

        shouldThrow<FunksjonellException> {
            unntaksperiodeService.godkjennPeriode(1L, endretUnntaksperiodeGodkjenning)
        }
    }

    @Test
    fun `godkjennPeriode tom endret periode forvent exception`() {
        val unntaksperiode = Unntaksperiode(null, null)
        val endretUnntaksperiodeGodkjenning = UnntaksperiodeGodkjenning.builder()
            .endretPeriode(unntaksperiode)
            .build()

        shouldThrow<FunksjonellException> {
            unntaksperiodeService.godkjennPeriode(1L, endretUnntaksperiodeGodkjenning)
        }
    }

    @Test
    fun `ikkeGodkjennPeriode opp ned periode forvent ingen exception`() {
        val sedSaksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.SEDOPPL
            dokument = SedDokument().apply {
                lovvalgsperiode = PERIODE_BAD
            }
        }
        behandling.saksopplysninger.add(sedSaksopplysning)
        val begrunnelser = mutableSetOf(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.kode)

        every { behandlingsresultatService.settUtfallRegistreringUnntakOgType(any(), any()) } just Runs
        every { behandlingsresultatService.oppdaterBegrunnelser(any(), any(), any()) } just Runs
        every { prosessinstansService.opprettProsessinstansUnntaksperiodeAvvist(any(), any()) } just Runs
        every { oppgaveService.ferdigstillOppgaveMedBehandlingID(any()) } just Runs

        shouldNotThrow<Exception> {
            unntaksperiodeService.ikkeGodkjennPeriode(1L, begrunnelser, null)
        }
    }

    @Test
    fun `ikkeGodkjennPeriode med begrunnelser ingen feil`() {
        leggTilNødvendigeSaksopplysninger()
        val begrunnelser = mutableSetOf(Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.kode)

        every { behandlingsresultatService.settUtfallRegistreringUnntakOgType(any(), any()) } just Runs
        every { behandlingsresultatService.oppdaterBegrunnelser(any(), any(), any()) } just Runs
        every { prosessinstansService.opprettProsessinstansUnntaksperiodeAvvist(any(), any()) } just Runs
        every { oppgaveService.ferdigstillOppgaveMedBehandlingID(any()) } just Runs

        unntaksperiodeService.ikkeGodkjennPeriode(1L, begrunnelser, null)

        verify { prosessinstansService.opprettProsessinstansUnntaksperiodeAvvist(any(), any()) }
    }

    @Test
    fun `ikkeGodkjennPeriode ingen begrunnelser forvent exception`() {
        leggTilNødvendigeSaksopplysninger()

        val exception = shouldThrow<FunksjonellException> {
            unntaksperiodeService.ikkeGodkjennPeriode(1L, setOf(), null)
        }
        exception.message shouldContain "Ingen begrunnelser"
    }

    @Test
    fun `ikkeGodkjennPeriode begrunnelse annet ingen fritekst forvent exception`() {
        leggTilNødvendigeSaksopplysninger()
        val begrunnelser = mutableSetOf(
            Ikke_godkjent_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.kode,
            Ikke_godkjent_begrunnelser.ANNET.kode
        )

        val exception = shouldThrow<FunksjonellException> {
            unntaksperiodeService.ikkeGodkjennPeriode(1L, begrunnelser, null)
        }
        exception.message shouldContain "krever fritekst"
    }

    private fun leggTilNødvendigeSaksopplysninger() {
        behandling.saksopplysninger.add(Saksopplysning().apply {
            this.type = SaksopplysningType.SEDOPPL
            this.dokument = SedDokument().apply {
                this.lovvalgsperiode = PERIODE_OK
            }
        })
    }

    companion object {
        private val PERIODE_OK = Periode(LocalDate.now(), LocalDate.now().plusYears(2))
        private val PERIODE_BAD = Periode(LocalDate.now(), LocalDate.now().minusYears(2))
    }
}
