package no.nav.melosys.service.journalforing

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.optional.shouldBeEmpty
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.FagsakTestFactory.builder
import no.nav.melosys.domain.FagsakTestFactory.lagFagsak
import no.nav.melosys.domain.arkiv.ArkivDokument
import no.nav.melosys.domain.arkiv.BrukerIdType
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.*
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
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

        val unleash = FakeUnleash()
        val saksbehandlingRegler = SaksbehandlingRegler(behandlingsresultatRepository, unleash)
        val lovligeKombinasjonerSaksbehandlingService = LovligeKombinasjonerSaksbehandlingService(
            fagsakService, behandlingService, behandlingsresultatService, unleash
        )
        val journalfoeringValidering = JournalfoeringValidering(
            lovligeKombinasjonerSaksbehandlingService, eessiService, saksbehandlingRegler, behandlingsresultatService, fagsakService
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
                Behandlingsaarsaktyper.SØKNAD, null, journalpost.mottaksKanalErEessi()
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
                Behandlingsaarsaktyper.HENVENDELSE, INSTITUSJON_ID, journalpost.mottaksKanalErEessi()
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
    fun journalførOgOpprettAndregangsBehandling_ugyldigAktoersrolle_nårSenderForvaltningsmelding_kasterException() {
        tilordneDto.apply {
            behandlingstypeKode = Behandlingstyper.NY_VURDERING.kode
            saksnummer = "123"
            brukerID = null
            forvaltningsmeldingMottaker = ForvaltningsmeldingMottaker.BRUKER
        }
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost
        every { fagsakService.hentFagsak(tilordneDto.saksnummer) } returns lagFagsak(lagBehandling()).apply { tema = Sakstemaer.MEDLEMSKAP_LOVVALG }


        shouldThrow<FunksjonellException> { journalfoeringService.journalførOgKnyttTilEksisterendeSak(tilordneDto) }
            .message.shouldBe("Kan kun sende forvaltningsmelding for Aktoersroller: BRUKER")
    }

    @Test
    fun journalførOgOpprettSak_ugyldigAktoersrolle_nårSenderForvaltningsmelding_kasterException() {
        opprettDto.apply {
            fagsak = lagFagsakDto(LocalDate.MIN, LocalDate.MAX, "DK", Sakstyper.EU_EOS).apply { sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode }
            brukerID = null
            behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
            forvaltningsmeldingMottaker = ForvaltningsmeldingMottaker.BRUKER
        }
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost


        shouldThrow<FunksjonellException> { journalfoeringService.journalførOgOpprettSak(opprettDto) }
            .message.shouldBe("Kan kun sende forvaltningsmelding for Aktoersroller: BRUKER")
    }

    @Test
    fun journalførOgOpprettSak_gyldigSkalSendeForvaltningsmeldingKasterIkkeFeilUnderValidering() {
        opprettDto.apply {
            fagsak = lagFagsakDto(LocalDate.MIN, LocalDate.MAX, "DK", Sakstyper.EU_EOS).apply { sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode }
            behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
            behandlingstemaKode = Behandlingstema.UTSENDT_SELVSTENDIG.kode
            forvaltningsmeldingMottaker = ForvaltningsmeldingMottaker.BRUKER
        }
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost
        every { utenlandskMyndighetService.finnInstitusjonID(any()) } returns Optional.empty()


        journalfoeringService.journalførOgOpprettSak(opprettDto)

        verify {
            prosessinstansService.opprettProsessinstansJournalføringNySak(
                capture(journalfoeringOpprettRequestSlot), ProsessType.JFR_NY_SAK_BRUKER, true, LocalDate.EPOCH,
                Behandlingsaarsaktyper.SØKNAD, null, journalpost.mottaksKanalErEessi()
            )
        }
        journalfoeringOpprettRequestSlot.captured.shouldNotBeNull().shouldBe(opprettDto.tilJournalfoeringOpprettRequest())
    }

    @Test
    fun journalførOgOpprettSakNyVurdering_gyldigSkalSendeForvaltningsmeldingKasterIkkeFeilUnderValidering() {
        opprettDto.apply {
            fagsak = lagFagsakDto(LocalDate.MIN, LocalDate.MAX, "DK", Sakstyper.EU_EOS).apply { sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode }
            behandlingstypeKode = Behandlingstyper.NY_VURDERING.kode
            behandlingstemaKode = Behandlingstema.UTSENDT_SELVSTENDIG.kode
            forvaltningsmeldingMottaker = ForvaltningsmeldingMottaker.BRUKER
        }
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost
        every { utenlandskMyndighetService.finnInstitusjonID(any()) } returns Optional.empty()


        journalfoeringService.journalførOgOpprettSak(opprettDto)


        verify {
            prosessinstansService.opprettProsessinstansJournalføringNySak(
                capture(journalfoeringOpprettRequestSlot), ProsessType.JFR_NY_SAK_BRUKER, true, LocalDate.EPOCH,
                Behandlingsaarsaktyper.SØKNAD, null, journalpost.mottaksKanalErEessi()
            )
        }
        journalfoeringOpprettRequestSlot.captured.shouldNotBeNull().shouldBe(opprettDto.tilJournalfoeringOpprettRequest())
    }

    @Test
    fun journalførOgOpprettSak_oppretterKorrektProsessinstans() {
        opprettDto.apply {
            fagsak = lagFagsakDto(LocalDate.MIN, LocalDate.MAX, "DK", Sakstyper.EU_EOS).apply { sakstema = Sakstemaer.UNNTAK.kode }
            opprettDto.behandlingstypeKode = Behandlingstyper.HENVENDELSE.kode
            opprettDto.behandlingstemaKode = Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET.kode
            opprettDto.brukerID = "1234"
        }
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost
        every { utenlandskMyndighetService.finnInstitusjonID(any()) } returns Optional.of(INSTITUSJON_ID)


        journalfoeringService.journalførOgOpprettSak(opprettDto)

        verify {
            prosessinstansService.opprettProsessinstansJournalføringNySak(
                opprettDto.tilJournalfoeringOpprettRequest(), ProsessType.JFR_NY_SAK_BRUKER, false, LocalDate.EPOCH,
                Behandlingsaarsaktyper.HENVENDELSE, INSTITUSJON_ID, journalpost.mottaksKanalErEessi()
            )
        }
    }

    @Test
    fun journalførOgOpprettSak_behandlingstemaIkkeYrkesaktivUtenLandOgPeriode_prosessinstansBlirOpprettet() {
        opprettDto.fagsak = lagFagsakDto(null, null, null, Sakstyper.EU_EOS)
        opprettDto.behandlingstemaKode = Behandlingstema.IKKE_YRKESAKTIV.kode
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost
        every { utenlandskMyndighetService.finnInstitusjonID(any()) } returns Optional.of(INSTITUSJON_ID)


        journalfoeringService.journalførOgOpprettSak(opprettDto)


        verify {
            prosessinstansService.opprettProsessinstansJournalføringNySak(
                opprettDto.tilJournalfoeringOpprettRequest(), ProsessType.JFR_NY_SAK_BRUKER, false, LocalDate.EPOCH,
                Behandlingsaarsaktyper.SØKNAD, INSTITUSJON_ID, journalpost.mottaksKanalErEessi()
            )
        }
    }


    @Test
    fun journalførOgOpprettSak_sakstypeFtrlUtenLandOgPeriode_prosessinstansBlirOpprettet() {
        opprettDto.fagsak = lagFagsakDto(null, null, null, Sakstyper.FTRL)
        opprettDto.behandlingstemaKode = Behandlingstema.YRKESAKTIV.kode
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost
        every { utenlandskMyndighetService.finnInstitusjonID(any()) } returns Optional.of(INSTITUSJON_ID)


        journalfoeringService.journalførOgOpprettSak(opprettDto)


        verify {
            prosessinstansService.opprettProsessinstansJournalføringNySak(
                opprettDto.tilJournalfoeringOpprettRequest(), ProsessType.JFR_NY_SAK_BRUKER, false, LocalDate.EPOCH,
                Behandlingsaarsaktyper.SØKNAD, INSTITUSJON_ID, journalpost.mottaksKanalErEessi()
            )
        }
    }

    @Test
    fun journalførOgOpprettSak_sakstypeFtrlBehandlingstemaArbeidFlereLand_feilKombinasjonSakstypeBehandlingstemaKasterFeil() {
        opprettDto.fagsak = lagFagsakDto(null, null, null, Sakstyper.FTRL)
        opprettDto.behandlingstemaKode = Behandlingstema.ARBEID_FLERE_LAND.kode
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost


        shouldThrow<FunksjonellException> { journalfoeringService.journalførOgOpprettSak(opprettDto) }
            .message.shouldBe("ARBEID_FLERE_LAND er ikke et lovlig behandlingstema med de andre valgte verdiene")
    }

    @Test
    fun journalførOgOpprettSak_fomEtterTom_feiler() {
        opprettDto.fagsak = lagFagsakDto(LocalDate.MAX, LocalDate.MIN, "DK", Sakstyper.EU_EOS)
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost


        shouldThrow<FunksjonellException> { journalfoeringService.journalførOgOpprettSak(opprettDto) }
            .message.shouldBe("Fra og med dato kan ikke være etter til og med dato.")
    }

    @Test
    fun journalførOgOpprettSak_utenTom_gyldig() {
        opprettDto.fagsak = lagFagsakDto(LocalDate.MIN, null, "DK", Sakstyper.EU_EOS)
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost
        every { utenlandskMyndighetService.finnInstitusjonID(any()) } returns Optional.of(INSTITUSJON_ID)


        journalfoeringService.journalførOgOpprettSak(opprettDto)

        verify {
            prosessinstansService.opprettProsessinstansJournalføringNySak(
                opprettDto.tilJournalfoeringOpprettRequest(), ProsessType.JFR_NY_SAK_BRUKER,
                true, LocalDate.EPOCH, Behandlingsaarsaktyper.SØKNAD, INSTITUSJON_ID, journalpost.mottaksKanalErEessi()
            )
        }
    }

    @Test
    fun journalførOgOpprettSak_sakstypeFtrl_oppretterSak() {
        opprettDto.fagsak = lagFagsakDto(LocalDate.MIN, null, "DK", Sakstyper.FTRL)
        opprettDto.behandlingstemaKode = Behandlingstema.YRKESAKTIV.kode
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost
        every { utenlandskMyndighetService.finnInstitusjonID(any()) } returns Optional.of(INSTITUSJON_ID)


        journalfoeringService.journalførOgOpprettSak(opprettDto)


        verify {
            prosessinstansService.opprettProsessinstansJournalføringNySak(
                opprettDto.tilJournalfoeringOpprettRequest(), ProsessType.JFR_NY_SAK_BRUKER,
                false, LocalDate.EPOCH, Behandlingsaarsaktyper.SØKNAD, INSTITUSJON_ID, journalpost.mottaksKanalErEessi()
            )
        }
    }

    @Test
    fun journalførOgOpprettSak_oppgaveID_mangler() {
        opprettDto.oppgaveID = null
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost


        shouldThrow<FunksjonellException> { journalfoeringService.journalførOgOpprettSak(opprettDto) }
            .message.shouldBe("OppgaveID mangler")
    }

    @Test
    fun journalførOgOpprettSak_avsenderIdMangler_kasterFeil() {
        opprettDto.avsenderID = null
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost


        shouldThrow<FunksjonellException> { journalfoeringService.journalførOgOpprettSak(opprettDto) }
            .message.shouldBe("AvsenderID er påkrevd når AvsenderType er satt")
    }

    @Test
    fun journalførOgOpprettSak_avsenderTypeMangler_kasterFeil() {
        opprettDto.avsenderType = null
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost


        shouldThrow<FunksjonellException> { journalfoeringService.journalførOgOpprettSak(opprettDto) }
            .message.shouldBe("AvsenderType er påkrevd når AvsenderID er satt")
    }

    @Test
    fun journalførOgOpprettSak_avsenderManglerMottakskanalErEessi_kasterIkkeFeil() {
        opprettDto.apply {
            avsenderID = null
            avsenderNavn = null
            avsenderType = null
            behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
            fagsak = lagFagsakDto(LocalDate.MIN, LocalDate.MAX, "DK", Sakstyper.EU_EOS)
        }
        journalpost.mottaksKanal = "EESSI"
        val melosysEessiMelding = MelosysEessiMelding().apply { rinaSaksnummer = RINA_SAKSNUMMER }
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost
        every { utenlandskMyndighetService.finnInstitusjonID(any()) } returns Optional.empty()
        every { eessiService.finnSakForRinasaksnummer(RINA_SAKSNUMMER) } returns Optional.empty()
        every { eessiService.støtterAutomatiskBehandling(melosysEessiMelding) } returns false
        every { eessiService.hentSedTilknyttetJournalpost(journalpost.journalpostId) } returns melosysEessiMelding


        shouldNotThrowAny { journalfoeringService.journalførOgOpprettSak(opprettDto) }
    }

    @Test
    fun journalførOgOpprettSak_støtterAutomatiskBehandling_forventException() {
        opprettDto.behandlingstemaKode = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING.kode
        journalpost.mottaksKanal = "EESSI"
        val melosysEessiMelding = MelosysEessiMelding().apply { rinaSaksnummer = RINA_SAKSNUMMER }
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost
        every { eessiService.hentSedTilknyttetJournalpost(journalpost.journalpostId) } returns melosysEessiMelding
        every { eessiService.støtterAutomatiskBehandling(melosysEessiMelding) } returns true


        shouldThrow<FunksjonellException> { journalfoeringService.journalførOgOpprettSak(opprettDto) }
            .message.shouldBe("Journalpost med id null skal ikke journalføres manuelt")
    }

    @Test
    fun journalførOgOpprettSak_sedAlleredeTilknyttet_kasterException() {
        val melosysEessiMelding = MelosysEessiMelding().apply { rinaSaksnummer = RINA_SAKSNUMMER }
        opprettDto.behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
        journalpost.mottaksKanal = "EESSI"
        every { eessiService.hentSedTilknyttetJournalpost(journalpost.journalpostId) } returns melosysEessiMelding
        every { eessiService.støtterAutomatiskBehandling(melosysEessiMelding) } returns false
        every { eessiService.finnSakForRinasaksnummer(RINA_SAKSNUMMER) } returns Optional.of(ARKIVSAK_ID)
        every { fagsakService.finnFagsakFraArkivsakID(ARKIVSAK_ID) } returns Optional.of(builder().saksnummer(ARKIVSAK_ID.toString()).build())
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost


        shouldThrow<FunksjonellException> { journalfoeringService.journalførOgOpprettSak(opprettDto) }
            .message.shouldBe("RINA-sak 22222 er allerede tilknyttet 111")
    }

    @Test
    fun journalførOgOpprettSak_brukerIDOgVirksomheOrgnrMangler_kasterException() {
        opprettDto.brukerID = null
        opprettDto.virksomhetOrgnr = null
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost


        shouldThrow<FunksjonellException> { journalfoeringService.journalførOgOpprettSak(opprettDto) }
            .message.shouldBe("Både BrukerID og VirksomhetOrgnr mangler. Krever én")
    }

    @Test
    fun journalførOgOpprettSak_brukerIDOgVirksomhetOrgnrFinnes_kasterException() {
        opprettDto.brukerID = "fnr"
        opprettDto.virksomhetOrgnr = "orgnr"
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost


        shouldThrow<FunksjonellException> { journalfoeringService.journalførOgOpprettSak(opprettDto) }
            .message.shouldBe("Både BrukerID og VirksomhetOrgnr finnes. Dette kan skape problemer. Velg én å journalføre dokumentet på.")
    }

    @Test
    fun journalførOgOpprettSak_journalpostErFerdigstilt_kasterException() {
        journalpost.isErFerdigstilt = true
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost


        shouldThrow<FunksjonellException> { journalfoeringService.journalførOgOpprettSak(opprettDto) }
            .message.shouldBe("Journalposten er allerede ferdigstilt!")
    }

    @Test
    fun journalførOgOpprettSak_trygdeMyndighetEøsOgSakstypeIkkeEøs_kasterException() {
        opprettDto.apply {
            avsenderID = "BE"
            fagsak.sakstype = Sakstyper.TRYGDEAVTALE.kode
            behandlingstemaKode = Behandlingstema.YRKESAKTIV.kode
        }
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost


        shouldThrow<FunksjonellException> { journalfoeringService.journalførOgOpprettSak(opprettDto) }
            .message.shouldBe("Sak for trygdemyndighet fra BE skal være av type EU_EOS")
    }

    @Test
    fun journalførOgOpprettSak_trygdeMyndighetlandOgSakstypeIkkeTrygeavtale_kasterException() {
        opprettDto.apply {
            avsenderID = "RS"
            fagsak.sakstype = Sakstyper.EU_EOS.kode
            behandlingstemaKode = Behandlingstema.ARBEID_FLERE_LAND.kode
        }
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost


        shouldThrow<FunksjonellException> { journalfoeringService.journalførOgOpprettSak(opprettDto) }
            .message.shouldBe("Sak for trygdemyndighet fra RS skal være av type TRYGDEAVTALE")
    }

    @Test
    fun journalførOgKnyttTilEksisterendeSak_altOK_prosessinstansOpprettet() {
        tilordneDto.saksnummer = MELOSYS_SAKSNUMMER
        val fagsak = builder().behandlinger(Behandling().apply { status = Behandlingsstatus.UNDER_BEHANDLING }).build()
        every { joarkFasade.hentJournalpost(tilordneDto.journalpostID) } returns journalpost
        every { fagsakService.hentFagsak(MELOSYS_SAKSNUMMER) } returns fagsak
        every { utenlandskMyndighetService.finnInstitusjonID(any()) } returns Optional.of(INSTITUSJON_ID)


        journalfoeringService.journalførOgKnyttTilEksisterendeSak(tilordneDto)


        verify {
            prosessinstansService.opprettProsessinstansJournalføringKnyttTilEksisterende(
                tilordneDto.tilJournalfoeringTilordneRequest(), tilordneDto.saksnummer,
                fagsak, INSTITUSJON_ID, journalpost.mottaksKanalErEessi()
            )
        }
    }

    @Test
    fun journalførOgKnyttTilEksisterendeSak_behandlingstypeFØRSTEGANG_prosessinstansOpprettet() {
        tilordneDto.saksnummer = MELOSYS_SAKSNUMMER
        tilordneDto.behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
        val fagsak = builder().behandlinger(Behandling().apply { status = Behandlingsstatus.UNDER_BEHANDLING }).build()
        every { joarkFasade.hentJournalpost(tilordneDto.journalpostID) } returns journalpost
        every { fagsakService.hentFagsak(MELOSYS_SAKSNUMMER) } returns fagsak
        every { utenlandskMyndighetService.finnInstitusjonID(any()) } returns Optional.empty()


        journalfoeringService.journalførOgKnyttTilEksisterendeSak(tilordneDto)


        verify {
            prosessinstansService.opprettProsessinstansJournalføringKnyttTilEksisterende(
                tilordneDto.tilJournalfoeringTilordneRequest(), tilordneDto.saksnummer,
                fagsak, null, journalpost.mottaksKanalErEessi()
            )
        }
    }

    @Test
    fun journalførOgKnyttTilEksisterendeSak_fagsakFinnesIkke_kasterFeil() {
        tilordneDto.saksnummer = null
        val feilmelding = "Det finnes ingen fagsak med saksnummer: null"
        every { joarkFasade.hentJournalpost(tilordneDto.journalpostID) } returns journalpost
        every { fagsakService.hentFagsak(null) } throws IkkeFunnetException(feilmelding)


        shouldThrow<IkkeFunnetException> { journalfoeringService.journalførOgKnyttTilEksisterendeSak(tilordneDto) }.message.shouldBe(feilmelding)
    }

    @Test
    fun journalførOgKnyttTilEksisterendeSak_sedSakTilknyttetAnnenFagsak_kasterException() {
        journalpost.mottaksKanal = "EESSI"
        tilordneDto.saksnummer = "FAGSAK SOM PRØVER Å KNYTTE JOURNALPOST FOR SED TIL SEG"
        val melosysEessiMelding = MelosysEessiMelding().apply { rinaSaksnummer = RINA_SAKSNUMMER }
        val fagsak = builder()
            .saksnummer("FAGSAK KOBLET TIL SED FRA FØR")
            .behandlinger(Behandling().apply { status = Behandlingsstatus.UNDER_BEHANDLING })
            .build()
        every { eessiService.hentSedTilknyttetJournalpost(journalpost.journalpostId) } returns melosysEessiMelding
        every { eessiService.finnSakForRinasaksnummer(RINA_SAKSNUMMER) } returns Optional.of(ARKIVSAK_ID)
        every { eessiService.støtterAutomatiskBehandling(melosysEessiMelding) } returns false
        every { fagsakService.finnFagsakFraArkivsakID(ARKIVSAK_ID) } returns Optional.of(fagsak)
        every { fagsakService.hentFagsak("FAGSAK SOM PRØVER Å KNYTTE JOURNALPOST FOR SED TIL SEG") } returns lagFagsak()
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost


        shouldThrow<FunksjonellException> { journalfoeringService.journalførOgKnyttTilEksisterendeSak(tilordneDto) }
            .message.shouldBe("RINA-sak 22222 er allerede tilknyttet FAGSAK KOBLET TIL SED FRA FØR")

    }

    @Test
    fun journalførOgOpprettAndregangsBehandling_altOK_prosessinstansOpprettet() {
        tilordneDto.saksnummer = MELOSYS_SAKSNUMMER
        tilordneDto.behandlingstemaKode = Behandlingstema.BESLUTNING_LOVVALG_NORGE.kode
        tilordneDto.behandlingstypeKode = Behandlingstyper.NY_VURDERING.kode
        val behandling = lagBehandling().apply {
            status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
            type = Behandlingstyper.NY_VURDERING
            tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        }
        val fagsak = lagFagsak(behandling).apply {
            type = Sakstyper.FTRL
            tema = Sakstemaer.UNNTAK
            leggTilAktør(Aktoer().apply { rolle = Aktoersroller.BRUKER })
        }
        every { behandlingsresultatRepository.findById(any()) } returns Optional.empty()
        every { joarkFasade.hentJournalpost(tilordneDto.journalpostID) } returns journalpost
        every { fagsakService.hentFagsak(MELOSYS_SAKSNUMMER) } returns fagsak
        every { utenlandskMyndighetService.finnInstitusjonID(any()) } returns Optional.empty()


        journalfoeringService.journalførOgOpprettAndregangsBehandling(tilordneDto)


        verify {
            prosessinstansService.journalførOgOpprettAndregangsBehandling(
                ProsessType.JFR_ANDREGANG_NY_BEHANDLING,
                Behandlingstema.BESLUTNING_LOVVALG_NORGE,
                Behandlingstyper.NY_VURDERING,
                tilordneDto.tilJournalfoeringTilordneRequest(),
                Behandlingsaarsaktyper.ANNET,
                LocalDate.EPOCH,
                null,
                journalpost.mottaksKanalErEessi()
            )
        }
    }

    @Test
    fun journalførOgOpprettAndregangsBehandlingIkkeKopierBehandling_altOK_prosessinstansOpprettet() {
        tilordneDto.saksnummer = MELOSYS_SAKSNUMMER
        tilordneDto.behandlingstemaKode = Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET.kode
        tilordneDto.behandlingstypeKode = Behandlingstyper.HENVENDELSE.kode
        val behandling = lagBehandling().apply {
            status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
        }
        val fagsak = lagFagsak(behandling).apply {
            type = Sakstyper.EU_EOS
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            leggTilAktør(Aktoer().apply { rolle = Aktoersroller.BRUKER })
        }
        every { joarkFasade.hentJournalpost(tilordneDto.journalpostID) } returns journalpost
        every { fagsakService.hentFagsak(MELOSYS_SAKSNUMMER) } returns fagsak
        every { utenlandskMyndighetService.finnInstitusjonID(any()) } returns Optional.empty()


        journalfoeringService.journalførOgOpprettAndregangsBehandling(tilordneDto)


        verify {
            prosessinstansService.journalførOgOpprettAndregangsBehandling(
                ProsessType.JFR_ANDREGANG_NY_BEHANDLING,
                Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET,
                Behandlingstyper.HENVENDELSE,
                tilordneDto.tilJournalfoeringTilordneRequest(),
                Behandlingsaarsaktyper.HENVENDELSE,
                LocalDate.EPOCH,
                null,
                journalpost.mottaksKanalErEessi()
            )
        }
    }

    @Test
    fun journalførOgOpprettAndregangsBehandlingKopierBehandling_altOK_prosessinstansOpprettet() {
        tilordneDto.saksnummer = MELOSYS_SAKSNUMMER
        tilordneDto.behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
        tilordneDto.behandlingstypeKode = Behandlingstyper.NY_VURDERING.kode
        val behandling = lagBehandling().apply {
            status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
            type = Behandlingstyper.NY_VURDERING
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        }
        val fagsak = lagFagsak(behandling).apply {
            type = Sakstyper.EU_EOS
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            leggTilAktør(Aktoer().apply { rolle = Aktoersroller.BRUKER })
        }
        val behandlingsresultat = Behandlingsresultat().apply { type = Behandlingsresultattyper.AVSLAG_SØKNAD }
        every { joarkFasade.hentJournalpost(tilordneDto.journalpostID) } returns journalpost
        every { fagsakService.hentFagsak(MELOSYS_SAKSNUMMER) } returns fagsak
        every { behandlingsresultatRepository.findById(any()) } returns Optional.of(behandlingsresultat)
        every { utenlandskMyndighetService.finnInstitusjonID(any()) } returns Optional.empty()


        journalfoeringService.journalførOgOpprettAndregangsBehandling(tilordneDto)


        verify {
            prosessinstansService.journalførOgOpprettAndregangsBehandling(
                ProsessType.JFR_ANDREGANG_REPLIKER_BEHANDLING,
                Behandlingstema.UTSENDT_ARBEIDSTAKER,
                Behandlingstyper.NY_VURDERING,
                tilordneDto.tilJournalfoeringTilordneRequest(),
                Behandlingsaarsaktyper.SØKNAD,
                LocalDate.EPOCH,
                null,
                journalpost.mottaksKanalErEessi()
            )
        }
    }

    @Test
    fun journalførOgOpprettAndregangsBehandling_fagsakHarAktivBehandling_feilKastes() {
        tilordneDto.saksnummer = MELOSYS_SAKSNUMMER
        val aktivBehandling = lagBehandling().apply { status = Behandlingsstatus.UNDER_BEHANDLING }
        every { joarkFasade.hentJournalpost(tilordneDto.journalpostID) } returns journalpost
        every { fagsakService.hentFagsak(MELOSYS_SAKSNUMMER) } returns builder().behandlinger(aktivBehandling).build()
        every { behandlingsresultatService.hentBehandlingsresultatMedAnmodningsperioder(aktivBehandling.id) } returns Behandlingsresultat()


        shouldThrow<FunksjonellException> { journalfoeringService.journalførOgOpprettAndregangsBehandling(tilordneDto) }
            .message.shouldBe("Det finnes allerede en aktiv behandling på fagsak ${FagsakTestFactory.SAKSNUMMER}")
    }

    @Test
    fun journalførOgOpprettAndregangsBehandling_fagsakHarAktivBehandlingMenErArtikkel16AnmodningSendtUtland_feilKastesIkke() {
        tilordneDto.saksnummer = MELOSYS_SAKSNUMMER
        val aktivBehandling = lagBehandling().apply { status = Behandlingsstatus.UNDER_BEHANDLING }
        val behandlingsresultat = Behandlingsresultat().apply {
            anmodningsperioder.add(Anmodningsperiode().apply { setSendtUtland(true) })
        }
        every { joarkFasade.hentJournalpost(tilordneDto.journalpostID) } returns journalpost
        every { fagsakService.hentFagsak(MELOSYS_SAKSNUMMER) } returns lagFagsak(aktivBehandling)
        every { behandlingsresultatService.hentBehandlingsresultatMedAnmodningsperioder(aktivBehandling.id) } returns behandlingsresultat


        shouldThrow<FunksjonellException> { journalfoeringService.journalførOgOpprettAndregangsBehandling(tilordneDto) }
            .message.shouldNotBe("Det finnes allerede en aktiv behandling på fagsak ${FagsakTestFactory.SAKSNUMMER}")
    }

    @Test
    fun journalførOgOpprettAndregangsBehandling_sedSakTilknyttetAnnenFagsak_kasterException() {
        journalpost.mottaksKanal = "EESSI"
        val melosysEessiMelding = MelosysEessiMelding().apply { rinaSaksnummer = RINA_SAKSNUMMER }
        val fagsak1 = lagFagsak("FAGSAK KOBLET TIL SED FRA FØR", lagBehandling().apply { status = Behandlingsstatus.UNDER_BEHANDLING })
        val fagsak2 = lagFagsak("FAGSAK SOM PRØVER Å KNYTTE JOURNALPOST FOR SED TIL SEG", lagBehandling())
        tilordneDto.saksnummer = fagsak2.saksnummer
        every { eessiService.hentSedTilknyttetJournalpost(journalpost.journalpostId) } returns melosysEessiMelding
        every { eessiService.støtterAutomatiskBehandling(melosysEessiMelding) } returns false
        every { eessiService.finnSakForRinasaksnummer(RINA_SAKSNUMMER) } returns Optional.of(ARKIVSAK_ID)
        every { fagsakService.finnFagsakFraArkivsakID(ARKIVSAK_ID) } returns Optional.of(fagsak1)
        every { fagsakService.hentFagsak(fagsak2.saksnummer) } returns fagsak2
        every { joarkFasade.hentJournalpost(journalpost.journalpostId) } returns journalpost


        shouldThrow<FunksjonellException> { journalfoeringService.journalførOgOpprettAndregangsBehandling(tilordneDto) }
            .message.shouldBe("RINA-sak 22222 er allerede tilknyttet FAGSAK KOBLET TIL SED FRA FØR")
    }

    @Test
    fun journalførOgOpprettAndregangsBehandling_fagsakFinnesIkke_kasterFeil() {
        tilordneDto.saksnummer = null
        val feilmelding = "Det finnes ingen fagsak med saksnummer: null"
        every { joarkFasade.hentJournalpost(tilordneDto.journalpostID) } returns journalpost
        every { fagsakService.hentFagsak(null) } throws IkkeFunnetException(feilmelding)


        shouldThrow<IkkeFunnetException> { journalfoeringService.journalførOgOpprettAndregangsBehandling(tilordneDto) }.message.shouldBe(feilmelding)
    }

    @Test
    fun journalførSed_støtterIkkeAutomatiskBehandling_forventException() {
        every { eessiService.støtterAutomatiskBehandling(journalfoeringSedDto.journalpostID) } returns false


        shouldThrow<FunksjonellException> { journalfoeringService.journalførSed(journalfoeringSedDto) }
            .message.shouldBe("Sed tilknyttet journalpost journalpostID støtter ikke automatisk behandling!")
    }

    @Test
    fun journalførSed_manglerBrukerID_forventException() {
        journalfoeringSedDto.brukerID = null


        shouldThrow<FunksjonellException> { journalfoeringService.journalførSed(journalfoeringSedDto) }
            .message.shouldBe("BrukerID er påkrevd!")
    }

    @Test
    fun journalførSed_manglerJournalpostID_forventException() {
        journalfoeringSedDto.journalpostID = null


        shouldThrow<FunksjonellException> { journalfoeringService.journalførSed(journalfoeringSedDto) }
            .message.shouldBe("JournalpostID er påkrevd!")
    }

    @Test
    fun journalførSed_manglerOppgaveID_forventException() {
        journalfoeringSedDto.oppgaveID = null


        shouldThrow<FunksjonellException> { journalfoeringService.journalførSed(journalfoeringSedDto) }
            .message.shouldBe("OppgaveID er påkrevd!")
    }

    @Test
    fun journalførSed_støtterAutomatiskBehandling_prosessinstansOpprettetOppgaveFerdigstilt() {
        val melosysEessiMelding = MelosysEessiMelding().apply { rinaSaksnummer = "123" }
        every { eessiService.støtterAutomatiskBehandling(journalfoeringSedDto.journalpostID) } returns true
        every { eessiService.hentSedTilknyttetJournalpost(journalfoeringSedDto.journalpostID) } returns melosysEessiMelding
        every { persondataFasade.hentAktørIdForIdent(journalfoeringSedDto.brukerID) } returns AKTØR_ID


        journalfoeringService.journalførSed(journalfoeringSedDto)


        verify { prosessinstansService.opprettProsessinstansSedMottak(melosysEessiMelding, AKTØR_ID) }
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

    private fun lagFagsak(behandling: Behandling): Fagsak = lagFagsak(FagsakTestFactory.SAKSNUMMER, behandling)

    private fun lagBehandling(): Behandling =
        Behandling().apply {
            id = 1L
            status = Behandlingsstatus.AVSLUTTET
        }
}

