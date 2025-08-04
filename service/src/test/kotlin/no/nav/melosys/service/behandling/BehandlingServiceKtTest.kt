package no.nav.melosys.service.behandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.UNDER_BEHANDLING
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.FØRSTEGANG
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository
import no.nav.melosys.service.brev.UtkastBrevService
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class BehandlingServiceKtTest {

    @RelaxedMockK
    lateinit var behandlingRepository: BehandlingRepository

    @RelaxedMockK
    lateinit var tidligereMedlemsperiodeRepo: TidligereMedlemsperiodeRepository

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    lateinit var oppgaveService: OppgaveService

    @RelaxedMockK
    lateinit var lovligeKombinasjonerSaksbehandlingService: LovligeKombinasjonerSaksbehandlingService

    @RelaxedMockK
    lateinit var utkastBrevService: UtkastBrevService

    @RelaxedMockK
    lateinit var utledMottaksdato: UtledMottaksdato

    @RelaxedMockK
    lateinit var replikerBehandlingsresultatService: ReplikerBehandlingsresultatService

    @RelaxedMockK
    lateinit var applicationEventPublisher: ApplicationEventPublisher

    private lateinit var behandlingService: BehandlingService
    private lateinit var defaultBehandling: Behandling

    @BeforeEach
    fun setUp() {
        behandlingService = BehandlingService(
            behandlingRepository,
            tidligereMedlemsperiodeRepo,
            behandlingsresultatService,
            oppgaveService,
            lovligeKombinasjonerSaksbehandlingService,
            utkastBrevService,
            applicationEventPublisher,
            utledMottaksdato,
            replikerBehandlingsresultatService
        )

        defaultBehandling = BehandlingTestFactory.builderWithDefaults()
            .medId(BEHANDLING_ID)
            .build()
    }

    @Test
    fun `hentBehandling finner ikke behandling kaster IkkeFunnetException`() {
        every { behandlingRepository.findWithSaksopplysningerById(BEHANDLING_ID) } returns null

        shouldThrow<IkkeFunnetException> {
            behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID)
        }.message shouldContain "Finner ikke behandling med id $BEHANDLING_ID"
    }

    @Test
    fun `finnMedlemsperioder ingen tidligere medlemsperioder returnerer tom liste`() {
        every { tidligereMedlemsperiodeRepo.findById_BehandlingId(any()) } returns ArrayList()

        val periodeIder = behandlingService.hentMedlemsperioder(BEHANDLING_ID)

        periodeIder.shouldBeEmpty()
    }

    @Test
    fun `hentMedlemsperioder returnerer periode ider`() {
        val tidligereMedlemsperioder = listOf(
            TidligereMedlemsperiode(BEHANDLING_ID, 2L),
            TidligereMedlemsperiode(BEHANDLING_ID, 3L)
        )
        every { tidligereMedlemsperiodeRepo.findById_BehandlingId(any()) } returns tidligereMedlemsperioder

        val periodeIder = behandlingService.hentMedlemsperioder(BEHANDLING_ID)

        periodeIder shouldContainExactly listOf(2L, 3L)
    }

    @Test
    fun `behandlingMedSaksnummerTilhørerSaksbehandlerID saksbehandler er satt på oppgaven forvent true`() {
        val saksbehandlerId = "Z123456"
        val oppgave = Oppgave.Builder()
            .setOppgaveId("1")
            .setTilordnetRessurs(saksbehandlerId)
            .build()

        every { oppgaveService.finnBehandlingsoppgaveForBehandlingID(BEHANDLING_ID) } returns oppgave

        val result = behandlingService.behandlingMedSaksnummerTilhørerSaksbehandlerID(BEHANDLING_ID, saksbehandlerId)

        result.shouldBeTrue()
    }

    @Test
    fun `behandlingMedSaksnummerTilhørerSaksbehandlerID saksbehandler er ikke satt på oppgaven forvent false`() {
        val oppgave = Oppgave.Builder()
            .setOppgaveId("1")
            .setTilordnetRessurs("lol")
            .build()

        every { oppgaveService.finnBehandlingsoppgaveForBehandlingID(BEHANDLING_ID) } returns oppgave

        val result = behandlingService.behandlingMedSaksnummerTilhørerSaksbehandlerID(BEHANDLING_ID, "Z123456")

        result.shouldBeFalse()
    }

    @Test
    fun `nyBehandling mangler mottaksdato og årsak kaster feil`() {
        val fagsak = FagsakTestFactory.lagFagsak()

        shouldThrow<FunksjonellException> {
            behandlingService.nyBehandling(
                fagsak,
                Behandlingsstatus.OPPRETTET,
                FØRSTEGANG,
                Behandlingstema.UTSENDT_ARBEIDSTAKER,
                null,
                null,
                null,
                null,
                null
            )
        }.message shouldContain "Mangler mottaksdato eller behandlingsårsaktype"
    }

    private fun opprettBehandlingMedData() = opprettTomBehandlingMedId().apply {
        tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        status = Behandlingsstatus.AVSLUTTET
        initierendeJournalpostId = "initierendeJournalpostId"
        dokumentasjonSvarfristDato = Instant.parse("2017-12-11T09:37:30.00Z")
        behandlingsårsak = opprettBehandlingsårsak()
        saksopplysninger = LinkedHashSet()
        mottatteOpplysninger = MottatteOpplysninger().apply {
            mottatteOpplysningerData = MottatteOpplysningerData()
        }
        saksopplysninger.add(opprettSaksopplysning())
        fagsak = FagsakTestFactory.lagFagsak()
    }

    private fun opprettBehandlingsårsak() = Behandlingsaarsak().apply {
        id = 23L
        mottaksdato = LocalDate.now()
    }

    private fun opprettSaksopplysning() = Saksopplysning().apply {
        behandling = opprettTomBehandlingMedId()
        kilder = opprettSaksopplysningkildeMedID()
        type = SaksopplysningType.INNTK
        endretDato = Instant.parse("2020-02-11T09:37:30Z")
    }

    private fun opprettSaksopplysningkildeMedID() = setOf(
        SaksopplysningKilde().apply {
            id = 123321L
            kilde = SaksopplysningKildesystem.EREG
            mottattDokument = "dokxml"
        }
    )

    private fun opprettTomBehandlingMedId() = BehandlingTestFactory.builderWithDefaults()
        .medId(665L)
        .build()

    companion object {
        private const val BEHANDLING_ID = 11L
        private val BEHANDLING_TYPE = Behandlingstyper.NY_VURDERING
        private val BEHANDLING_TEMA = Behandlingstema.ARBEID_FLERE_LAND
        private val BEHANDLING_STATUS = UNDER_BEHANDLING
        private val MOTTAKSDATO = LocalDate.now().plusMonths(1)
        private val PERIODE_IDS = listOf(2L, 3L)
    }
}
