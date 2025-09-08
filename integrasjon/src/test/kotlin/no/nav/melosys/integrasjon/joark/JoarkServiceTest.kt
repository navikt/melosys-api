package no.nav.melosys.integrasjon.joark

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.arkiv.BrukerIdType
import no.nav.melosys.domain.arkiv.DokumentReferanse
import no.nav.melosys.domain.arkiv.FysiskDokument
import no.nav.melosys.domain.arkiv.OpprettJournalpost
import no.nav.melosys.domain.kodeverk.Avsendertyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.exception.SikkerhetsbegrensningException
import no.nav.melosys.integrasjon.Konstanter
import no.nav.melosys.integrasjon.joark.journalpostapi.JournalpostapiConsumer
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.AvsenderMottaker.IdType
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.Bruker.BrukerIdType.FNR
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.FerdigstillJournalpostRequest
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OppdaterJournalpostRequest
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostRequest
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostResponse
import no.nav.melosys.integrasjon.joark.saf.SafConsumer
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class JoarkServiceTest {
    private lateinit var joarkService: JoarkService

    private val journalpostapiConsumer = mockk<JournalpostapiConsumer>()
    private val safConsumer = mockk<SafConsumer>()

    private val ferdigstillJournalpostCaptor = slot<FerdigstillJournalpostRequest>()
    private val oppdaterJournalpostRequestCaptor = slot<OppdaterJournalpostRequest>()
    private val logiskVedleggTittelCaptor = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        joarkService = JoarkService(journalpostapiConsumer, safConsumer)
        logiskVedleggTittelCaptor.clear()
    }

    @Test
    fun `hent journalposter tilknyttet sak bruker saf mapper alle saf journalposter`() {
        val saksnummer = "191919"
        val arkivsakID = 12345L
        every { safConsumer.hentDokumentoversikt(saksnummer) } returns listOf(safJournalpost("111"), safJournalpost("222"))

        val journalposter = joarkService.hentJournalposterTilknyttetSak(HentJournalposterTilknyttetSakRequest(arkivsakID, saksnummer))

        journalposter shouldHaveSize 2
    }

    @Test
    fun `oppdater journalpost når påkrevde verdier er utfylt`() {
        val journalpostID = "11112233"
        val tittel = "tittel"
        val hovedDokumentID = "1234"
        val vedleggMedTitler = mutableMapOf<String, String>()
        val fysiskVedleggTittel = "Fysisk vedlegg"
        val fysiskVedleggID = "vedleggDokID"
        vedleggMedTitler[fysiskVedleggID] = fysiskVedleggTittel

        val journalpostOppdatering = JournalpostOppdatering.Builder()
            .medSaksnummer("MEL-9")
            .medHovedDokumentID(hovedDokumentID)
            .medBrukerID("12345")
            .medAvsenderID("12")
            .medAvsenderNavn("321")
            .medAvsenderType(Avsendertyper.ORGANISASJON)
            .medTittel(tittel)
            .medFysiskeVedlegg(vedleggMedTitler)
            .medLogiskeVedleggTitler(listOf("dok1", "dok2"))
            .build()

        every { safConsumer.hentJournalpost(any()) } returns safJournalpost(journalpostID)
        every { journalpostapiConsumer.fjernLogiskeVedlegg(any(), any()) } just Runs
        every { journalpostapiConsumer.oppdaterJournalpost(capture(oppdaterJournalpostRequestCaptor), any()) } just Runs
        every { journalpostapiConsumer.leggTilLogiskVedlegg(any(), capture(logiskVedleggTittelCaptor)) } just Runs
        every { journalpostapiConsumer.ferdigstillJournalpost(any(), any()) } just Runs

        joarkService.oppdaterOgFerdigstillJournalpost("123", journalpostOppdatering)

        verify { journalpostapiConsumer.fjernLogiskeVedlegg(any(), any()) }
        verify { journalpostapiConsumer.oppdaterJournalpost(any(), any()) }
        val request = oppdaterJournalpostRequestCaptor.captured

        request.shouldNotBeNull().run {
            this.tittel shouldBe tittel
            avsenderMottaker.shouldNotBeNull().run {
                navn.shouldNotBeNull()
                idType shouldBe IdType.ORGNR
            }

            bruker.shouldNotBeNull().run {
                id.shouldNotBeNull()
                idType shouldBe FNR
            }

            sak.shouldNotBeNull().run {
                fagsakId shouldBe journalpostOppdatering.saksnummer
                sakstype shouldBe "FAGSAK"
                fagsaksystem.shouldNotBeNull()
            }
        }

        request.dokumenter.shouldHaveSize(2).toList().run {
            get(0).tittel shouldBe tittel
            get(0).dokumentInfoId shouldBe hovedDokumentID
            get(1).tittel shouldBe fysiskVedleggTittel
            get(1).dokumentInfoId shouldBe fysiskVedleggID
        }

        verify(exactly = 2) { journalpostapiConsumer.leggTilLogiskVedlegg(any(), any()) }
        verify { journalpostapiConsumer.ferdigstillJournalpost(any(), any()) }
        logiskVedleggTittelCaptor shouldHaveSize 2
        logiskVedleggTittelCaptor[0] shouldBe "dok1"
        logiskVedleggTittelCaptor[1] shouldBe "dok2"
    }

    @Test
    fun `oppdater journalpost uten vedlegg fungerer`() {
        val tittel = "tittel"
        val hovedDokumentID = "1234"

        val journalpostOppdatering = JournalpostOppdatering.Builder()
            .medSaksnummer("MEL-8")
            .medBrukerID("12345")
            .medHovedDokumentID(hovedDokumentID)
            .medAvsenderID("12")
            .medAvsenderNavn("321")
            .medAvsenderType(Avsendertyper.PERSON)
            .medTittel(tittel)
            .build()

        every { safConsumer.hentJournalpost(any()) } returns safJournalpostUtenVedlegg()
        every { journalpostapiConsumer.fjernLogiskeVedlegg(any(), any()) } just Runs
        every { journalpostapiConsumer.oppdaterJournalpost(capture(oppdaterJournalpostRequestCaptor), any()) } just Runs
        every { journalpostapiConsumer.leggTilLogiskVedlegg(any(), any()) } just Runs
        every { journalpostapiConsumer.ferdigstillJournalpost(any(), any()) } just Runs

        joarkService.oppdaterOgFerdigstillJournalpost("123", journalpostOppdatering)

        verify(exactly = 0) { journalpostapiConsumer.fjernLogiskeVedlegg(any(), any()) }
        verify { journalpostapiConsumer.oppdaterJournalpost(any(), any()) }
        val request = oppdaterJournalpostRequestCaptor.captured

        request.shouldNotBeNull().run {
            this.tittel shouldBe tittel
            avsenderMottaker.shouldNotBeNull().run {
                navn.shouldNotBeNull()
            }

            bruker.shouldNotBeNull().run {
                id.shouldNotBeNull()
                idType shouldBe FNR
            }

            sak.shouldNotBeNull().run {
                fagsakId shouldBe journalpostOppdatering.saksnummer
                sakstype shouldBe "FAGSAK"
                fagsaksystem.shouldNotBeNull()
            }
        }

        request.dokumenter.shouldHaveSize(1).toList().run {
            get(0).tittel shouldBe tittel
            get(0).dokumentInfoId shouldBe hovedDokumentID
        }

        verify(exactly = 0) { journalpostapiConsumer.leggTilLogiskVedlegg(any(), any()) }
        verify { journalpostapiConsumer.ferdigstillJournalpost(any(), any()) }
    }

    @Test
    fun `oppdater journalpost uten avsender fungerer`() {
        val tittel = "tittel"
        val hovedDokumentID = "1234"

        val journalpostOppdatering = JournalpostOppdatering.Builder()
            .medSaksnummer("MEL-9")
            .medHovedDokumentID(hovedDokumentID)
            .medBrukerID("12345")
            .medTittel(tittel)
            .build()

        every { safConsumer.hentJournalpost(any()) } returns safJournalpostUtenVedlegg()
        every { journalpostapiConsumer.fjernLogiskeVedlegg(any(), any()) } just Runs
        every { journalpostapiConsumer.oppdaterJournalpost(capture(oppdaterJournalpostRequestCaptor), any()) } just Runs
        every { journalpostapiConsumer.leggTilLogiskVedlegg(any(), any()) } just Runs
        every { journalpostapiConsumer.ferdigstillJournalpost(any(), any()) } just Runs

        joarkService.oppdaterOgFerdigstillJournalpost("123", journalpostOppdatering)

        verify(exactly = 0) { journalpostapiConsumer.fjernLogiskeVedlegg(any(), any()) }
        verify { journalpostapiConsumer.oppdaterJournalpost(any(), any()) }
        val request = oppdaterJournalpostRequestCaptor.captured

        request.shouldNotBeNull().run {
            this.tittel shouldBe tittel
            avsenderMottaker.shouldBeNull()

            bruker.shouldNotBeNull().run {
                id.shouldNotBeNull()
                idType shouldBe FNR
            }

            sak.shouldNotBeNull().run {
                fagsakId shouldBe journalpostOppdatering.saksnummer
                sakstype shouldBe "FAGSAK"
                fagsaksystem.shouldNotBeNull()
            }
        }

        request.dokumenter.shouldHaveSize(1).toList().run {
            get(0).tittel shouldBe tittel
            get(0).dokumentInfoId shouldBe hovedDokumentID
        }

        verify(exactly = 0) { journalpostapiConsumer.leggTilLogiskVedlegg(any(), any()) }
        verify { journalpostapiConsumer.ferdigstillJournalpost(any(), any()) }
    }

    @Test
    fun `oppdater journalpost som skal ferdigstilles kaller ferdigstill journalpost`() {
        val journalpostID = "123321"
        val journalpostOppdatering = JournalpostOppdatering.Builder()
            .medSaksnummer("MEL-1111")
            .medBrukerID("12345")
            .build()

        every { safConsumer.hentJournalpost(any()) } returns safJournalpost(journalpostID, false)
        every { journalpostapiConsumer.oppdaterJournalpost(any<OppdaterJournalpostRequest>(), any()) } just Runs
        every { journalpostapiConsumer.ferdigstillJournalpost(any<FerdigstillJournalpostRequest>(), eq(journalpostID)) } just Runs

        joarkService.oppdaterOgFerdigstillJournalpost(journalpostID, journalpostOppdatering)

        verify(exactly = 0) { journalpostapiConsumer.fjernLogiskeVedlegg(any(), any()) }
        verify { journalpostapiConsumer.oppdaterJournalpost(any<OppdaterJournalpostRequest>(), any()) }
        verify { journalpostapiConsumer.ferdigstillJournalpost(any<FerdigstillJournalpostRequest>(), eq(journalpostID)) }
    }

    @Test
    fun `valider tilgang til arkivvariant med tilgang til vedlegg kaster ingen exception`() {
        val journalpostId = "11122233"
        val saksnummer = "191919"
        val arkivsakID = 12345L
        val safJournalpost = safJournalpost(journalpostId)
        every { safConsumer.hentDokumentoversikt(saksnummer) } returns listOf(safJournalpost)

        val dokumentReferanser = listOf(DokumentReferanse(journalpostId, VEDLEGG_MED_TILGANG_ID))

        // Should not throw exception
        joarkService.validerDokumenterTilhørerSakOgHarTilgang(
            HentJournalposterTilknyttetSakRequest(arkivsakID, saksnummer),
            dokumentReferanser
        )
    }

    @Test
    fun `valider tilgang til arkivvariant med tom dokument referanser collection henter ikke dokumentoversikt`() {
        val saksnummer = "191919"
        val arkivsakID = 12345L
        val dokumentReferanser = emptyList<DokumentReferanse>()

        joarkService.validerDokumenterTilhørerSakOgHarTilgang(
            HentJournalposterTilknyttetSakRequest(arkivsakID, saksnummer),
            dokumentReferanser
        )

        verify(exactly = 0) { safConsumer.hentDokumentoversikt(any()) }
    }

    @Test
    fun `valider tilgang til arkivvariant uten tilgang til vedlegg kaster sikkerhetsbegrensning exception`() {
        val journalpostId = "11122233"
        val saksnummer = "191919"
        val arkivsakID = 12345L
        val safJournalpost = safJournalpost(journalpostId)
        every { safConsumer.hentDokumentoversikt(saksnummer) } returns listOf(safJournalpost)

        val dokumentReferanser = listOf(DokumentReferanse(journalpostId, VEDLEGG_UTEN_TILGANG_ID))
        val request = HentJournalposterTilknyttetSakRequest(arkivsakID, saksnummer)

        val exception = shouldThrow<SikkerhetsbegrensningException> {
            joarkService.validerDokumenterTilhørerSakOgHarTilgang(request, dokumentReferanser)
        }

        exception.message shouldContain "Ikke tilgang"
    }

    @Test
    fun `valider tilgang til arkivvariant med journalposter ikke tilknyttet sak kaster funksjonell exception`() {
        val journalpostId = "11122233"
        val saksnummer = "191919"
        val arkivsakID = 12345L
        val safJournalpost = safJournalpost(journalpostId)
        every { safConsumer.hentDokumentoversikt(saksnummer) } returns listOf(safJournalpost)

        val dokumentReferanser = listOf(DokumentReferanse("12345", VEDLEGG_MED_TILGANG_ID))
        val request = HentJournalposterTilknyttetSakRequest(arkivsakID, saksnummer)

        val exception = shouldThrow<FunksjonellException> {
            joarkService.validerDokumenterTilhørerSakOgHarTilgang(request, dokumentReferanser)
        }

        exception.message shouldContain "tilhører ikke sak "
    }

    @Test
    fun `valider tilgang til arkivvariant med feil journalpost id kaster ikke funnet exception`() {
        val journalpostId = "11122233"
        val saksnummer = "191919"
        val arkivsakID = 12345L
        val safJournalpost = safJournalpostUtenVedlegg()
        every { safConsumer.hentDokumentoversikt(saksnummer) } returns listOf(safJournalpost)

        val dokumentReferanser = listOf(DokumentReferanse(journalpostId, VEDLEGG_MED_TILGANG_ID))
        val request = HentJournalposterTilknyttetSakRequest(arkivsakID, saksnummer)

        val exception = shouldThrow<IkkeFunnetException> {
            joarkService.validerDokumenterTilhørerSakOgHarTilgang(request, dokumentReferanser)
        }

        exception.message shouldContain "Finner ikke dokument "
    }

    @Test
    fun `hent journalpost med status utgår er utgått`() {
        val journalpostID = "1112233"
        val safJournalpost = safJournalpost(journalpostID, Journalstatus.UTGAAR, false)
        every { safConsumer.hentJournalpost(journalpostID) } returns safJournalpost

        val journalpost = joarkService.hentJournalpost(journalpostID)

        journalpost.isErUtgått shouldBe true
    }

    @Test
    fun `hent journalpost med status mottatt er ikke utgått`() {
        val journalpostID = "1112233"
        val safJournalpost = safJournalpost(journalpostID, Journalstatus.MOTTATT, false)
        every { safConsumer.hentJournalpost(journalpostID) } returns safJournalpost

        val journalpost = joarkService.hentJournalpost(journalpostID)

        journalpost.isErUtgått shouldBe false
    }

    @Test
    fun `hent journalpost verifiser mapping`() {
        val journalpostID = "1112233"
        val safJournalpost = safJournalpost(journalpostID)
        every { safConsumer.hentJournalpost(journalpostID) } returns safJournalpost

        val journalpost = joarkService.hentJournalpost(journalpostID)

        journalpost.run {
            journalpostId shouldBe safJournalpost.journalpostId()
            journalposttype shouldBe no.nav.melosys.domain.arkiv.Journalposttype.INN
            brukerId shouldBe safJournalpost.bruker().id()
            brukerIdType shouldBe BrukerIdType.FOLKEREGISTERIDENT
            avsenderId shouldBe safJournalpost.avsenderMottaker().id()
            avsenderNavn shouldBe safJournalpost.avsenderMottaker().navn()
            avsenderType shouldBe Avsendertyper.ORGANISASJON
            avsenderLand shouldBe "FINLAND"
            forsendelseJournalfoert.shouldBeNull()
            forsendelseMottatt shouldBe safJournalpost.relevanteDatoer()
                .filter(RelevantDato::harDatotypeRegistrert)
                .map(RelevantDato::dato)
                .map(this@JoarkServiceTest::tilInstant)
                .first()
            innhold shouldBe safJournalpost.tittel()
            korrespondansepartId shouldBe safJournalpost.avsenderMottaker().id()
            korrespondansepartNavn shouldBe safJournalpost.avsenderMottaker().navn()
            mottaksKanal shouldBe safJournalpost.kanal()
            tema shouldBe safJournalpost.tema()
        }

        val safHovedDokument = safJournalpost.dokumenter().iterator().next()
        val safLogiskVedlegg = safHovedDokument.logiskeVedlegg()[0]
        val safDokumentVariant = safHovedDokument.dokumentvarianter()[0]

        journalpost.hoveddokument.run {
            dokumentId shouldBe safHovedDokument.dokumentInfoId()
            tittel shouldBe safHovedDokument.tittel()
            navSkjemaID shouldBe safHovedDokument.brevkode()

            logiskeVedlegg[0].run {
                logiskVedleggID shouldBe safLogiskVedlegg.logiskVedleggId()
                tittel shouldBe safLogiskVedlegg.tittel()
            }

            dokumentVarianter[0].run {
                saksbehandlerHarTilgang shouldBe safDokumentVariant.saksbehandlerHarTilgang()
                variantFormat.name shouldBe safDokumentVariant.variantformat()
            }
        }
    }

    private fun tilInstant(localDateTime: LocalDateTime): Instant =
        localDateTime.atZone(ZoneId.systemDefault()).toInstant()

    @Test
    fun `hent mottaksdato for journalpost når journalpost finnes returnerer mottaksdato`() {
        val journalpostID = "12421"
        val safJournalpost = safJournalpost(journalpostID)
        val forventetMottaksdato = safJournalpost.relevanteDatoer()
            .filter(RelevantDato::harDatotypeRegistrert)
            .map(RelevantDato::dato)
            .map(LocalDateTime::toLocalDate)
            .first()

        every { safConsumer.hentJournalpost(journalpostID) } returns safJournalpost

        joarkService.hentMottaksDatoForJournalpost(journalpostID).shouldNotBeNull() shouldBe forventetMottaksdato
    }

    @Test
    fun `ferdigstill journalpost når journalpost blir journalført ingen exception`() {
        val journalpostId = "123"
        every { journalpostapiConsumer.ferdigstillJournalpost(capture(ferdigstillJournalpostCaptor), eq(journalpostId)) } just Runs

        joarkService.ferdigstillJournalføring(journalpostId)

        verify { journalpostapiConsumer.ferdigstillJournalpost(any(), eq(journalpostId)) }
        ferdigstillJournalpostCaptor.captured.journalfoerendeEnhet shouldBe Konstanter.MELOSYS_ENHET_ID.toString()
    }

    @Test
    fun `opprett journalpost uten validering forvent metodekall`() {
        every { journalpostapiConsumer.opprettJournalpost(any<OpprettJournalpostRequest>(), any()) } returns
            OpprettJournalpostResponse.builder().journalpostId("1234").build()

        val journalpostId = joarkService.opprettJournalpost(lagOpprettJournalpost(), false)

        verify { journalpostapiConsumer.opprettJournalpost(any<OpprettJournalpostRequest>(), any()) }
        journalpostId.shouldNotBeNull()
    }

    @Test
    fun `opprett journalpost med valider felt forvent validert`() {
        every { journalpostapiConsumer.opprettJournalpost(any<OpprettJournalpostRequest>(), any()) } returns
            OpprettJournalpostResponse.builder().journalpostId("1234").build()

        val journalpostId = joarkService.opprettJournalpost(lagOpprettJournalpost(), true)

        verify { journalpostapiConsumer.opprettJournalpost(any<OpprettJournalpostRequest>(), any()) }
        journalpostId.shouldNotBeNull()
    }

    @Test
    fun `opprett journalpost med valider felt forvent exception`() {
        val opprettJournalpost = lagOpprettJournalpost().apply {
            saksnummer = null
        }

        val exception = shouldThrow<FunksjonellException> {
            joarkService.opprettJournalpost(opprettJournalpost, true)
        }

        exception.message shouldContain "Saksnummer mangler"
        verify(exactly = 0) { journalpostapiConsumer.opprettJournalpost(any<OpprettJournalpostRequest>(), any()) }
    }

    @Test
    fun `opprett journalpost når forsendelse mottatt er satt forvent dato mottatt`() {
        val opprettJournalpost = lagOpprettJournalpost().apply {
            forsendelseMottatt = Instant.now()
        }
        val captor = slot<OpprettJournalpostRequest>()

        every { journalpostapiConsumer.opprettJournalpost(capture(captor), any()) } returns
            OpprettJournalpostResponse.builder().journalpostId("1234").build()

        joarkService.opprettJournalpost(opprettJournalpost, false)

        verify { journalpostapiConsumer.opprettJournalpost(any(), any()) }
        val opprettJournalpostRequest = captor.captured
        opprettJournalpostRequest.shouldNotBeNull()
        opprettJournalpostRequest.datoMottatt shouldBe LocalDate.ofInstant(opprettJournalpost.forsendelseMottatt, ZoneId.systemDefault())
    }

    private fun lagOpprettJournalpost() = OpprettJournalpost().apply {
        journalposttype = no.nav.melosys.domain.arkiv.Journalposttype.UT
        journalførendeEnhet = "9999"
        tema = "tema"
        mottaksKanal = "kanal"
        innhold = "innhold"
        saksnummer = "MEL-111"
        brukerId = "12345678901"
        brukerIdType = BrukerIdType.FOLKEREGISTERIDENT
        korrespondansepartNavn = "navn"
        korrespondansepartId = "id"
        setKorrespondansepartIdType(OpprettJournalpost.KorrespondansepartIdType.UTENLANDSK_ORGANISASJON)

        setHoveddokument(FysiskDokument().apply {
            tittel = "tittel"
            brevkode = "brevkode"
            dokumentVarianter = listOf(no.nav.melosys.domain.arkiv.DokumentVariant.lagDokumentVariant("dokument".toByteArray()))
            dokumentKategori = DokumentKategoriKode.SED.name
        })
    }

    private fun safJournalpost(journalpostID: String): Journalpost =
        safJournalpost(journalpostID, true)

    private fun safJournalpost(journalpostID: String, medLogiskVedlegg: Boolean): Journalpost =
        safJournalpost(journalpostID, Journalstatus.MOTTATT, medLogiskVedlegg)

    private fun safJournalpost(journalpostID: String, journalstatus: Journalstatus, medLogiskVedlegg: Boolean): Journalpost {
        val logiskVedlegg = LogiskVedlegg("4143", "Tittel logisk vedlegg")
        val dokumentVedlegg = DokumentVariant(true, Variantformat.ARKIV.name)
        return Journalpost(
            journalpostID,
            "Tittel",
            journalstatus,
            Tema.MED.kode,
            Journalposttype.I,
            Sak("MEL-123"),
            Bruker("123123", Brukertype.FNR),
            AvsenderMottaker("010101", AvsenderMottakerType.ORGNR, "Org AS", "FINLAND"),
            "SKAN_NETS",
            setOf(RelevantDato(LocalDateTime.now(), Datotype.DATO_REGISTRERT)),
            listOf(
                DokumentInfo(
                    "123",
                    "hoveddokument kommer først",
                    null,
                    if (medLogiskVedlegg) listOf(logiskVedlegg) else emptyList(),
                    listOf(dokumentVedlegg)
                ),
                DokumentInfo(
                    VEDLEGG_MED_TILGANG_ID, "vedlegg kommer etterpå", null, emptyList(), listOf(
                        DokumentVariant(true, Variantformat.ARKIV.name)
                    )
                ),
                DokumentInfo(
                    VEDLEGG_UTEN_TILGANG_ID, "tredje dokument", null, emptyList(), listOf(
                        DokumentVariant(false, Variantformat.ARKIV.name)
                    )
                )
            )
        )
    }

    private fun safJournalpostUtenVedlegg(): Journalpost =
        Journalpost(
            "11122233",
            "Tittel",
            Journalstatus.MOTTATT,
            Tema.MED.kode,
            Journalposttype.I,
            Sak("MEL-123"),
            Bruker("123123", Brukertype.FNR),
            AvsenderMottaker("010101", AvsenderMottakerType.ORGNR, "Org AS", null),
            "SKAN_NETS",
            setOf(RelevantDato(LocalDateTime.now(), Datotype.DATO_REGISTRERT)),
            listOf(DokumentInfo("123", "hoveddokument kommer først", null, emptyList(), emptyList()))
        )

    companion object {
        private const val VEDLEGG_MED_TILGANG_ID = "124"
        private const val VEDLEGG_UTEN_TILGANG_ID = "125"
    }
}
