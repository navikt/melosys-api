package no.nav.melosys.integrasjon.joark.saf

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.exception.IntegrasjonException
import no.nav.melosys.exception.SikkerhetsbegrensningException
import no.nav.melosys.integrasjon.felles.graphql.GraphQLResponse
import no.nav.melosys.integrasjon.joark.saf.dto.HentDokumentoversiktResponse
import no.nav.melosys.integrasjon.joark.saf.dto.HentDokumentoversiktResponseWrapper
import no.nav.melosys.integrasjon.joark.saf.dto.SideInfo
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.*
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.jetbrains.annotations.NotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.io.IOException
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

class SafConsumerTest {
    private lateinit var safConsumer: SafConsumer

    private lateinit var mockServer: MockWebServer

    private val objectWriter: ObjectWriter = ObjectMapper().writer()

    @BeforeEach
    fun setup() {
        mockServer = MockWebServer().apply {
            start()
        }

        safConsumer = SafConsumer(WebClient.builder().baseUrl("http://localhost:${mockServer.port}").build())
    }

    @Test
    fun `hentDokument når dokument finnes forvent PDF`() {
        mockServer.enqueue(
            MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .setBody("pdf")
        )

        val pdf = safConsumer.hentDokument(JOURNALPOST_ID, DOKUMENT_ID)

        pdf shouldBe byteArrayOf('p'.code.toByte(), 'd'.code.toByte(), 'f'.code.toByte())
    }

    @Test
    fun `hentDokument når dokument finnes ikke 404 status kaster IkkeFunnetException`() {
        mockServer.enqueue(
            MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(404)
                .setBody(HENT_DOKUMENT_404_RESPONSE)
        )

        val exception = shouldThrow<IkkeFunnetException> {
            safConsumer.hentDokument(JOURNALPOST_ID, DOKUMENT_ID)
        }

        exception.message shouldContain "ikke funnet"
    }

    @Test
    fun `hentDokument ikke autentisert kaster SikkerhetsbegrensningException`() {
        mockServer.enqueue(
            MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(401)
                .setBody(HENT_DOKUMENT_401_RESPONSE)
        )

        val exception = shouldThrow<SikkerhetsbegrensningException> {
            safConsumer.hentDokument(JOURNALPOST_ID, DOKUMENT_ID)
        }

        exception.message shouldContain "no valid token"
    }

    @Test
    fun `hentJournalpost når journalpost finnes ikke kaster IntegrasjonException`() {
        mockServer.enqueue(
            MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(200)
                .setBody(HENT_JOURNALPOST_IKKE_FUNNET_RESPONSE)
        )

        val exception = shouldThrow<IntegrasjonException> {
            safConsumer.hentJournalpost(JOURNALPOST_ID)
        }

        exception.message shouldContain "ikke funnet"
    }

    @Test
    fun `hentJournalpost ikke autentisert kaster SikkerhetsbegrensningException`() {
        mockServer.enqueue(
            MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(401)
                .setBody(HENT_JOURNALPOST_401_RESPONSE)
        )

        val exception = shouldThrow<SikkerhetsbegrensningException> {
            safConsumer.hentJournalpost(JOURNALPOST_ID)
        }

        exception.message shouldContain "no valid token"
    }

    @Test
    fun `hentDokumentoversikt ingen paginering forvent antall journalposter og kall`() {
        mockServer.enqueue(responseAv("peker", false))

        val journalposter = safConsumer.hentDokumentoversikt("MEL-1")

        journalposter shouldHaveSize 10
        mockServer.requestCount shouldBe 1
    }

    @Test
    fun `hentDokumentoversikt med paginering forvent antall journalposter og kall`() {
        mockServer.dispatcher = object : Dispatcher() {
            @NotNull
            override fun dispatch(@NotNull request: RecordedRequest): MockResponse {
                return when {
                    harPeker(request, "p1") -> responseAv("p2", true)
                    harPeker(request, "p2") -> responseAv("p3", true)
                    harPeker(request, "p3") -> responseAv("p4", false)
                    else -> responseAv("p1", true)
                }
            }
        }

        val journalposter = safConsumer.hentDokumentoversikt("MEL-1")

        journalposter shouldHaveSize 40
        mockServer.requestCount shouldBe 4
    }

    private fun harPeker(req: RecordedRequest, peker: String): Boolean {
        return try {
            req.body.peek().readUtf8().contains(peker)
        } catch (e: IOException) {
            throw RuntimeException("Kunne ikke lese request")
        }
    }

    private fun responseAv(nestePeker: String, finnesNeste: Boolean): MockResponse {
        return try {
            MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(200)
                .setBody(objectWriter.writeValueAsString(lagHentDokumentoversiktResponse(nestePeker, finnesNeste)))
        } catch (e: JsonProcessingException) {
            throw RuntimeException("Kunne ikke serialisere response")
        }
    }

    private fun lagHentDokumentoversiktResponse(peker: String, finnesNeste: Boolean): GraphQLResponse<HentDokumentoversiktResponseWrapper> =
        GraphQLResponse(
            HentDokumentoversiktResponseWrapper(
                HentDokumentoversiktResponse(
                    lagJournalposter(10),
                    SideInfo(peker, finnesNeste)
                )
            ), Collections.emptyList()
        )

    private fun lagJournalposter(antall: Int): List<Journalpost> =
        Stream.generate { lagJournalpost() }.limit(antall.toLong()).collect(Collectors.toList())

    private fun lagJournalpost(): Journalpost =
        Journalpost(
            "id",
            "tittel",
            Journalstatus.JOURNALFOERT,
            "MED",
            Journalposttype.I,
            Sak("123"),
            Bruker("123", Brukertype.FNR),
            AvsenderMottaker("123", AvsenderMottakerType.FNR, "navn", null),
            "SED",
            Collections.emptyList(),
            listOf(
                DokumentInfo(
                    "1",
                    "dok-tittel",
                    "brevkode",
                    Collections.emptyList(),
                    listOf(DokumentVariant(true, "ARKIV"))
                )
            )
        )

    companion object {
        private const val JOURNALPOST_ID = "1"
        private const val DOKUMENT_ID = "1"

        private const val HENT_DOKUMENT_401_RESPONSE = """
        {
          "timestamp": "2021-03-25T08:47:33.594+00:00",
          "status": 401,
          "error": "Unauthorized",
          "message": "no.nav.security.token.support.core.exceptions.JwtTokenMissingException: no valid token found in validation context",
          "path": "/rest/hentdokument/1/1/ARKIV"
        }
        """

        private const val HENT_DOKUMENT_404_RESPONSE = """
        {
          "timestamp": "2021-03-25T08:47:33.594+00:00",
          "status": 404,
          "error": "Not Found",
          "message": "Dokumentet tilnyttet journalpostId=1, dokumentInfoId=1, variant=ARKIV ikke funnet.",
          "path": "/rest/hentdokument/1/1/ARKIV"
        }
        """

        private const val HENT_JOURNALPOST_401_RESPONSE = """
        {
          "timestamp": "2021-03-25T08:47:33.594+00:00",
          "status": 401,
          "error": "Unauthorized",
          "message": "no.nav.security.token.support.core.exceptions.JwtTokenMissingException: no valid token found in validation context",
          "path": "/graphql"
        }
        """

        private const val HENT_JOURNALPOST_IKKE_FUNNET_RESPONSE = """
        {
            "errors": [
                {
                    "message": "Journalpost med journalpostId=1 ikke funnet.",
                    "locations": [],
                    "extensions": {
                        "classification": "DataFetchingException"
                    }
                }
            ],
            "data": {
                "query": null
            }
        }
        """

    }
}
