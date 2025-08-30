package no.nav.melosys.integrasjon.oppgave

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Fagsystem
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.domain.oppgave.PrioritetType
import no.nav.melosys.integrasjon.Konstanter
import no.nav.melosys.integrasjon.oppgave.konsument.OppgaveConsumer
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveDto
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveSearchRequest
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OpprettOppgaveDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@ExtendWith(MockKExtension::class)
internal class OppgaveFasadeImplTest {

    private val oppgaveConsumer = mockk<OppgaveConsumer>()

    private lateinit var oppgaveFasadeImpl: OppgaveFasadeImpl

    @BeforeEach
    fun setup() {
        oppgaveFasadeImpl = OppgaveFasadeImpl(oppgaveConsumer)
    }

    @Test
    fun `opprettOppgave vurderer dokument setter data`() {
        val behandlingstema = "ae9999"
        val oppgaveBuilder = Oppgave.Builder().apply {
            setOppgavetype(Oppgavetyper.VUR)
            setTema(Tema.MED)
            setBehandlingstema(behandlingstema)
            setFristFerdigstillelse(LocalDate.now())
        }
        every { oppgaveConsumer.opprettOppgave(any()) } returns "123"


        oppgaveFasadeImpl.opprettOppgave(oppgaveBuilder.build())


        val captor = slot<OpprettOppgaveDto>()
        verify { oppgaveConsumer.opprettOppgave(capture(captor)) }
        val opprettOppgaveDto = captor.captured

        opprettOppgaveDto.run {
            oppgavetype shouldBe Oppgavetyper.VUR.kode
            behandlingstema shouldBe behandlingstema
            fristFerdigstillelse.shouldNotBeNull()
        }
    }

    @Test
    fun `opprettOppgave gyldig oppgave validerer dto`() {
        val oppgave = lagOppgave()
        every { oppgaveConsumer.opprettOppgave(any()) } returns "123"


        oppgaveFasadeImpl.opprettOppgave(oppgave)


        val opprettOppgaveDtoCaptor = slot<OpprettOppgaveDto>()
        verify { oppgaveConsumer.opprettOppgave(capture(opprettOppgaveDtoCaptor)) }

        opprettOppgaveDtoCaptor.captured.shouldNotBeNull().run {
            journalpostId shouldBe oppgave.journalpostId
            aktørId shouldBe oppgave.aktørId
            orgnr shouldBe oppgave.orgnr
            behandlesAvApplikasjon shouldBe Fagsystem.MELOSYS.kode
            beskrivelse shouldBe OppgaveFasadeImpl.hentNyBeskrivelseHendelseslogg("bla bla", "sak123")
            oppgavetype shouldBe oppgave.oppgavetype.kode
            prioritet shouldBe PrioritetType.NORM.toString()
            tema shouldBe oppgave.tema.kode
            tildeltEnhetsnr shouldBe Konstanter.MELOSYS_ENHET_ID.toString()
            tilordnetRessurs shouldBe oppgave.tilordnetRessurs
        }
    }

    @Test
    fun `finnOppgaveListeMedAnsvarlig gyldig oppgave verifiserer to kall mot oppgave`() {
        val oppgaveDto = OppgaveDto()
        every { oppgaveConsumer.hentOppgaveListe(any<OppgaveSearchRequest>()) } returns listOf(oppgaveDto)


        oppgaveFasadeImpl.finnOppgaverMedAnsvarlig("123")


        val oppgaveSearchRequestCaptor = mutableListOf<OppgaveSearchRequest>()
        verify(exactly = 2) { oppgaveConsumer.hentOppgaveListe(capture(oppgaveSearchRequestCaptor)) }

        oppgaveSearchRequestCaptor.shouldHaveSize(2).toList().run {
            get(0).behandlesAvApplikasjon shouldBe Fagsystem.MELOSYS.kode
            get(1).behandlesAvApplikasjon shouldBe null
            get(1).oppgavetype!![0] shouldBe Oppgavetyper.JFR.kode
        }
    }

    @Test
    fun `finnOppgaveListeMedAnsvarlig to duplikate oppgaver filtrerer ut duplikater`() {
        val oppgaveID = "123duplikat"
        val oppgaveDto1 = OppgaveDto().apply { id = oppgaveID }
        val oppgaveDto2 = OppgaveDto().apply { id = oppgaveID }
        every { oppgaveConsumer.hentOppgaveListe(any<OppgaveSearchRequest>()) } returns listOf(oppgaveDto1, oppgaveDto2)


        val oppgaver = oppgaveFasadeImpl.finnOppgaverMedAnsvarlig("123")


        oppgaver.shouldHaveSize(1).single()
            .oppgaveId shouldBe oppgaveID
    }

    @Test
    fun `test mapping mellom DTO og domain for oppgave`() {
        val oppgaveDto = OppgaveDto().apply {
            id = "1234"
            saksreferanse = "456"
            oppgavetype = "BEH_SAK_MK"
            tema = "MED"
            saksreferanse = "MEL-111"
        }
        every { oppgaveConsumer.hentOppgave("1234") } returns oppgaveDto


        val oppgave = oppgaveFasadeImpl.hentOppgave("1234")


        oppgave.run {
            oppgaveId shouldBe "1234"
            saksnummer shouldBe "MEL-111"
            oppgavetype shouldBe Oppgavetyper.valueOf("BEH_SAK_MK")
            tema shouldBe Tema.valueOf("MED")
        }
    }

    @Test
    fun `finnUtildelteOppgaverEtterFrist mottar oppgave med og uten saksreferanse returnerer oppgave med saksreferanse`() {
        val jfrOppgave = OppgaveDto().apply { oppgavetype = "JFR" }
        val behOppgave = OppgaveDto().apply { saksreferanse = "MEL-123" }
        every { oppgaveConsumer.hentOppgaveListe(any<OppgaveSearchRequest>()) } returns listOf(jfrOppgave, behOppgave)


        val oppgaver = oppgaveFasadeImpl.finnUtildelteOppgaverEtterFrist(null)


        oppgaver.shouldHaveSize(1).single()
            .saksnummer shouldBe "MEL-123"
    }

    @Test
    fun `oppdaterOppgave mapper oppgave oppdatering til oppgave dto riktig`() {
        val oppgaveDto = OppgaveDto().apply { mappeId = "321" }
        every { oppgaveConsumer.hentOppgave("123") } returns oppgaveDto
        every { oppgaveConsumer.oppdaterOppgave(any()) } returns mockk()

        val oppgaveOppdatering = OppgaveOppdatering.builder()
            .oppgavetype(Oppgavetyper.JFR)
            .tema(Tema.MED)
            .behandlesAvApplikasjon(Fagsystem.MELOSYS)
            .saksnummer("saksnr")
            .behandlingstema("behandlingstema")
            .behandlingstype("behandlingstype")
            .prioritet("prioritet #1")
            .status("heeelt ferdig")
            .tilordnetRessurs("Z133337")
            .fristFerdigstillelse(LocalDate.now())
            .build()


        oppgaveFasadeImpl.oppdaterOppgave("123", oppgaveOppdatering)


        val oppgaveDtoArgumentCaptor = slot<OppgaveDto>()
        verify { oppgaveConsumer.oppdaterOppgave(capture(oppgaveDtoArgumentCaptor)) }
        val capturedDto = oppgaveDtoArgumentCaptor.captured
        capturedDto.run {
            oppgavetype shouldBe Oppgavetyper.JFR.kode
            tema shouldBe Tema.MED.kode
            behandlesAvApplikasjon shouldBe Fagsystem.MELOSYS.kode
            saksreferanse shouldBe "saksnr"
            behandlingstema shouldBe "behandlingstema"
            behandlingstype shouldBe "behandlingstype"
            prioritet shouldBe "prioritet #1"
            status shouldBe "heeelt ferdig"
            tilordnetRessurs shouldBe "Z133337"
            fristFerdigstillelse shouldBe LocalDate.now()
            mappeId shouldBe "321"
        }
    }

    @Test
    fun `oppdaterOppgave formaterer beskrivelseslogg riktig når beskrivelse eksisterer`() {
        val oppgaveDto = OppgaveDto().apply { beskrivelse = "Testy test" }
        every { oppgaveConsumer.hentOppgave("123") } returns oppgaveDto
        every { oppgaveConsumer.oppdaterOppgave(any()) } returns mockk()

        val oppgaveOppdatering = OppgaveOppdatering.builder()
            .behandlingstema("UTSENDT_ARBEIDSTAKER")
            .beskrivelse("Ny beskrivelse")
            .saksnummer("MEL-123")
            .build()


        oppgaveFasadeImpl.oppdaterOppgave("123", oppgaveOppdatering)


        val oppgaveDtoArgumentCaptor = slot<OppgaveDto>()
        verify { oppgaveConsumer.oppdaterOppgave(capture(oppgaveDtoArgumentCaptor)) }
        val oppdateringstidspunkt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        oppgaveDtoArgumentCaptor.captured.beskrivelse shouldBe
            "--- $oppdateringstidspunkt (srvmelosys, ${Fagsystem.MELOSYS.beskrivelse}) ---\n Ny beskrivelse - MEL-123\n\nTesty test"
    }

    @Test
    fun `oppdaterOppgave formaterer beskrivelseslogg riktig når beskrivelse ikke eksisterer`() {
        val oppgaveDto = OppgaveDto()
        every { oppgaveConsumer.hentOppgave("123") } returns oppgaveDto
        every { oppgaveConsumer.oppdaterOppgave(any()) } returns mockk()

        val oppgaveOppdatering = OppgaveOppdatering.builder()
            .behandlingstema("UTSENDT_ARBEIDSTAKER")
            .beskrivelse("Ny beskrivelse")
            .saksnummer("MEL-123")
            .build()


        oppgaveFasadeImpl.oppdaterOppgave("123", oppgaveOppdatering)


        val oppgaveDtoArgumentCaptor = slot<OppgaveDto>()
        verify { oppgaveConsumer.oppdaterOppgave(capture(oppgaveDtoArgumentCaptor)) }
        val oppdateringstidspunkt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        oppgaveDtoArgumentCaptor.captured.beskrivelse shouldBe
            "--- $oppdateringstidspunkt (srvmelosys, Melosys) ---\n Ny beskrivelse - MEL-123\n"
    }

    private fun lagOppgave() = Oppgave.Builder().apply {
        setAktivDato(LocalDate.now())
        setAktørId("aktoer123")
        setOrgnr("orgnr")
        setBehandlingstema("abbehandlingstema1234")
        setBeskrivelse("bla bla")
        setOpprettetTidspunkt(ZonedDateTime.now())
        setFristFerdigstillelse(LocalDate.now().plusMonths(1L))
        setOppgaveId("123")
        setOppgavetype(Oppgavetyper.BEH_SAK_MK)
        setJournalpostId("journalpost123")
        setPrioritet(PrioritetType.NORM)
        setSaksnummer("sak123")
        setStatus("tildet")
        setTema(Tema.MED)
        setTemagruppe("temagruppe")
        setTildeltEnhetsnr("4530")
        setTilordnetRessurs("ressurs123")
        setMappeId("321")
    }.build()
}
