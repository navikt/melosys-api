package no.nav.melosys.service.journalforing

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.optional.shouldBeEmpty
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakTestFactory.builder
import no.nav.melosys.domain.arkiv.ArkivDokument
import no.nav.melosys.domain.arkiv.BrukerIdType
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.journalfoering.JournalfoeringOpprettRequest
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.felles.dto.SoeknadslandDto
import no.nav.melosys.service.journalforing.dto.*
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.LocalDate
import java.util.*


@ExtendWith(MockKExtension::class)
internal class JournalfoeringServiceTest {
    private val MELOSYS_SAKSNUMMER = "MEL-0123"
    private val RINA_SAKSNUMMER = "22222"
    private val AKTØR_ID = "432537"
    private val ARKIVSAK_ID = 111L
    private val INSTITUSJON_ID = "AB:123"

    @MockK
    private lateinit var joarkFasade: JoarkFasade

    @RelaxedMockK
    private lateinit var prosessinstansService: ProsessinstansService

    @MockK
    private lateinit var eessiService: EessiService

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var persondataFasade: PersondataFasade

    @MockK
    private lateinit var behandlingService: BehandlingService

    @RelaxedMockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var behandlingsresultatRepository: BehandlingsresultatRepository

    @MockK
    private lateinit var utenlandskMyndighetService: UtenlandskMyndighetService

    private lateinit var journalfoeringService: JournalfoeringService

    private lateinit var opprettDto: JournalfoeringOpprettDto
    private lateinit var tilordneDto: JournalfoeringTilordneDto
    private lateinit var journalpost: Journalpost
    private lateinit var journalfoeringSedDto: JournalfoeringSedDto

    private var journalfoeringOpprettRequestSlot = slot<JournalfoeringOpprettRequest>()
    private val unleash = FakeUnleash()

    @BeforeEach
    fun setup() {
        journalpost = Journalpost("123").apply {
            hoveddokument = ArkivDokument()
            forsendelseMottatt = Instant.EPOCH
        }
        opprettDto = JournalfoeringOpprettDto().apply {
            journalpostID = journalpost.journalpostId
            oppgaveID = "setOppgaveID"
            avsenderNavn = "setAvsenderNavn"
            avsenderID = "setAvsenderID"
            avsenderType = Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET
            brukerID = "setBrukerID"
            hoveddokument = DokumentDto("3333", "setDokumenttittel")
            behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
            behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
            fagsak = FagsakDto().apply {
                sakstype = Sakstyper.EU_EOS.kode
                sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode
            }
        }
        tilordneDto = JournalfoeringTilordneDto().apply {
            behandlingstypeKode = Behandlingstyper.ENDRET_PERIODE.kode
            behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
            journalpostID = journalpost.journalpostId
            oppgaveID = "setOppgaveID"
            avsenderNavn = "setAvsenderNavn"
            avsenderID = "setAvsenderID"
            avsenderType = Avsendertyper.PERSON
            brukerID = "setBrukerID"
            hoveddokument = DokumentDto("123", "setDokumenttittel")
        }
        journalfoeringSedDto = JournalfoeringSedDto().apply {
            brukerID = "brukerID"
            journalpostID = "journalpostID"
            oppgaveID = "321"
        }

        SpringSubjectHandler.set(TestSubjectHandler())
        val saksbehandlingRegler = SaksbehandlingRegler(behandlingsresultatRepository, unleash)
        unleash.resetAll()
        val lovligeKombinasjonerSaksbehandlingService = LovligeKombinasjonerSaksbehandlingService(
            fagsakService, behandlingService, behandlingsresultatService, unleash
        )
        val journalfoeringValidering = JournalfoeringValidering(
            lovligeKombinasjonerSaksbehandlingService, eessiService, saksbehandlingRegler, behandlingsresultatService, fagsakService, unleash
        )
        journalfoeringService = JournalfoeringService(
            journalfoeringValidering, joarkFasade, prosessinstansService, eessiService, fagsakService,
            persondataFasade, saksbehandlingRegler, behandlingService, utenlandskMyndighetService
        )
    }

    @Test
    fun finnBrukerIdent_brukerIdentErFolkeregisterident_returnererIdent() {
        journalpost.brukerId = "123"
        journalpost.brukerIdType = BrukerIdType.FOLKEREGISTERIDENT


        journalfoeringService.finnHovedpartIdent(journalpost)
            .shouldBePresent()
            .shouldBe(journalpost.brukerId)
    }

    @Test
    fun finnBrukerIdent_brukerIdentErAktørId_henterIdent() {
        val ident = "123321"
        journalpost.brukerId = AKTØR_ID
        journalpost.brukerIdType = BrukerIdType.AKTØR_ID
        every { persondataFasade.hentFolkeregisterident(journalpost.brukerId) } returns ident

        journalfoeringService.finnHovedpartIdent(journalpost)
            .shouldBePresent()
            .shouldBe(ident)

    }

    @Test
    fun finnBrukerIdent_brukerIdentErOrgnr_returnererOrgnr() {
        journalpost.brukerId = "123"
        journalpost.brukerIdType = BrukerIdType.ORGNR


        journalfoeringService.finnHovedpartIdent(journalpost)
            .shouldBePresent()
            .shouldBe(journalpost.brukerId)
    }

    @Test
    fun finnBrukerIdent_brukerErNull_returnererIngenting() {
        journalfoeringService.finnHovedpartIdent(journalpost)
            .shouldBeEmpty()
    }

    @Test
    fun journalførOgOpprettSak_ikkeSed_prosessinstansBlirOpprettet() {
        opprettDto.fagsak = lagFagsakDto(LocalDate.MIN, LocalDate.MAX, "DK", Sakstyper.EU_EOS)
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost
        every { utenlandskMyndighetService.finnInstitusjonID(any()) } returns Optional.empty()


        journalfoeringService.journalførOgOpprettSak(opprettDto)


        verify {
            prosessinstansService.opprettProsessinstansJournalføringNySak(
                opprettDto.tilJournalfoeringOpprettRequest(), ProsessType.JFR_NY_SAK_BRUKER, true, LocalDate.EPOCH,
                Behandlingsaarsaktyper.SØKNAD, null, journalpost.mottaksKanalErElektronisk()
            )
        }
    }

    @Test
    fun journalførOgOpprettSak_medVirksomhetOrgnr_oppretterKorrektProsessinstans() {
        opprettDto.apply {
            fagsak = lagFagsakDto(LocalDate.MIN, LocalDate.MAX, "DK", Sakstyper.EU_EOS)
            brukerID = null
            virksomhetOrgnr = "orgnr"
            behandlingstemaKode = Behandlingstema.VIRKSOMHET.kode
            behandlingstypeKode = Behandlingstyper.HENVENDELSE.kode
        }
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost
        every { utenlandskMyndighetService.finnInstitusjonID(any()) } returns Optional.of(INSTITUSJON_ID)


        journalfoeringService.journalførOgOpprettSak(opprettDto)


        verify {
            prosessinstansService.opprettProsessinstansJournalføringNySak(
                opprettDto.tilJournalfoeringOpprettRequest(), ProsessType.JFR_NY_SAK_VIRKSOMHET, false, LocalDate.EPOCH,
                Behandlingsaarsaktyper.HENVENDELSE, INSTITUSJON_ID, journalpost.mottaksKanalErElektronisk()
            )
        }
    }

    @Test
    fun journalførOgOpprettSak_ugyldigBehandlingstypeOgSakstema_nårSenderForvaltningsmelding_kasterException() {
        opprettDto.behandlingstypeKode = Behandlingstyper.KLAGE.kode
        opprettDto.forvaltningsmeldingMottaker = ForvaltningsmeldingMottaker.BRUKER
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost


        shouldThrow<FunksjonellException> { journalfoeringService.journalførOgOpprettSak(opprettDto) }
            .message.shouldBe("Kan kun sende forvaltningsmelding for behandlingtyper: FØRSTEGANG og NY_VURDERING og sakstema: MEDLEMSKAP_LOVVALG")
    }

    @Test
    fun journalførOgKnyttTilEksisterendeSak_ugyldigBehandlingstypeOgSakstema_nårSenderForvaltningsmelding_kasterException() {
        tilordneDto.apply {
            behandlingstypeKode = Behandlingstyper.KLAGE.kode
            saksnummer = "123"
            forvaltningsmeldingMottaker = ForvaltningsmeldingMottaker.BRUKER
        }
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost
        every { fagsakService.hentFagsak(tilordneDto.saksnummer) } returns lagFagsak(lagBehandling()).apply { tema = Sakstemaer.UNNTAK }


        shouldThrow<FunksjonellException> { journalfoeringService.journalførOgKnyttTilEksisterendeSak(tilordneDto) }
            .message.shouldBe("Kan kun sende forvaltningsmelding for behandlingtyper: FØRSTEGANG og NY_VURDERING og sakstema: MEDLEMSKAP_LOVVALG")
    }

    @Test
    fun journalførOgOpprettAndregangsBehandling_lukkerIkkeÅrsavregningBehandling() {
        tilordneDto.saksnummer = MELOSYS_SAKSNUMMER
        tilordneDto.behandlingstypeKode = Behandlingstyper.ÅRSAVREGNING.kode
        tilordneDto.behandlingstemaKode = Behandlingstema.YRKESAKTIV.kode
        val årsavregningBehandling = lagBehandling().apply {
            status = Behandlingsstatus.UNDER_BEHANDLING
            type = Behandlingstyper.ÅRSAVREGNING
            tema = Behandlingstema.YRKESAKTIV
            registrertDato = Instant.now()
        }
        val fagsak = builder()
            .saksnummer(MELOSYS_SAKSNUMMER)
            .behandlinger(årsavregningBehandling)
            .type(Sakstyper.FTRL)
            .tema(Sakstemaer.TRYGDEAVGIFT)
            .build()
            .apply {
                leggTilAktør(Aktoer().apply { rolle = Aktoersroller.BRUKER })
                årsavregningBehandling.fagsak = this
            }
        every { joarkFasade.hentJournalpost(tilordneDto.journalpostID) } returns journalpost
        every { fagsakService.hentFagsak(MELOSYS_SAKSNUMMER) } returns fagsak
        every { utenlandskMyndighetService.finnInstitusjonID(any()) } returns Optional.empty()

        journalfoeringService.journalførOgOpprettAndregangsBehandling(tilordneDto)

        verify(exactly = 0) { behandlingService.avsluttBehandling(any()) }
    }

    private fun lagFagsakDto(fom: LocalDate?, tom: LocalDate?, land: String?, sakstype: Sakstyper): FagsakDto =
        FagsakDto().apply {
            this.sakstype = sakstype.kode
            this.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode
            this.soknadsperiode = PeriodeDto().apply {
                this.fom = fom
                this.tom = tom
            }
            this.land = SoeknadslandDto(listOf(land), false)
        }


    private fun lagFagsak(saksnummer: String, behandling: Behandling): Fagsak =
        builder().saksnummer(saksnummer).behandlinger(behandling).build().apply {
            behandling.fagsak = this
        }

    private fun lagFagsak(behandling: Behandling): Fagsak = lagFagsak(MELOSYS_SAKSNUMMER, behandling)

    private fun lagBehandling(): Behandling =
        Behandling().apply {
            id = 1L
            status = Behandlingsstatus.AVSLUTTET
        }
}

