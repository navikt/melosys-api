package no.nav.melosys.integrasjon.oppgave.konsument

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.domain.readResourceAsString
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveDto
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveSearchRequest
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OpprettOppgaveDto
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.ZonedDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OppgaveConsumerTest {

    private lateinit var oppgaveConsumer: OppgaveConsumer
    private lateinit var mockServer: MockWebServer

    @BeforeAll
    fun setupServer() {
        mockServer = MockWebServer()
        mockServer.start()
    }

    @AfterAll
    fun tearDown() {
        mockServer.shutdown()
    }

    @BeforeEach
    fun setup() {
        oppgaveConsumer = OppgaveConsumer(WebClient.builder().baseUrl("http://localhost:${mockServer.port}").build())
    }

    @Test
    fun `hentOppgave oppgave finnes verifiserer mapping`() {
        mockServer.enqueue(
            MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(readResourceAsString(OPPGAVE_GET_JSON_PATH))
        )


        val oppgave = oppgaveConsumer.hentOppgave("1")


        oppgave!!.run {
            id shouldBe "11519"
            tildeltEnhetsnr shouldBe "4530"
            journalpostId shouldBe "439654251"
            behandlesAvApplikasjon shouldBe "FS38"
            saksreferanse shouldBe "MEL-301"
            aktørId shouldBe "1332607802528"
            orgnr shouldBe "orgnr"
            tilordnetRessurs shouldBe "Z990757"
            beskrivelse shouldBe " "
            tema shouldBe "MED"
            behandlingstema shouldBe "ab0390"
            oppgavetype shouldBe "BEH_SED"
            versjon shouldBe 3
            prioritet shouldBe "NORM"
            status shouldBe "AAPNET"
            fristFerdigstillelse shouldBe LocalDate.parse("2019-12-26")
            aktivDato shouldBe LocalDate.parse("2019-10-03")
            opprettetTidspunkt shouldBe ZonedDateTime.parse("2019-10-03T10:27:26.566Z")
        }
    }

    @Test
    fun `hentOppgave oppgave finnes ikke 404 status kaster IkkeFunnetException`() {
        mockServer.enqueue(
            MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(404)
                .setBody(readResourceAsString(OPPGAVE_FEILMELDING_JSON_PATH))
        )


        val exception = shouldThrow<IkkeFunnetException> {
            oppgaveConsumer.hentOppgave("1")
        }


        exception.message shouldContain "Fant ingen oppgave"
    }

    @Test
    fun `hentOppgaveListe mottar to oppgaver verifiserer mapping`() {
        mockServer.enqueue(
            MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(readResourceAsString(OPPGAVELIST_GET_JSON_PATH))
        )

        val searchRequest = OppgaveSearchRequest.Builder("123").apply {
            medOppgaveTyper(arrayOf("BEH_SED", "BEH_SAK"))
            medAktørId("123")
            medBehandlingstema("ab2344")
            medBehandlingsType("ba432?")
            medBehandlesAvApplikasjon("FS38")
            medTema("MED", "UFM")
            medStatusKategori("AAPEN")
        }.build()


        val oppgaver = oppgaveConsumer.hentOppgaveListe(searchRequest)


        oppgaver.map { it.saksreferanse } shouldContainExactlyInAnyOrder listOf("MEL-301", "MEL-513")
    }

    @Test
    fun `oppdaterOppgave oppgave oppdateres verifiserer mapping`() {
        mockServer.enqueue(
            MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(readResourceAsString(OPPGAVE_GET_JSON_PATH))
        )


        val result = oppgaveConsumer.oppdaterOppgave(OppgaveDto())


        result.id shouldBe "11519"
    }

    @Test
    fun `opprettOppgave oppgave opprettes verifiserer mapping`() {
        mockServer.enqueue(
            MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(readResourceAsString(OPPGAVE_GET_JSON_PATH))
        )


        val oppgaveID = oppgaveConsumer.opprettOppgave(OpprettOppgaveDto())


        oppgaveID shouldBe "11519"
    }

    companion object {
        private const val OPPGAVE_GET_JSON_PATH = "mock/oppgave/oppgave_get.json"
        private const val OPPGAVELIST_GET_JSON_PATH = "mock/oppgave/hentOppgaveListe_get.json"
        private const val OPPGAVE_FEILMELDING_JSON_PATH = "mock/oppgave/feil.json"
    }
}
