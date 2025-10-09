package no.nav.melosys.service.behandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.brev.utkast.UtkastBrev
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
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
import java.time.temporal.ChronoUnit
import java.util.*

@ExtendWith(MockKExtension::class)
class BehandlingServiceTest {

    @MockK
    lateinit var behandlingRepository: BehandlingRepository

    @MockK
    lateinit var tidligereMedlemsperiodeRepo: TidligereMedlemsperiodeRepository

    @MockK
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

        defaultBehandling = Behandling.forTest {
            id = BEHANDLING_ID
        }
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
        val fagsak = Fagsak.forTest {}


        shouldThrow<FunksjonellException> {
            behandlingService.nyBehandling(
                fagsak,
                OPPRETTET,
                Behandlingstyper.FØRSTEGANG,
                Behandlingstema.UTSENDT_ARBEIDSTAKER,
                null,
                null,
                null,
                null,
                null
            )
        }.message shouldContain "Mangler mottaksdato eller behandlingsårsaktype"
    }

    @Test
    fun `endreBehandling oppdaterer alle felt og publiserer event`() {
        defaultBehandling = Behandling.forTest {
            id = BEHANDLING_ID
            tema = Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY
            type = Behandlingstyper.HENVENDELSE
            status = OPPRETTET
            behandlingsårsak = Behandlingsaarsak()
            fagsak {
                medBruker()
            }
        }

        val behandlingSlots = mutableListOf<Behandling>()
        val behandlingEventSlots = mutableListOf<BehandlingEvent>()

        every { behandlingRepository.findById(BEHANDLING_ID) } returns Optional.of(defaultBehandling)
        every { behandlingRepository.save(capture(behandlingSlots)) } answers { firstArg() }
        every { applicationEventPublisher.publishEvent(capture(behandlingEventSlots)) } just Runs

        behandlingService.endreBehandling(BEHANDLING_ID, BEHANDLING_TYPE, BEHANDLING_TEMA, BEHANDLING_STATUS, MOTTAKSDATO)

        verify(exactly = 5) { behandlingRepository.save(any()) }
        verify { applicationEventPublisher.publishEvent(any<BehandlingEvent>()) }

        // Verify the order and content of saves
        behandlingSlots[0].id shouldBe BEHANDLING_ID
        behandlingSlots[0].status shouldBe BEHANDLING_STATUS
        behandlingSlots[1].id shouldBe BEHANDLING_ID
        behandlingSlots[1].type shouldBe BEHANDLING_TYPE
        behandlingSlots[2].id shouldBe BEHANDLING_ID
        behandlingSlots[2].behandlingsårsak?.mottaksdato shouldBe MOTTAKSDATO
        behandlingSlots[2].behandlingsfrist shouldBe behandlingSlots[2].utledBehandlingsfrist(MOTTAKSDATO)
        behandlingSlots[3].id shouldBe BEHANDLING_ID
        behandlingSlots[3].tema shouldBe BEHANDLING_TEMA

        // Verify events
        behandlingEventSlots[0].behandlingID shouldBe BEHANDLING_ID
        (behandlingEventSlots[0] as BehandlingEndretStatusEvent).behandlingsstatus shouldBe BEHANDLING_STATUS
    }

    @Test
    fun `endreBehandling nullEllerSammeVerdi ingenEndring`() {
        defaultBehandling = Behandling.forTest {
            id = BEHANDLING_ID
            tema = BEHANDLING_TEMA
            type = BEHANDLING_TYPE
            status = BEHANDLING_STATUS
            behandlingsfrist = MOTTAKSDATO
            mottatteOpplysninger = opprettMottatteOpplysninger()
            fagsak {
                medBruker()
            }
        }

        every { behandlingRepository.findById(BEHANDLING_ID) } returns Optional.of(defaultBehandling)
        every { utledMottaksdato.getMottaksdato(any()) } returns MOTTAKSDATO
        every { behandlingRepository.save(any()) } answers { firstArg() }

        behandlingService.endreBehandling(BEHANDLING_ID, BEHANDLING_TYPE, null, null, null)

        verify { behandlingRepository.save(any()) }
        verify(exactly = 0) { applicationEventPublisher.publishEvent(any()) }
    }

    @Test
    fun `avsluttBehandling har utkastBrev kasterFunksjonellException`() {
        defaultBehandling = Behandling.forTest {
            id = BEHANDLING_ID
            status = UNDER_BEHANDLING
        }

        every { behandlingRepository.findById(BEHANDLING_ID) } returns Optional.of(defaultBehandling)
        every { utkastBrevService.hentUtkast(BEHANDLING_ID) } returns listOf(
            UtkastBrev.Builder().behandlingID(BEHANDLING_ID).lagretAvSaksbehandler("test").build()
        )

        shouldThrow<FunksjonellException> {
            behandlingService.avsluttBehandling(BEHANDLING_ID)
        }.message shouldContain "Det finnes et åpent brevutkast"
    }


    @Test
    fun `oppdaterStatus statusAvsluttet ferdigstillOppgave`() {
        defaultBehandling = Behandling.forTest {
            id = BEHANDLING_ID
            status = UNDER_BEHANDLING
        }

        every { behandlingRepository.findById(BEHANDLING_ID) } returns Optional.of(defaultBehandling)
        every { behandlingRepository.save(any()) } answers { firstArg() }
        every { oppgaveService.ferdigstillOppgaveMedBehandlingID(any()) } just Runs
        every { applicationEventPublisher.publishEvent(any<BehandlingEndretStatusEvent>()) } just Runs
        every { lovligeKombinasjonerSaksbehandlingService.validerNyStatusMulig(any(), any()) } just Runs

        behandlingService.endreStatus(BEHANDLING_ID, AVSLUTTET)

        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        verify { applicationEventPublisher.publishEvent(any<BehandlingEndretStatusEvent>()) }
    }

    @Test
    fun `knyttMedlemsperioder avsluttetBehandling kasterException`() {
        defaultBehandling = Behandling.forTest {
            id = BEHANDLING_ID
            status = AVSLUTTET
        }

        every { behandlingRepository.findById(BEHANDLING_ID) } returns Optional.of(defaultBehandling)

        shouldThrow<FunksjonellException> {
            behandlingService.knyttMedlemsperioder(BEHANDLING_ID, PERIODE_IDS)
        }.message shouldContain "Medlemsperioder kan ikke lagres på behandling med status AVSLUTTET"
    }

    @Test
    fun `knyttMedlemsperioder ingenBehandling kasterException`() {
        every { behandlingRepository.findById(BEHANDLING_ID) } returns Optional.empty()

        shouldThrow<IkkeFunnetException> {
            behandlingService.knyttMedlemsperioder(BEHANDLING_ID, PERIODE_IDS)
        }.message shouldContain "Finner ikke behandling med id $BEHANDLING_ID"
    }


    private fun opprettMottatteOpplysninger() = MottatteOpplysninger().apply {
        mottatteOpplysningerData = MottatteOpplysningerData()
    }

    private fun opprettBehandlingMedData() = opprettTomBehandlingMedId().apply {
        tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        status = AVSLUTTET
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

    @Test
    fun `knyttMedlemsperioder successfully`() {
        val behandling = Behandling.forTest {
            status = UNDER_BEHANDLING
        }

        every { behandlingRepository.findById(BEHANDLING_ID) } returns Optional.of(behandling)
        every { tidligereMedlemsperiodeRepo.deleteById_BehandlingId(BEHANDLING_ID) } just Runs
        every { tidligereMedlemsperiodeRepo.saveAll(any<List<TidligereMedlemsperiode>>()) } returns emptyList()

        behandlingService.knyttMedlemsperioder(BEHANDLING_ID, PERIODE_IDS)

        verify { tidligereMedlemsperiodeRepo.deleteById_BehandlingId(BEHANDLING_ID) }
        verify { tidligereMedlemsperiodeRepo.saveAll(any<List<TidligereMedlemsperiode>>()) }
    }

    @Test
    fun `nyBehandling creates new behandling`() {
        val initierendeJournalpostId = "234"
        val initierendeDokumentId = "221234"
        val fagsak = FagsakTestFactory.lagFagsak()

        every { behandlingRepository.save(any()) } answers { firstArg() }
        every { behandlingsresultatService.lagreNyttBehandlingsresultat(any()) } just Runs

        val behandling = behandlingService.nyBehandling(
            fagsak, OPPRETTET, Behandlingstyper.FØRSTEGANG, Behandlingstema.UTSENDT_ARBEIDSTAKER,
            initierendeJournalpostId, initierendeDokumentId, MOTTAKSDATO, Behandlingsaarsaktyper.SØKNAD, null
        )

        verify { behandlingRepository.save(behandling) }
        verify { behandlingsresultatService.lagreNyttBehandlingsresultat(behandling) }
        behandling.type shouldBe Behandlingstyper.FØRSTEGANG
        behandling.status shouldBe OPPRETTET
        behandling.initierendeJournalpostId shouldBe initierendeJournalpostId
        behandling.initierendeDokumentId shouldBe initierendeDokumentId
    }

    @Test
    fun `nyBehandling behandlingsfristKriterier får8UkerBehandlingsfrist`() {
        val initierendeJournalpostId = "234"
        val initierendeDokumentId = "221234"
        val fagsak = FagsakTestFactory.lagFagsak()
        val frist8Uker = LocalDate.now().plusWeeks(8)

        every { behandlingRepository.save(any()) } answers { firstArg() }
        every { behandlingsresultatService.lagreNyttBehandlingsresultat(any()) } just Runs

        val behandling = behandlingService.nyBehandling(
            fagsak, OPPRETTET, Behandlingstyper.FØRSTEGANG, Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
            initierendeJournalpostId, initierendeDokumentId, LocalDate.now(), Behandlingsaarsaktyper.SØKNAD, null
        )

        verify { behandlingRepository.save(behandling) }
        verify { behandlingsresultatService.lagreNyttBehandlingsresultat(behandling) }
        behandling.behandlingsfrist shouldBe frist8Uker
        behandling.status shouldBe OPPRETTET
        behandling.initierendeJournalpostId shouldBe initierendeJournalpostId
        behandling.initierendeDokumentId shouldBe initierendeDokumentId
    }

    @Test
    fun `endreBehandlingstema gyldigEndringForSøknad behandlingLagresOgOppgaveOppdateres`() {
        defaultBehandling = Behandling.forTest {
            id = BEHANDLING_ID
            tema = Behandlingstema.ARBEID_FLERE_LAND
            mottatteOpplysninger = MottatteOpplysninger().apply {
                mottatteOpplysningerData = MottatteOpplysningerData()
            }
        }

        every { behandlingRepository.save(any()) } answers { firstArg() }

        behandlingService.endreTema(defaultBehandling, Behandlingstema.UTSENDT_ARBEIDSTAKER)

        verify(exactly = 0) { applicationEventPublisher.publishEvent(any()) }
        verify { behandlingRepository.save(any()) }

        val savedSlot = slot<Behandling>()
        verify { behandlingRepository.save(capture(savedSlot)) }
        savedSlot.captured.tema shouldBe Behandlingstema.UTSENDT_ARBEIDSTAKER
        savedSlot.captured.id shouldBe BEHANDLING_ID
    }

    @Test
    fun `nyBehandling behandlingsfristKriterier får70DagerBehandlingsfrist`() {
        val initierendeJournalpostId = "234"
        val initierendeDokumentId = "221234"
        val fagsak = FagsakTestFactory.lagFagsak()
        val frist70Dager = LocalDate.now().plusDays(70)

        every { behandlingRepository.save(any()) } answers { firstArg() }
        every { behandlingsresultatService.lagreNyttBehandlingsresultat(any()) } just Runs

        val behandling = behandlingService.nyBehandling(
            fagsak, OPPRETTET, Behandlingstyper.KLAGE, Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
            initierendeJournalpostId, initierendeDokumentId, LocalDate.now(), Behandlingsaarsaktyper.SØKNAD, null
        )

        verify { behandlingRepository.save(behandling) }
        verify { behandlingsresultatService.lagreNyttBehandlingsresultat(behandling) }
        behandling.behandlingsfrist shouldBe frist70Dager
        behandling.status shouldBe OPPRETTET
        behandling.initierendeJournalpostId shouldBe initierendeJournalpostId
        behandling.initierendeDokumentId shouldBe initierendeDokumentId
    }

    @Test
    fun `nyBehandling behandlingsfristKriterier får90DagerBehandlingsfrist`() {
        val frist90Dager = LocalDate.now().plusDays(90)
        val initierendeJournalpostId = "234"
        val initierendeDokumentId = "221234"
        val fagsak = FagsakTestFactory.builder().tema(Sakstemaer.TRYGDEAVGIFT).build()

        every { behandlingRepository.save(any()) } answers { firstArg() }
        every { behandlingsresultatService.lagreNyttBehandlingsresultat(any()) } just Runs

        val behandling = behandlingService.nyBehandling(
            fagsak, OPPRETTET, Behandlingstyper.FØRSTEGANG, Behandlingstema.ARBEID_KUN_NORGE,
            initierendeJournalpostId, initierendeDokumentId, LocalDate.now(), Behandlingsaarsaktyper.SØKNAD, null
        )

        verify { behandlingRepository.save(behandling) }
        verify { behandlingsresultatService.lagreNyttBehandlingsresultat(behandling) }
        behandling.behandlingsfrist shouldBe frist90Dager
        behandling.status shouldBe OPPRETTET
        behandling.initierendeJournalpostId shouldBe initierendeJournalpostId
        behandling.initierendeDokumentId shouldBe initierendeDokumentId
    }

    @Test
    fun `nyBehandling behandlingsfristKriterier får180DagerBehandlingsfrist`() {
        val frist180Dager = LocalDate.now().plusDays(180)
        val initierendeJournalpostId = "234"
        val initierendeDokumentId = "221234"
        val fagsak = FagsakTestFactory.builder().tema(Sakstemaer.UNNTAK).build()

        every { behandlingRepository.save(any()) } answers { firstArg() }
        every { behandlingsresultatService.lagreNyttBehandlingsresultat(any()) } just Runs

        val behandling = behandlingService.nyBehandling(
            fagsak, OPPRETTET, Behandlingstyper.FØRSTEGANG, Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
            initierendeJournalpostId, initierendeDokumentId, LocalDate.now(), Behandlingsaarsaktyper.SØKNAD, null
        )

        verify { behandlingRepository.save(behandling) }
        verify { behandlingsresultatService.lagreNyttBehandlingsresultat(behandling) }
        behandling.behandlingsfrist shouldBe frist180Dager
        behandling.status shouldBe OPPRETTET
        behandling.initierendeJournalpostId shouldBe initierendeJournalpostId
        behandling.initierendeDokumentId shouldBe initierendeDokumentId
    }

    @Test
    fun `replikerBehandling replikererObjekterOgCollections`() {
        val tidligsteInaktiveBehandling = opprettBehandlingMedData()
        every { behandlingRepository.save(any()) } answers { firstArg() }
        val replikertBehandling = behandlingService.replikerBehandling(tidligsteInaktiveBehandling, Behandlingstyper.ENDRET_PERIODE)
        tidligsteInaktiveBehandling.registrertDato = Instant.now().minus(2, ChronoUnit.DAYS)

        replikertBehandling.id shouldBe 0L
        replikertBehandling.tema shouldBe tidligsteInaktiveBehandling.tema
        replikertBehandling.status shouldBe OPPRETTET
        replikertBehandling.dokumentasjonSvarfristDato shouldBe tidligsteInaktiveBehandling.dokumentasjonSvarfristDato
        replikertBehandling.initierendeJournalpostId shouldBe tidligsteInaktiveBehandling.initierendeJournalpostId
        replikertBehandling.behandlingsårsak shouldBe null
        replikertBehandling.registrertDato shouldNotBe tidligsteInaktiveBehandling.registrertDato
        replikertBehandling.mottatteOpplysninger?.mottatteOpplysningerData shouldNotBe null

        replikertBehandling.saksopplysninger.size shouldBe 1
        replikertBehandling.saksopplysninger.all { it.id == null } shouldBe true
        replikertBehandling.saksopplysninger.all { it.behandling == replikertBehandling } shouldBe true
        replikertBehandling.saksopplysninger.all { it.kilder.first().mottattDokument == "dokxml" } shouldBe true
        replikertBehandling.saksopplysninger.all { it.type == SaksopplysningType.INNTK } shouldBe true
        replikertBehandling.saksopplysninger.all { it.endretDato.toString() == "2020-02-11T09:37:30Z" } shouldBe true
        replikertBehandling.saksopplysninger.flatMap { it.kilder }.all { it.id == null } shouldBe true
    }

    @Test
    fun `replikerBehandling utenMottatteOpplysninger blirReplikert`() {
        val tidligsteInaktiveBehandling = opprettBehandlingMedData()
        tidligsteInaktiveBehandling.mottatteOpplysninger = null

        every { behandlingRepository.save(any()) } answers { firstArg() }
        val replikertBehandling = behandlingService.replikerBehandling(tidligsteInaktiveBehandling, Behandlingstyper.NY_VURDERING)

        replikertBehandling.mottatteOpplysninger shouldBe null
    }

    @Test
    fun `avsluttBehandling updates status`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
        }

        every { behandlingRepository.findById(BEHANDLING_ID) } returns Optional.of(behandling)
        every { behandlingRepository.save(any()) } answers { firstArg() }
        every { utkastBrevService.hentUtkast(BEHANDLING_ID) } returns emptyList()
        every { lovligeKombinasjonerSaksbehandlingService.validerNyStatusMulig(any(), AVSLUTTET) } just Runs

        behandlingService.avsluttBehandling(BEHANDLING_ID)

        verify { behandlingRepository.save(any()) }
        verify { applicationEventPublisher.publishEvent(any<BehandlingEndretStatusEvent>()) }

        val savedSlot = slot<Behandling>()
        val eventSlot = slot<BehandlingEndretStatusEvent>()
        verify { behandlingRepository.save(capture(savedSlot)) }
        verify { applicationEventPublisher.publishEvent(capture(eventSlot)) }

        savedSlot.captured.status shouldBe AVSLUTTET
        eventSlot.captured.behandlingID shouldBe BEHANDLING_ID
        eventSlot.captured.behandlingsstatus shouldBe AVSLUTTET
    }

    @Test
    fun `avsluttBehandling kasterFunksjonellException dersomBehandlingErAvsluttet`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
            status = AVSLUTTET
        }

        every { behandlingRepository.findById(BEHANDLING_ID) } returns Optional.of(behandling)

        shouldThrow<FunksjonellException> {
            behandlingService.avsluttBehandling(BEHANDLING_ID)
        }.message shouldContain "Behandling $BEHANDLING_ID er allerede avsluttet!"
    }

    @Test
    fun `avsluttBehandling finnesUtkastBrev kasterFunksjonellException`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
        }

        every { behandlingRepository.findById(BEHANDLING_ID) } returns Optional.of(behandling)
        every { utkastBrevService.hentUtkast(BEHANDLING_ID) } returns listOf(
            UtkastBrev.Builder().behandlingID(BEHANDLING_ID).lagretAvSaksbehandler("test").build()
        )

        shouldThrow<FunksjonellException> {
            behandlingService.avsluttBehandling(BEHANDLING_ID)
        }.message shouldContain "Det finnes et åpent brevutkast. Du må sende eller forkaste brevet før du avslutter behandlingen"
    }

    @Test
    fun `avsluttNyVurdering avslutterBehandlingOgOppdatererBehandlingsresultattype`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
            type = Behandlingstyper.NY_VURDERING
        }

        every { behandlingRepository.findById(BEHANDLING_ID) } returns Optional.of(behandling)
        every { behandlingRepository.save(any()) } answers { firstArg() }
        every { utkastBrevService.hentUtkast(BEHANDLING_ID) } returns emptyList()
        every { lovligeKombinasjonerSaksbehandlingService.validerNyStatusMulig(any(), AVSLUTTET) } just Runs
        every { behandlingsresultatService.oppdaterBehandlingsresultattype(any(), any()) } just Runs

        behandlingService.avsluttAndregangsbehandling(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE_BORTFALT)

        verify { behandlingRepository.save(any()) }
        verify { applicationEventPublisher.publishEvent(any<BehandlingEndretStatusEvent>()) }
        verify { behandlingsresultatService.oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE_BORTFALT) }

        val savedSlot = slot<Behandling>()
        val eventSlot = slot<BehandlingEndretStatusEvent>()
        verify { behandlingRepository.save(capture(savedSlot)) }
        verify { applicationEventPublisher.publishEvent(capture(eventSlot)) }

        savedSlot.captured.status shouldBe AVSLUTTET
        eventSlot.captured.behandlingID shouldBe BEHANDLING_ID
        eventSlot.captured.behandlingsstatus shouldBe AVSLUTTET
    }

    @Test
    fun `avsluttAndregangsbehandling nyVurdering oppdatererBehandlingsresultattype`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
            type = Behandlingstyper.NY_VURDERING
        }

        every { behandlingRepository.findById(BEHANDLING_ID) } returns Optional.of(behandling)
        every { behandlingRepository.save(any()) } answers { firstArg() }
        every { utkastBrevService.hentUtkast(BEHANDLING_ID) } returns emptyList()
        every { lovligeKombinasjonerSaksbehandlingService.validerNyStatusMulig(any(), AVSLUTTET) } just Runs
        every { behandlingsresultatService.oppdaterBehandlingsresultattype(any(), any()) } just Runs

        behandlingService.avsluttAndregangsbehandling(BEHANDLING_ID, Behandlingsresultattyper.FERDIGBEHANDLET)

        verify { behandlingsresultatService.oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.FERDIGBEHANDLET) }
    }

    @Test
    fun `avsluttAndregangsbehandling manglendeInnbetalingTrygdeavgift oppdatererBehandlingsresultattype`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
            type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        }

        every { behandlingRepository.findById(BEHANDLING_ID) } returns Optional.of(behandling)
        every { behandlingRepository.save(any()) } answers { firstArg() }
        every { utkastBrevService.hentUtkast(BEHANDLING_ID) } returns emptyList()
        every { lovligeKombinasjonerSaksbehandlingService.validerNyStatusMulig(any(), AVSLUTTET) } just Runs
        every { behandlingsresultatService.oppdaterBehandlingsresultattype(any(), any()) } just Runs

        behandlingService.avsluttAndregangsbehandling(BEHANDLING_ID, Behandlingsresultattyper.FERDIGBEHANDLET)

        verify { behandlingsresultatService.oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.FERDIGBEHANDLET) }
    }

    @Test
    fun `endreStatus setterSvarFristPåToUker nårNyStatusErAnmodningUnntakSendt`() {
        val behandling = opprettBehandlingUnderBehandling()

        every { behandlingRepository.save(any()) } answers { firstArg() }

        behandlingService.endreStatus(behandling, ANMODNING_UNNTAK_SENDT)

        verify { behandlingRepository.save(any()) }

        val savedSlot = slot<Behandling>()
        verify { behandlingRepository.save(capture(savedSlot)) }

        savedSlot.captured.dokumentasjonSvarfristDato shouldNotBe null
        val forventetInstant = Instant.now().plus(java.time.Period.ofWeeks(2))
        val actualInstant = savedSlot.captured.dokumentasjonSvarfristDato!!
        // Allow 60 seconds tolerance for timing differences
        actualInstant.isAfter(forventetInstant.minusSeconds(60)) shouldBe true
        actualInstant.isBefore(forventetInstant.plusSeconds(60)) shouldBe true
    }

    @Test
    fun `endreBehandlingstema gyldigEndringForSED behandlingLagresOgOppgaveOppdateres`() {
        defaultBehandling = Behandling.forTest {
            id = BEHANDLING_ID
            tema = Behandlingstema.TRYGDETID
            mottatteOpplysninger = MottatteOpplysninger().apply {
                mottatteOpplysningerData = MottatteOpplysningerData()
            }
        }

        every { behandlingRepository.save(any()) } answers { firstArg() }

        behandlingService.endreTema(defaultBehandling, Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET)

        verify(exactly = 0) { applicationEventPublisher.publishEvent(any()) }
        verify { behandlingRepository.save(any()) }

        val savedSlot = slot<Behandling>()
        verify { behandlingRepository.save(capture(savedSlot)) }
        savedSlot.captured.tema shouldBe Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET
        savedSlot.captured.id shouldBe BEHANDLING_ID
    }

    @Test
    fun `oppdaterStatus behIkkeFunnet`() {
        every { behandlingRepository.findById(any()) } returns Optional.empty()

        shouldThrow<IkkeFunnetException> {
            behandlingService.endreStatus(BEHANDLING_ID, AVVENT_DOK_PART)
        }
    }

    @Test
    fun `oppdaterStatus ugyldig`() {
        every { behandlingRepository.findById(any()) } returns Optional.empty()

        shouldThrow<FunksjonellException> {
            behandlingService.endreStatus(BEHANDLING_ID, AVSLUTTET)
        }.message shouldContain "Finner ikke behandling med id $BEHANDLING_ID"
    }

    @Test
    fun `oppdaterStatus statusErAlleredeVurderDokument ingentingSkjer`() {
        val behandling = Behandling.forTest {
            status = VURDER_DOKUMENT
        }

        every { behandlingRepository.findById(any()) } returns Optional.of(behandling)

        behandlingService.endreStatus(BEHANDLING_ID, VURDER_DOKUMENT)

        verify(exactly = 0) { behandlingRepository.save(any()) }
    }

    @Test
    fun `oppdaterStatus statusAvventDok dokumentasjonSvarfristOppdatert`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
            status = VURDER_DOKUMENT
        }

        every { behandlingRepository.findById(any()) } returns Optional.of(behandling)
        every { behandlingRepository.save(any()) } answers { firstArg() }
        every { oppgaveService.oppdaterOppgaveMedSaksnummer(any(), any()) } just Runs
        every { applicationEventPublisher.publishEvent(any()) } just Runs

        behandlingService.endreStatus(BEHANDLING_ID, AVVENT_DOK_PART)

        verify { applicationEventPublisher.publishEvent(any<BehandlingEndretStatusEvent>()) }
        behandling.dokumentasjonSvarfristDato shouldNotBe null
        verify { oppgaveService.oppdaterOppgaveMedSaksnummer(eq(FagsakTestFactory.SAKSNUMMER), any()) }

        val eventSlot = slot<BehandlingEndretStatusEvent>()
        verify { applicationEventPublisher.publishEvent(capture(eventSlot)) }
        eventSlot.captured.behandlingID shouldBe BEHANDLING_ID
        eventSlot.captured.behandlingsstatus shouldBe AVVENT_DOK_PART
    }

    @Test
    fun `oppdaterStatus statusAnmodningUnntakSendt behandlingLagret`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
            status = VURDER_DOKUMENT
        }

        every { behandlingRepository.findById(any()) } returns Optional.of(behandling)
        every { behandlingRepository.save(any()) } answers { firstArg() }
        every { applicationEventPublisher.publishEvent(any()) } just Runs

        behandlingService.endreStatus(BEHANDLING_ID, ANMODNING_UNNTAK_SENDT)

        verify { behandlingRepository.save(behandling) }
        verify { applicationEventPublisher.publishEvent(any<BehandlingEndretStatusEvent>()) }

        val eventSlot = slot<BehandlingEndretStatusEvent>()
        verify { applicationEventPublisher.publishEvent(capture(eventSlot)) }
        eventSlot.captured.behandlingID shouldBe BEHANDLING_ID
        eventSlot.captured.behandlingsstatus shouldBe ANMODNING_UNNTAK_SENDT
    }

    @Test
    fun `endreStatus setterSvarFristPåToUker nårNyStatusErAvventDokPart`() {
        val behandling = opprettBehandlingUnderBehandling()

        every { behandlingRepository.save(any()) } answers { firstArg() }

        behandlingService.endreStatus(behandling, AVVENT_DOK_PART)

        verify { behandlingRepository.save(any()) }
        val savedSlot = slot<Behandling>()
        verify { behandlingRepository.save(capture(savedSlot)) }

        savedSlot.captured.dokumentasjonSvarfristDato shouldNotBe null
        val forventetInstant = Instant.now().plus(java.time.Period.ofWeeks(2))
        val actualInstant = savedSlot.captured.dokumentasjonSvarfristDato!!
        actualInstant.isAfter(forventetInstant.minusSeconds(60)) shouldBe true
        actualInstant.isBefore(forventetInstant.plusSeconds(60)) shouldBe true
    }

    @Test
    fun `endreStatus setterSvarFristPåToUker nårNyStatusErAvventDokUtl`() {
        val behandling = opprettBehandlingUnderBehandling()

        every { behandlingRepository.save(any()) } answers { firstArg() }

        behandlingService.endreStatus(behandling, AVVENT_DOK_UTL)

        verify { behandlingRepository.save(any()) }
        val savedSlot = slot<Behandling>()
        verify { behandlingRepository.save(capture(savedSlot)) }

        savedSlot.captured.dokumentasjonSvarfristDato shouldNotBe null
        val forventetInstant = Instant.now().plus(java.time.Period.ofWeeks(2))
        val actualInstant = savedSlot.captured.dokumentasjonSvarfristDato!!
        actualInstant.isAfter(forventetInstant.minusSeconds(60)) shouldBe true
        actualInstant.isBefore(forventetInstant.plusSeconds(60)) shouldBe true
    }

    @Test
    fun `endreStatus setterSvarFristTilNull nårNyStatusErUnderBehandling`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
            fagsak = FagsakTestFactory.lagFagsak()
            status = AVVENT_DOK_UTL
        }

        every { behandlingRepository.save(any()) } answers { firstArg() }

        behandlingService.endreStatus(behandling, UNDER_BEHANDLING)

        verify { behandlingRepository.save(any()) }
        val savedSlot = slot<Behandling>()
        verify { behandlingRepository.save(capture(savedSlot)) }

        savedSlot.captured.dokumentasjonSvarfristDato shouldBe null
    }

    @Test
    fun `endreBehandlingsstatusFraOpprettetTilUnderBehandling harStatusOpprettet statusBlirSattTilUnderBehandling`() {
        val behandling = Behandling.forTest {
            status = OPPRETTET
        }

        every { behandlingRepository.save(any()) } answers { firstArg() }

        behandlingService.endreBehandlingsstatusFraOpprettetTilUnderBehandling(behandling)

        behandling.status shouldBe UNDER_BEHANDLING
        verify { behandlingRepository.save(behandling) }
    }

    @Test
    fun `endreBehandlingsstatusFraOpprettetTilUnderBehandling harStatusAvventerSvar ingenStatusendring`() {
        val behandling = Behandling.forTest {
            status = AVVENT_DOK_PART
        }

        behandlingService.endreBehandlingsstatusFraOpprettetTilUnderBehandling(behandling)

        verify(exactly = 0) { behandlingRepository.save(any()) }
    }

    @Test
    fun `avsluttAndregangsbehandling kasterFunksjonellException dersomBehandlingTypeIkkeErNyVurderingEllerManglendeInnbetalingTrygdeavgift`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
            status = AVSLUTTET
        }

        every { behandlingRepository.findById(BEHANDLING_ID) } returns Optional.of(behandling)

        shouldThrow<FunksjonellException> {
            behandlingService.avsluttAndregangsbehandling(BEHANDLING_ID, Behandlingsresultattyper.FERDIGBEHANDLET)
        }.message shouldContain "Behandling $BEHANDLING_ID er ikke typen NY_VURDERING eller MANGLENDE_INNBETALING_TRYGDEAVGIFT!"
    }

    @Test
    fun `avsluttAndregangsbehandling kasterFunksjonellException dersomBehandlingErAvsluttet`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
            status = AVSLUTTET
            type = Behandlingstyper.NY_VURDERING
        }

        every { behandlingRepository.findById(BEHANDLING_ID) } returns Optional.of(behandling)

        shouldThrow<FunksjonellException> {
            behandlingService.avsluttAndregangsbehandling(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE_BORTFALT)
        }.message shouldContain "Behandling $BEHANDLING_ID er allerede avsluttet!"
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

    private fun opprettTomBehandlingMedId() = Behandling.forTest {
        id = 665L
    }

    private fun opprettBehandlingUnderBehandling() = Behandling.forTest {
        id = BEHANDLING_ID
        fagsak = FagsakTestFactory.lagFagsak()
        status = UNDER_BEHANDLING
    }

    @Test
    fun `avsluttBehandling med resultattype kan overskrive IKKE_FASTSATT`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
            status = UNDER_BEHANDLING
        }
        val behandlingsresultat = Behandlingsresultat().apply {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.IKKE_FASTSATT
        }

        every { behandlingRepository.findById(BEHANDLING_ID) } returns Optional.of(behandling)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingsresultatService.oppdaterBehandlingsresultattype(any(), any()) } just Runs
        every { behandlingRepository.save(any()) } answers { firstArg() }
        every { utkastBrevService.hentUtkast(BEHANDLING_ID) } returns emptyList()


        behandlingService.avsluttBehandling(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE)


        verify { behandlingsresultatService.oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE) }
        verify { behandlingRepository.save(any()) }
        verify { applicationEventPublisher.publishEvent(any<BehandlingEndretStatusEvent>()) }
    }

    @Test
    fun `avsluttBehandling medBehandlingsresultattype kanIkkeOverskriveAlleredeSetType`() {
        val behandling = Behandling.forTest {
            id = BEHANDLING_ID
            status = UNDER_BEHANDLING
        }
        val behandlingsresultat = Behandlingsresultat().apply {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.AVSLAG_SØKNAD
        }

        every { behandlingRepository.findById(BEHANDLING_ID) } returns Optional.of(behandling)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        shouldThrow<FunksjonellException> {
            behandlingService.avsluttBehandling(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE)
        }.message shouldContain "skal ikke overstyres"

        verify(exactly = 0) { behandlingsresultatService.oppdaterBehandlingsresultattype(any(), any()) }
        verify(exactly = 0) { behandlingRepository.save(any()) }
    }

    companion object {
        private const val BEHANDLING_ID = 11L
        private val BEHANDLING_TYPE = Behandlingstyper.NY_VURDERING
        private val BEHANDLING_TEMA = Behandlingstema.ARBEID_FLERE_LAND
        private val BEHANDLING_STATUS = UNDER_BEHANDLING
        private val MOTTAKSDATO = LocalDate.now().plusMonths(1)
        private val PERIODE_IDS = listOf(2L, 3L)
    }
}

