package no.nav.melosys.service.oppgave

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.domain.oppgave.OppgaveTilbakelegging
import no.nav.melosys.domain.oppgave.PrioritetType
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade
import no.nav.melosys.repository.OppgaveTilbakeleggingRepository
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.oppgave.dto.PlukkOppgaveInnDto
import no.nav.melosys.service.oppgave.dto.TilbakeleggingDto
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
internal class OppgaveplukkerTest {
    @MockK
    private lateinit var oppgaveFasade: OppgaveFasade

    @MockK
    private lateinit var oppgaveTilbakkeleggingRepo: OppgaveTilbakeleggingRepository

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var oppgaveService: OppgaveService

    private val oppgaveFactory = OppgaveFactory()

    private lateinit var oppgaveplukker: Oppgaveplukker

    @BeforeEach
    fun setUp() {
        oppgaveplukker = Oppgaveplukker(
            oppgaveFasade,
            oppgaveTilbakkeleggingRepo,
            fagsakService,
            behandlingService,
            oppgaveService,
            oppgaveFactory
        )
    }

    @Test
    fun plukkOppgave_toOppgaverMedPriHOYForskjelligFrist_plukkoppgaveEldsteFrist() {
        val oppgaver = listOf(
            opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2017, 8, 7), SAKSNUMMER_1),
            opprettOppgave(
                "2",
                Oppgavetyper.BEH_SAK_MK,
                PrioritetType.HOY,
                LocalDate.of(2018, 8, 7),
                SAKSNUMMER_2
            ),
            opprettOppgave("3", Oppgavetyper.JFR, PrioritetType.NORM, LocalDate.of(2018, 8, 10), SAKSNUMMER_3),
            opprettOppgave(
                "4",
                Oppgavetyper.BEH_SAK_MK,
                PrioritetType.HOY,
                LocalDate.of(2018, 8, 5),
                SAKSNUMMER_4
            )
        )
        val fagsak_4 = opprettFagsakMedBehandling(SAKSNUMMER_4)
        val fagsaker = listOf(
            opprettFagsakMedBehandling(SAKSNUMMER_1),
            opprettFagsakMedBehandling(SAKSNUMMER_2),
            opprettFagsakMedBehandling(SAKSNUMMER_3),
            fagsak_4
        )
        every { oppgaveFasade.finnUtildelteOppgaverEtterFrist(any<String>()) } returns oppgaver
        every { fagsakService.hentFagsaker(any<Collection<String>>()) } returns fagsaker
        every { fagsakService.hentFagsak(SAKSNUMMER_4) } returns fagsak_4
        every { behandlingService.lagre(any<Behandling>()) } answers {}
        every { oppgaveService.tildelOppgave(any<String>(), any<String>()) } answers {}


        val oppgave = oppgaveplukker.plukkOppgave("Z01234", opprettPlukkOppgaveInnDto())


        oppgave.shouldNotBeNull()
        oppgave.saksnummer.shouldBe(SAKSNUMMER_4)
    }

    @Test
    fun plukkOppgave_toOppgaverMedPriHOYSammeFristForskjelligAktivDato_plukkoppgaveOpprettetSenest() {
        val oppgaver = listOf(
            opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2017, 8, 7), SAKSNUMMER_1),
            opprettOppgave(
                "2",
                Oppgavetyper.BEH_SAK_MK,
                PrioritetType.HOY,
                LocalDate.of(2018, 8, 7),
                SAKSNUMMER_2
            ),
            opprettOppgave("3", Oppgavetyper.JFR, PrioritetType.NORM, LocalDate.of(2018, 8, 10), SAKSNUMMER_3),
            opprettOppgave(
                "4",
                Oppgavetyper.BEH_SAK_MK,
                PrioritetType.HOY,
                LocalDate.of(2018, 8, 7),
                SAKSNUMMER_4
            )
        )
        val fagsak_2 = opprettFagsakMedBehandling(SAKSNUMMER_2)
        val fagsaker = listOf(
            opprettFagsakMedBehandling(SAKSNUMMER_1), fagsak_2, opprettFagsakMedBehandling(SAKSNUMMER_3), opprettFagsakMedBehandling(SAKSNUMMER_4)
        )
        every { oppgaveFasade.finnUtildelteOppgaverEtterFrist(any<String>()) } returns oppgaver
        every { fagsakService.hentFagsaker(any<Collection<String>>()) } returns fagsaker
        every { fagsakService.hentFagsak(SAKSNUMMER_2) } returns fagsak_2
        every { behandlingService.lagre(any<Behandling>()) } answers {}
        every { oppgaveService.tildelOppgave(any<String>(), any<String>()) } answers {}


        val oppgave = oppgaveplukker.plukkOppgave("Z01234", opprettPlukkOppgaveInnDto())


        oppgave.shouldNotBeNull()
        oppgave.saksnummer.shouldBe(SAKSNUMMER_2)
    }

    @Test
    fun plukkOppgave_avventerDokMedUtløptsvarfrist_plukkOppgave() {
        val oppgaver = listOf(opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2019, 8, 7), SAKSNUMMER_1))
        val fagsak = opprettFagsak(SAKSNUMMER_1)
        val behandling = opprettBehandling().apply {
            status = Behandlingsstatus.AVVENT_DOK_PART
            dokumentasjonSvarfristDato = Instant.now().minus(Duration.ofDays(1))
            this.fagsak = fagsak
        }
        fagsak.leggTilBehandling(behandling)
        every { oppgaveFasade.finnUtildelteOppgaverEtterFrist(any<String>()) } returns oppgaver
        every { fagsakService.hentFagsaker(any<Collection<String>>()) } returns listOf(fagsak)
        every { fagsakService.hentFagsak(SAKSNUMMER_1) } returns fagsak
        every { oppgaveService.tildelOppgave(any<String>(), any<String>()) } answers {}


        val oppgave = oppgaveplukker.plukkOppgave("Z01234", opprettPlukkOppgaveInnDto())


        oppgave.shouldNotBeNull()
        oppgave.oppgaveId.shouldBe("1")
    }

    @Test
    fun oppgaveplukker_velgerIkkeSak_nårStatusErVenterPaaFagligAvklaring() {
        val oppgaver = listOf(opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2019, 8, 7), SAKSNUMMER_1))
        val fagsak = opprettFagsak(SAKSNUMMER_1)
        val behandling = opprettBehandling().apply {
            status = Behandlingsstatus.AVVENT_FAGLIG_AVKLARING
            this.fagsak = fagsak
        }
        fagsak.leggTilBehandling(behandling)
        every { oppgaveFasade.finnUtildelteOppgaverEtterFrist(any<String>()) } returns oppgaver
        every { fagsakService.hentFagsaker(any<Collection<String>>()) } returns listOf(fagsak)


        val oppgave = oppgaveplukker.plukkOppgave("Z01234", opprettPlukkOppgaveInnDto())


        oppgave.shouldBeNull()
    }

    @Test
    fun leggTilbakeOppgave_venterPåDokumentasjon() {
        val behandling = opprettBehandling()
        val fagsak = opprettFagsak(SAKSNUMMER_1).apply {
            gsakSaksnummer = GSAK_SAKSNUMMER
        }
        behandling.fagsak = fagsak
        val oppgaveId = GSAK_SAKSNUMMER.toString()
        val oppgaveBuilder = Oppgave.Builder()
            .setOppgaveId(oppgaveId)
            .setPrioritet(PrioritetType.HOY)
        val saksbehandlerID = "test"
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { oppgaveService.hentÅpenBehandlingsoppgaveMedFagsaksnummer(SAKSNUMMER_1) } returns oppgaveBuilder.build()
        every { oppgaveFasade.leggTilbakeOppgave(any<String>()) } answers {}


        oppgaveplukker.leggTilbakeOppgave(saksbehandlerID, TilbakeleggingDto(BEHANDLING_ID, true))


        verify { oppgaveFasade.leggTilbakeOppgave(oppgaveId) }
        verify(exactly = 0) { oppgaveTilbakkeleggingRepo.save(any<OppgaveTilbakelegging>()) }
    }

    @Test
    fun plukkOppgave_behandlingSomVenterHarSvarfristSomikkeHarGåttUt_plukkerIkkeBehandlingen() {
        val oppgaver = listOf(
            opprettOppgave(
                "1",
                Oppgavetyper.BEH_SAK_MK,
                PrioritetType.LAV,
                LocalDate.of(2017, 8, 7),
                SAKSNUMMER_1
            )
        )
        val fagsak = opprettFagsak(SAKSNUMMER_1)
        val behandling = opprettBehandling().apply {
            status = Behandlingsstatus.AVVENT_DOK_PART
            dokumentasjonSvarfristDato = Instant.now().plus(Duration.ofDays(1))
            this.fagsak = fagsak
        }
        fagsak.leggTilBehandling(behandling)
        every { oppgaveFasade.finnUtildelteOppgaverEtterFrist(any<String>()) } returns oppgaver
        every { fagsakService.hentFagsaker(any<Collection<String>>()) } returns listOf(fagsak)


        val oppgave = oppgaveplukker.plukkOppgave("Z01234", opprettPlukkOppgaveInnDto())


        oppgave.shouldBeNull()
    }

    @Test
    fun plukkOppgave_oppgaveSomVenterHarIkkeSvarfrist_plukkerIkkeBehandlingen() {
        val oppgaver = listOf(
            opprettOppgave(
                "1",
                Oppgavetyper.BEH_SAK_MK,
                PrioritetType.LAV,
                LocalDate.of(2017, 8, 7),
                SAKSNUMMER_1
            )
        )
        val fagsak = opprettFagsak(SAKSNUMMER_1)
        val behandling = opprettBehandling().apply {
            status = Behandlingsstatus.AVVENT_DOK_PART
            this.fagsak = fagsak
        }
        fagsak.leggTilBehandling(behandling)
        every { oppgaveFasade.finnUtildelteOppgaverEtterFrist(any<String>()) } returns oppgaver
        every { fagsakService.hentFagsaker(any<Collection<String>>()) } returns listOf(fagsak)


        val oppgave = oppgaveplukker.plukkOppgave("Z01234", opprettPlukkOppgaveInnDto())


        oppgave.shouldBeNull()
    }

    @Test
    fun plukkOppgave_søknadStatusSvarAou_oppdaterStatus() {
        val behandlingSlot = slot<Behandling>()
        val oppgaver = listOf(opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2017, 8, 7), SAKSNUMMER_1))
        val fagsak = opprettFagsak(SAKSNUMMER_1)
        val behandling = opprettBehandling().apply {
            status = Behandlingsstatus.SVAR_ANMODNING_MOTTATT
            this.fagsak = fagsak

        }
        fagsak.leggTilBehandling(behandling)
        every { oppgaveFasade.finnUtildelteOppgaverEtterFrist(any<String>()) } returns oppgaver
        every { fagsakService.hentFagsaker(any<Collection<String>>()) } returns listOf(fagsak)
        every { fagsakService.hentFagsak(SAKSNUMMER_1) } returns fagsak
        every { behandlingService.lagre(any<Behandling>()) } answers {}
        every { oppgaveService.tildelOppgave(any<String>(), any<String>()) } answers {}


        val oppgave = oppgaveplukker.plukkOppgave("Z01234", opprettPlukkOppgaveInnDto())


        verify { oppgaveFasade.finnUtildelteOppgaverEtterFrist(any<String>()) }
        verify { behandlingService.lagre(capture(behandlingSlot)) }
        oppgave.shouldNotBeNull()
        behandlingSlot.captured.status.shouldBe(Behandlingsstatus.UNDER_BEHANDLING)
    }

    @Test
    fun plukkOppgave_kombinasjonFlereBehandlingstema_sokerOppgaveToGanger() {
        val oppgaver = listOf(opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2017, 8, 7), SAKSNUMMER_1))
        val fagsak = opprettFagsak(SAKSNUMMER_1)
        val behandling = opprettBehandling().apply {
            status = Behandlingsstatus.OPPRETTET
            tema = Behandlingstema.PENSJONIST
            this.fagsak = fagsak
        }
        fagsak.leggTilBehandling(behandling)
        every { oppgaveFasade.finnUtildelteOppgaverEtterFrist(any<String>()) } returns oppgaver
        every { fagsakService.hentFagsaker(any<Collection<String>>()) } returns listOf(fagsak)
        every { fagsakService.hentFagsak(SAKSNUMMER_1) } returns fagsak
        every { behandlingService.lagre(any<Behandling>()) } answers {}
        every { oppgaveService.tildelOppgave(any<String>(), any<String>()) } answers {}


        val oppgave =
            oppgaveplukker.plukkOppgave("Z01234", PlukkOppgaveInnDto(Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.PENSJONIST))


        verify(exactly = 1) { oppgaveFasade.finnUtildelteOppgaverEtterFrist(any<String>()) }
        oppgave.shouldNotBeNull()
    }

    @Test
    fun plukkOppgave_fagsakerUlikQuery_blirIgnorert() {
        val oppgaver = listOf(
            opprettOppgave("1", Oppgavetyper.BEH_SAK_MK, PrioritetType.LAV, LocalDate.of(2018, 8, 7), SAKSNUMMER_1),
            opprettOppgave("2", Oppgavetyper.BEH_SAK_MK, PrioritetType.HOY, LocalDate.of(2018, 8, 7), SAKSNUMMER_2),
            opprettOppgave("3", Oppgavetyper.BEH_SAK_MK, PrioritetType.NORM, LocalDate.of(2018, 8, 7), SAKSNUMMER_3)
        )
        val fagsaker = listOf(
            opprettFagsakMedBehandling(Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.YRKESAKTIV),
            opprettFagsakMedBehandling(Sakstyper.EU_EOS, Sakstemaer.TRYGDEAVGIFT, Behandlingstema.UTSENDT_ARBEIDSTAKER),
            opprettFagsakMedBehandling(Sakstyper.TRYGDEAVTALE, Sakstemaer.UNNTAK, Behandlingstema.UTSENDT_ARBEIDSTAKER)
        )
        every { oppgaveFasade.finnUtildelteOppgaverEtterFrist(any<String>()) } returns oppgaver
        every { fagsakService.hentFagsaker(listOf(SAKSNUMMER_1, SAKSNUMMER_2, SAKSNUMMER_3)) } returns fagsaker


        val oppgave = oppgaveplukker.plukkOppgave(
            "Z01234",
            PlukkOppgaveInnDto(Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.UTSENDT_ARBEIDSTAKER)
        )


        oppgave.shouldBeNull()
    }

    private fun opprettOppgave(
        oppgaveId: String,
        oppgavetype: Oppgavetyper,
        prioritet: PrioritetType,
        fristFerdigstillelse: LocalDate,
        saksnummer: String
    ): Oppgave =
        Oppgave.Builder()
            .setOppgavetype(oppgavetype)
            .setOppgaveId(oppgaveId)
            .setPrioritet(prioritet)
            .setFristFerdigstillelse(fristFerdigstillelse)
            .setSaksnummer(saksnummer)
            .build()

    private fun opprettPlukkOppgaveInnDto(): PlukkOppgaveInnDto =
        PlukkOppgaveInnDto(Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.UTSENDT_ARBEIDSTAKER)

    private fun opprettBehandling(): Behandling =
        Behandling().apply {
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            type = Behandlingstyper.FØRSTEGANG
        }

    private fun opprettFagsak(
        saksnummer: String?,
        sakstype: Sakstyper = Sakstyper.EU_EOS,
        sakstema: Sakstemaer = Sakstemaer.MEDLEMSKAP_LOVVALG
    ): Fagsak =
        FagsakTestFactory.builder().apply {
            this.saksnummer = saksnummer ?: UUID.randomUUID().toString()
            type = sakstype
            tema = sakstema
        }.build()

    private fun opprettFagsakMedBehandling(saksnummer: String): Fagsak {
        val fagsak = opprettFagsak(saksnummer)
        val behandling = opprettBehandling().apply {
            status = Behandlingsstatus.OPPRETTET
            this.fagsak = fagsak
        }
        fagsak.leggTilBehandling(behandling)

        FagsakTestFactory.builder()
            .behandlinger(Behandling())
            .build()

        FagsakTestFactory.builder().apply {
            behandlinger = mutableListOf(Behandling())
        }.build()

        return fagsak
    }

    private fun opprettFagsakMedBehandling(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema
    ): Fagsak {
        val fagsak = opprettFagsak(null, sakstype, sakstema)
        val behandling = Behandling().apply {
            type = Behandlingstyper.FØRSTEGANG
            tema = behandlingstema
            status = Behandlingsstatus.AVVENT_DOK_PART
            dokumentasjonSvarfristDato = Instant.now().plus(Duration.ofDays(1))
            this.fagsak = fagsak
        }
        fagsak.leggTilBehandling(behandling)
        return fagsak
    }

    companion object {
        private const val BEHANDLING_ID = 123L
        private const val GSAK_SAKSNUMMER = 42L
        private const val SAKSNUMMER_1 = "MEL-1111"
        private const val SAKSNUMMER_2 = "MEL-2222"
        private const val SAKSNUMMER_3 = "MEL-3333"
        private const val SAKSNUMMER_4 = "MEL-4444"
    }
}
