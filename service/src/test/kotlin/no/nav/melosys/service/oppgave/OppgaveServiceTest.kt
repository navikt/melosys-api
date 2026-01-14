package no.nav.melosys.service.oppgave

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.optional.shouldBeEmpty
import io.kotest.matchers.optional.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.personDokumentForTest
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.anmodningEllerAttest
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie
import no.nav.melosys.domain.mottatteopplysninger.mottatteOpplysningerForTest
import no.nav.melosys.domain.mottatteopplysninger.soeknad
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.behandling.UtledMottaksdato
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.oppgave.dto.BehandlingsoppgaveDto
import no.nav.melosys.service.oppgave.dto.JournalfoeringsoppgaveDto
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@ExtendWith(MockKExtension::class)
internal class OppgaveServiceTest {

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var oppgaveFasade: OppgaveFasade

    @MockK
    private lateinit var persondataFasade: PersondataFasade

    @MockK
    private lateinit var eregFasade: EregFasade

    @MockK
    private lateinit var utledMottaksdato: UtledMottaksdato

    @MockK
    private lateinit var saksopplysningerService: SaksopplysningerService

    @MockK
    private lateinit var mottatteOpplysningerService: MottatteOpplysningerService

    private val oppgaveOppdateringSlot = slot<OppgaveOppdatering>()
    private val oppgaveSlot = slot<Oppgave>()

    private lateinit var oppgave: Oppgave

    private val oppgaveFactory = OppgaveFactory()

    private lateinit var oppgaveService: OppgaveService

    @BeforeEach
    fun setUp() {
        oppgaveService = OppgaveService(
            behandlingService,
            behandlingsresultatService,
            fagsakService,
            oppgaveFasade,
            saksopplysningerService,
            mottatteOpplysningerService,
            persondataFasade,
            eregFasade,
            utledMottaksdato,
            oppgaveFactory
        )
        oppgave = Oppgave.Builder()
            .setOppgavetype(Oppgavetyper.BEH_SAK_MK)
            .setTilordnetRessurs(TILORDNET_RESSURS)
            .setOppgaveId(BEH_OPPG_ID)
            .setStatus("AAPNET")
            .setSaksnummer(FagsakTestFactory.SAKSNUMMER)
            .build()

        every { saksopplysningerService.finnSedOpplysninger(any()) } returns Optional.empty()
        every { oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(FagsakTestFactory.SAKSNUMMER) } returns listOf(oppgave)
        every { oppgaveFasade.oppdaterOppgave(any(), any()) } returns Unit
        every { oppgaveFasade.opprettOppgave(any<Oppgave>()) } returns BEH_OPPG_ID
        every { oppgaveFasade.opprettSensitivOppgave(any<Oppgave>()) } returns BEH_OPPG_ID
        every { persondataFasade.harStrengtFortroligAdresse(any()) } returns false
    }

    @Test
    fun hentOppgaverMedAnsvarlig() {
        val oppgave1 = Oppgave.Builder()
            .setOppgaveId(BEH_OPPG_ID)
            .setOppgavetype(Oppgavetyper.BEH_SAK_MK)
            .setSaksnummer(FagsakTestFactory.SAKSNUMMER)
        val oppgave2 = Oppgave.Builder()
            .setJournalpostId(JFR_OPPG_JPID)
            .setOppgaveId(JFR_OPPG_ID)
            .setOppgavetype(Oppgavetyper.JFR)
        val oppgaver = setOf(oppgave1.build(), oppgave2.build())
        val behandling = lagBehandling(oppgaveId = BEH_OPPG_ID)
        val fagsak = Fagsak.forTest {
            behandlinger(behandling)
        }
        every { oppgaveFasade.finnOppgaverMedAnsvarlig(TILORDNET_RESSURS) } returns oppgaver
        every { behandlingService.hentBehandling(any<Long>()) } returns behandling
        every { fagsakService.hentFagsak(any<String>()) } returns fagsak
        every { mottatteOpplysningerService.finnMottatteOpplysninger(behandling.id) } returns Optional.of(lagMottatteOpplysninger())


        val mineSaker = oppgaveService.hentOppgaverMedAnsvarlig(TILORDNET_RESSURS)


        mineSaker.shouldHaveSize(2)
        mineSaker.first { it.oppgaveID == BEH_OPPG_ID }
            .shouldNotBeNull()
            .shouldBeInstanceOf<BehandlingsoppgaveDto>()
            .apply {
                this.behandling.behandlingID.shouldBe(behandling.id)
                this.land.shouldNotBeNull()
            }
        mineSaker.first { it.oppgaveID == JFR_OPPG_ID }
            .shouldNotBeNull()
            .shouldBeInstanceOf<JournalfoeringsoppgaveDto>()
    }

    @Test
    fun hentOppgaverMedAnsvarlig_mottatteopplysningerFinnesIkke_mappesKorrekt() {
        val behandling = lagBehandling(oppgaveId = BEH_OPPG_ID)
        val fagsak = Fagsak.forTest {
            behandlinger(behandling)
        }
        every { oppgaveFasade.finnOppgaverMedAnsvarlig(TILORDNET_RESSURS) } returns setOf(oppgave)
        every { behandlingService.hentBehandling(any<Long>()) } returns behandling
        every { fagsakService.hentFagsak(any<String>()) } returns fagsak
        every { mottatteOpplysningerService.finnMottatteOpplysninger(behandling.id) } returns Optional.empty()

        val mineSaker = oppgaveService.hentOppgaverMedAnsvarlig(TILORDNET_RESSURS)

        mineSaker
            .shouldHaveSize(1)
            .first { it.oppgaveID == BEH_OPPG_ID }
            .shouldNotBeNull()
            .shouldBeInstanceOf<BehandlingsoppgaveDto>()
            .apply {
                this.behandling.behandlingID.shouldBe(behandling.id)
                this.land.shouldBeNull()
            }
    }

    @Test
    fun hentOppgaverMedAnsvarlig_mottatteopplysningerDataErAnmodningEllerAttest_mappesKorrekt() {
        val behandling = lagBehandling(oppgaveId = BEH_OPPG_ID)
        val fagsak = Fagsak.forTest {
            behandlinger(behandling)
        }
        val mottatteOpplysninger = mottatteOpplysningerForTest {
            anmodningEllerAttest { }
        }
        every { oppgaveFasade.finnOppgaverMedAnsvarlig(TILORDNET_RESSURS) } returns setOf(oppgave)
        every { behandlingService.hentBehandling(any<Long>()) } returns behandling
        every { fagsakService.hentFagsak(any<String>()) } returns fagsak
        every { mottatteOpplysningerService.finnMottatteOpplysninger(behandling.id) } returns Optional.of(mottatteOpplysninger)

        val mineSaker = oppgaveService.hentOppgaverMedAnsvarlig(TILORDNET_RESSURS)

        mineSaker
            .shouldHaveSize(1)
            .first { it.oppgaveID == BEH_OPPG_ID }
            .shouldNotBeNull()
            .shouldBeInstanceOf<BehandlingsoppgaveDto>()
            .apply {
                this.behandling.behandlingID.shouldBe(behandling.id)
            }
    }

    @Test
    fun hentOppgaverMedAnsvarlig_notaterEksisterer_forventSisteNotat() {
        val notatTidspunkt1 = Instant.now()
        val notatTidspunkt2 = notatTidspunkt1.plusMillis(2000)
        val behandling = lagBehandling(
            oppgaveId = BEH_OPPG_ID,
            behandlingsnotater = mutableSetOf(
                Behandlingsnotat().apply {
                    registrertDato = notatTidspunkt1
                    tekst = "Test1"
                },
                Behandlingsnotat().apply {
                    registrertDato = notatTidspunkt2
                    tekst = "Test2"
                }
            )
        )
        val fagsak = Fagsak.forTest {
            behandlinger(behandling)
        }
        every { oppgaveFasade.finnOppgaverMedAnsvarlig(TILORDNET_RESSURS) } returns setOf(oppgave)
        every { behandlingService.hentBehandling(any<Long>()) } returns behandling
        every { fagsakService.hentFagsak(any<String>()) } returns fagsak
        every { mottatteOpplysningerService.finnMottatteOpplysninger(behandling.id) } returns Optional.empty()

        val mineSaker = oppgaveService.hentOppgaverMedAnsvarlig(TILORDNET_RESSURS)

        mineSaker
            .shouldHaveSize(1)
            .first()
            .shouldNotBeNull()
            .shouldBeInstanceOf<BehandlingsoppgaveDto>()
            .apply {
                sisteNotat.shouldBe("Test2")
            }
    }

    @Test
    fun hentOppgaverMedAnsvarlig_aktøridOgOrgnrErNull_forventUkjentIdOgNavn() {
        val oppgave = Oppgave.Builder()
            .setOppgaveId(JFR_OPPG_ID)
            .setJournalpostId(JFR_OPPG_JPID)
            .setOppgavetype(Oppgavetyper.JFR)
            .setAktørId(null)
            .setOrgnr(null)
            .build()
        every { oppgaveFasade.finnOppgaverMedAnsvarlig(TILORDNET_RESSURS) } returns setOf(oppgave)

        val mineSaker = oppgaveService.hentOppgaverMedAnsvarlig(TILORDNET_RESSURS)

        mineSaker.shouldHaveSize(1)
            .first()
            .shouldNotBeNull()
            .apply {
                hovedpartIdent.shouldBe("UKJENT")
                navn.shouldBe("UKJENT")
            }
    }

    @Test
    fun hentOppgaverMedAnsvarlig_aktørIdEksisterer_forventFnrOgSammensattNavn() {
        val oppgave = Oppgave.Builder()
            .setOppgaveId(JFR_OPPG_ID)
            .setJournalpostId(JFR_OPPG_JPID)
            .setOppgavetype(Oppgavetyper.JFR)
            .setAktørId("1111")
            .setOrgnr(null)
            .build()
        every { oppgaveFasade.finnOppgaverMedAnsvarlig(TILORDNET_RESSURS) } returns setOf(oppgave)
        every { persondataFasade.finnFolkeregisterident("1111") } returns Optional.of("fnr")
        every { persondataFasade.hentSammensattNavn("fnr") } returns "sammensatt navn"

        val mineSaker = oppgaveService.hentOppgaverMedAnsvarlig(TILORDNET_RESSURS)

        mineSaker
            .shouldHaveSize(1)
            .first()
            .shouldNotBeNull()
            .apply {
                hovedpartIdent.shouldBe("fnr")
                navn.shouldBe("sammensatt navn")
            }
    }

    @Test
    fun hentOppgaverMedAnsvarlig_orgnrEksisterer_forventOrgnrOgNavn() {
        val oppgave = Oppgave.Builder()
            .setOppgaveId(JFR_OPPG_ID)
            .setJournalpostId(JFR_OPPG_JPID)
            .setOppgavetype(Oppgavetyper.JFR)
            .setAktivDato(null)
            .setOrgnr("2222")
            .build()
        every { oppgaveFasade.finnOppgaverMedAnsvarlig(TILORDNET_RESSURS) } returns setOf(oppgave)
        every { eregFasade.hentOrganisasjonNavn("2222") } returns "organisasjonsnavn"

        val mineSaker = oppgaveService.hentOppgaverMedAnsvarlig(TILORDNET_RESSURS)

        mineSaker
            .shouldHaveSize(1)
            .first()
            .shouldNotBeNull()
            .apply {
                hovedpartIdent.shouldBe("2222")
                navn.shouldBe("organisasjonsnavn")
            }
    }

    @Test
    fun hentOppgaveForFagsaksnummer_oppgaveEksisterer_forventOppgave() {
        val oppgave = oppgaveService.hentÅpenBehandlingsoppgaveMedFagsaksnummer(FagsakTestFactory.SAKSNUMMER)

        oppgave.erBehandling().shouldBeTrue()
    }

    @Test
    fun finnÅpenBehandlingsoppgaveMedFagsaksnummer_returnererOppgaveViStøtter_filtrererIkkeUtOppgave() {
        every { oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(FagsakTestFactory.SAKSNUMMER) } returns
            listOf(
                Oppgave.Builder()
                    .setTema(Tema.MED)
                    .setOppgavetype(Oppgavetyper.BEH_SAK)
                    .build()
            )

        val oppgave = oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(FagsakTestFactory.SAKSNUMMER)

        oppgave.shouldNotBeEmpty()
    }

    @Test
    fun finnÅpenBehandlingsoppgaveMedFagsaksnummer_returnererTrygdeavgiftOppgave_filtrererUtOppgave() {
        every { oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(FagsakTestFactory.SAKSNUMMER) } returns
            listOf(
                Oppgave.Builder()
                    .setTema(Tema.TRY)
                    .setOppgavetype(Oppgavetyper.VUR)
                    .build()
            )

        val oppgave = oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(FagsakTestFactory.SAKSNUMMER)

        oppgave.shouldBeEmpty()
    }

    @Test
    fun opprettEllerGjenBrukBehandlingsoppgave_medEksisterendeÅrsavregningOppgave_ÅrsavregningoppgaveBlirOpprettet() {
        val behandling = Behandling.forTest {
            id = 1L
            fagsak { medBruker() }
            type = Behandlingstyper.ÅRSAVREGNING
            tema = Behandlingstema.YRKESAKTIV
        }

        every { oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(FagsakTestFactory.SAKSNUMMER) } returns listOf(oppgave)
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns behandling
        every { behandlingService.hentBehandling(any<Long>()) } returns behandling
        every { utledMottaksdato.getMottaksdato(behandling) } returns LocalDate.now()
        every { behandlingService.lagre(behandling) } returns Unit


        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", TILORDNET_RESSURS)


        verify { oppgaveFasade.opprettOppgave(any()) }
        verify(exactly = 0) { oppgaveFasade.opprettSensitivOppgave(any()) }
        verify { behandlingService.lagre(behandling) }
        behandling.oppgaveId!! shouldBeEqual oppgave.oppgaveId
    }

    @Test
    fun opprettEllerGjenbrukBehandlingsoppgave_ingenEksisterendeOppgave_oppgaveBlirOpprettet() {
        val behandling = lagBehandling()
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns behandling
        every { behandlingService.hentBehandling(any<Long>()) } returns behandling
        every { utledMottaksdato.getMottaksdato(behandling) } returns LocalDate.now()
        every { oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(FagsakTestFactory.SAKSNUMMER) } returns emptyList()
        every { behandlingService.lagre(behandling) } returns Unit

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", "Z99999")

        verify { oppgaveFasade.opprettOppgave(any()) }
        verify(exactly = 0) { oppgaveFasade.opprettSensitivOppgave(any()) }
        verify { behandlingService.lagre(behandling) }
        behandling.oppgaveId!! shouldBeEqual BEH_OPPG_ID
    }

    @Test
    fun opprettEllerGjenbrukBehandlingsoppgave_oppgaveErFerdigstilt_oppgaveBlirOpprettet() {
        oppgave = Oppgave.Builder()
            .setOppgavetype(Oppgavetyper.BEH_SAK_MK)
            .setTilordnetRessurs(TILORDNET_RESSURS)
            .setOppgaveId(BEH_OPPG_ID)
            .setStatus("FERDIGSTILT")
            .setSaksnummer(FagsakTestFactory.SAKSNUMMER)
            .build()

        every { oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(FagsakTestFactory.SAKSNUMMER) } returns listOf(oppgave)

        val behandling = lagBehandling()
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns behandling
        every { behandlingService.hentBehandling(any<Long>()) } returns behandling
        every { utledMottaksdato.getMottaksdato(behandling) } returns LocalDate.now()
        every { behandlingService.lagre(behandling) } returns Unit

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", "Z99999")

        verify { oppgaveFasade.opprettOppgave(any()) }
        verify(exactly = 0) { oppgaveFasade.opprettSensitivOppgave(any()) }
        verify { behandlingService.lagre(behandling) }
        behandling.oppgaveId!! shouldBeEqual BEH_OPPG_ID
    }

    @Test
    fun opprettEllerGjenbrukBehandlingsoppgave_oppgaveOpprettElektroniskSøknad_oppgaveBlirOpprettetMedBeskrivelse() {
        val behandling = lagBehandling {
            mottatteOpplysninger {
                type = Mottatteopplysningertyper.SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS
                soeknad { }
            }
        }
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns behandling
        every { behandlingService.hentBehandling(any<Long>()) } returns behandling
        every { utledMottaksdato.getMottaksdato(behandling) } returns LocalDate.now()
        every { oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(FagsakTestFactory.SAKSNUMMER) } returns emptyList()
        every { behandlingService.lagre(behandling) } returns Unit

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", "Z99999")

        verify { oppgaveFasade.opprettOppgave(capture(oppgaveSlot)) }
        oppgaveSlot.captured.beskrivelse.shouldBe(behandling.tema.beskrivelse)
        verify { behandlingService.lagre(behandling) }
        behandling.oppgaveId!! shouldBeEqual BEH_OPPG_ID
    }

    @Test
    fun opprettEllerGjenbrukBehandlingsoppgave_oppgaveNyVurdering_oppgaveBlirOpprettetMedBeskrivelse() {
        val behandling = lagBehandling { type = Behandlingstyper.NY_VURDERING }
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns behandling
        every { behandlingService.hentBehandling(any<Long>()) } returns behandling
        every { utledMottaksdato.getMottaksdato(behandling) } returns LocalDate.now()
        every { oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(FagsakTestFactory.SAKSNUMMER) } returns emptyList()
        every { behandlingService.lagre(behandling) } returns Unit

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", "Z99999")

        verify { oppgaveFasade.opprettOppgave(capture(oppgaveSlot)) }
        oppgaveSlot.captured.beskrivelse.shouldBe(behandling.tema.beskrivelse)
    }

    @Test
    fun opprettEllerGjenbrukBehandlingsoppgave_oppgaveEksistererSaksbehandlerErTilordnet_oppgaveBlirIkkeOpprettetEllerOppdatert() {
        val behandling = Behandling.forTest {
            type = Behandlingstyper.FØRSTEGANG
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            fagsak = Fagsak.forTest()
        }
        every { behandlingService.hentBehandling(any<Long>()) } returns behandling
        every { behandlingService.lagre(behandling) } returns Unit

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", oppgave.tilordnetRessurs)

        verify(exactly = 0) { oppgaveFasade.opprettOppgave(any()) }
        verify(exactly = 0) { oppgaveFasade.oppdaterOppgave(any(), any()) }
    }

    @Test
    fun opprettEllerGjenbrukBehandlingsoppgave_oppgaveEksistererTilordnetAnnenRessurs_oppdaterTilordnetRessurs() {
        val tilordnetRessurs = "Z12332123"
        val behandling = Behandling.forTest {
            type = Behandlingstyper.FØRSTEGANG
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            fagsak = Fagsak.forTest()
        }
        every { behandlingService.hentBehandling(any<Long>()) } returns behandling
        every { oppgaveFasade.oppdaterOppgave(any(), any()) } returns Unit
        every { behandlingService.lagre(behandling) } returns Unit

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", tilordnetRessurs)

        verify(exactly = 0) { oppgaveFasade.opprettOppgave(any()) }
        verify { oppgaveFasade.oppdaterOppgave(eq(oppgave.oppgaveId), capture(oppgaveOppdateringSlot)) }
        oppgaveOppdateringSlot.captured.tilordnetRessurs.shouldBe(tilordnetRessurs)
    }

    @Test
    fun opprettEllerGjenbrukBehandlingsoppgave_personHarBeskyttelsesbehov_sensitivOppgaveBlirOpprettet() {
        val behandling = lagBehandling()
        every { behandlingService.hentBehandling(any<Long>()) } returns behandling
        every { persondataFasade.harStrengtFortroligAdresse(FagsakTestFactory.BRUKER_AKTØR_ID) } returns true
        every { utledMottaksdato.getMottaksdato(behandling) } returns LocalDate.now()
        every { oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(FagsakTestFactory.SAKSNUMMER) } returns emptyList()
        every { behandlingService.lagre(behandling) } returns Unit

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", "Z99999")

        verify(exactly = 0) { oppgaveFasade.opprettOppgave(any<Oppgave>()) }
        verify { oppgaveFasade.opprettSensitivOppgave(any()) }
    }

    @Test
    fun opprettEllerGjenbrukBehandlingsoppgave_barnHarBeskyttelsesbehov_sensitivOppgaveBlirOpprettet() {
        val behandling = lagBehandling {
            mottatteOpplysninger {
                soeknad {
                    // personOpplysninger.medfolgendeFamilie is set via custom logic
                }
            }
        }
        // Add medfolgendeFamilie to the søknad data - this requires a custom approach since it's deeply nested
        behandling.mottatteOpplysninger?.mottatteOpplysningerData?.personOpplysninger?.medfolgendeFamilie =
            listOf(MedfolgendeFamilie.tilBarnFraFnrOgNavn("fnrBarn", null))

        every { persondataFasade.harStrengtFortroligAdresse(FagsakTestFactory.BRUKER_AKTØR_ID) } returns false
        every { persondataFasade.harStrengtFortroligAdresse("fnrBarn") } returns true
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns behandling
        every { behandlingService.hentBehandling(any<Long>()) } returns behandling
        every { utledMottaksdato.getMottaksdato(behandling) } returns LocalDate.now()
        every { oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(FagsakTestFactory.SAKSNUMMER) } returns emptyList()
        every { behandlingService.lagre(behandling) } returns Unit

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(behandling, "222", "333", "Z99999")

        verify(exactly = 0) { oppgaveFasade.opprettOppgave(any<Oppgave>()) }
        verify { oppgaveFasade.opprettSensitivOppgave(any()) }
    }

    @Test
    fun opprettOppgaveForSak_oppretterNyOppgaveForFagsak() {
        val behandling = lagBehandling()
        val oppgave1 = Oppgave.Builder()
            .setTilordnetRessurs("tilordnet ressurs 1")
            .setOpprettetTidspunkt(LocalDate.now().atStartOfDay(ZoneId.systemDefault())).setStatus("FERDIGSTILT")
            .build()
        val oppgave2 = Oppgave.Builder()
            .setTilordnetRessurs("tilordnet ressurs 2")
            .setOpprettetTidspunkt(LocalDate.now().minusDays(2).atStartOfDay(ZoneId.systemDefault()))
            .setStatus("FERDIGSTILT").build()
        every { persondataFasade.harStrengtFortroligAdresse(FagsakTestFactory.BRUKER_AKTØR_ID) } returns false
        every { behandlingService.hentBehandlingMedSaksopplysninger(any<Long>()) } returns behandling
        every { behandlingService.hentBehandling(any<Long>()) } returns behandling
        every { oppgaveFasade.finnAvsluttetBehandlingsoppgaverMedSaksnummer(FagsakTestFactory.SAKSNUMMER) } returns listOf(oppgave1, oppgave2)
        every { oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(FagsakTestFactory.SAKSNUMMER) } returns emptyList()
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns behandling.fagsak
        every { utledMottaksdato.getMottaksdato(behandling) } returns LocalDate.now()
        every { behandlingService.lagre(behandling) } returns Unit

        oppgaveService.opprettOppgaveForSak(FagsakTestFactory.SAKSNUMMER)

        verify { oppgaveFasade.opprettOppgave(capture(oppgaveSlot)) }
        oppgaveSlot.captured.apply {
            saksnummer.shouldBe(FagsakTestFactory.SAKSNUMMER)
            tilordnetRessurs.shouldBe(oppgave1.tilordnetRessurs)
        }
    }

    @Test
    fun saksbehandlerErTilordnetOppgaveForSaksnummer_erTilordnet_erSann() {
        val saksnummer = "MEL-test"
        val saksbehandler = "Z12111"
        val behandling = lagBehandling(oppgaveId = "1")
        val oppgave = Oppgave.Builder().setTilordnetRessurs(saksbehandler).setOppgaveId("1").build()
        every { oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(saksnummer) } returns listOf(oppgave)
        every { behandlingService.hentBehandling(behandling.id) } returns behandling

        oppgaveService.saksbehandlerErTilordnetOppgaveForBehandling(saksbehandler, behandling.id)
            .shouldBeTrue()
    }

    @Test
    fun saksbehandlerErTilordnetOppgaveForSaksnummer_erIkkeTilordnet_erIkkeSann() {
        val saksnummer = "MEL-test"
        val saksbehandler = "Z12111"
        val behandling = lagBehandling(oppgaveId = "1")
        val oppgave = Oppgave.Builder().setOppgaveId("1").build()
        every { oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(saksnummer) } returns listOf(oppgave)
        every { behandlingService.hentBehandling(behandling.id) } returns behandling

        oppgaveService.saksbehandlerErTilordnetOppgaveForBehandling(saksbehandler, behandling.id)
            .shouldBeFalse()
    }

    @Test
    fun saksbehandlerErTilordnetOppgaveForSaksnummer_finnesIngenOppgaver_erIkkeSann() {
        val saksnummer = "MEL-test"
        val behandling = lagBehandling()
        every { oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(saksnummer) } returns emptyList()
        every { behandlingService.hentBehandling(behandling.id) } returns behandling

        oppgaveService.saksbehandlerErTilordnetOppgaveForBehandling("Z12111", behandling.id)
            .shouldBeFalse()
    }

    companion object {
        private const val BEH_OPPG_ID = "1"
        private const val JFR_OPPG_ID = "2"
        private const val JFR_OPPG_JPID = "02"
        private const val TILORDNET_RESSURS = "Z123456"

        private fun lagBehandling(
            oppgaveId: String? = null,
            behandlingsnotater: MutableSet<Behandlingsnotat> = mutableSetOf(),
            init: BehandlingTestFactory.BehandlingTestBuilder.() -> Unit = {}
        ): Behandling = Behandling.forTest {
            id = 1L
            fagsak { medBruker() }
            type = Behandlingstyper.FØRSTEGANG
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            registrertDato = Instant.ofEpochMilli(111L)
            endretDato = Instant.ofEpochMilli(222L)
            saksopplysning {
                type = SaksopplysningType.PERSOPL
                personDokument {
                    fnr = "fnr"
                    sammensattNavn = "sammensattNavn"
                }
            }
            dokumentasjonSvarfristDato = Instant.ofEpochMilli(333L)
            status = Behandlingsstatus.OPPRETTET
            init()
        }.apply {
            this.oppgaveId = oppgaveId
            this.behandlingsnotater = behandlingsnotater
        }

        private fun lagMottatteOpplysninger() = mottatteOpplysningerForTest {
            soeknad {
                landkoder(Landkoder.BE.kode)
                fysiskeArbeidssted {
                    landkode = Landkoder.NO.kode
                }
            }
        }
    }
}
