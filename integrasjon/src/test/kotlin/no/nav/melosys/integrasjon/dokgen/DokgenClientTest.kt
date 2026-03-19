package no.nav.melosys.integrasjon.dokgen

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.matchers.shouldNotBe
import no.nav.melosys.domain.*
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.integrasjon.MetricsTestConfig
import no.nav.melosys.integrasjon.dokgen.dto.MangelbrevBruker
import no.nav.melosys.integrasjon.dokgen.dto.standardvedlegg.InnvilgelseRettigheterPlikterStandardvedlegg
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import java.util.UUID

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        CorrelationIdOutgoingFilter::class,
        DokgenClientProducer::class,
        MetricsTestConfig::class,
    ]
)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DokgenClientTest(
    @Autowired private val dokgenClient: DokgenClient,
    @Value("\${mockserver.port}") mockServerPort: Int,
) {
    private val processUUID = UUID.randomUUID()
    private val mockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(mockServerPort))

    @BeforeAll
    fun beforeAll() {
        ThreadLocalAccessInfo.beforeExecuteProcess(processUUID, "prosessSteg")
        mockServer.start()
    }

    @AfterAll
    fun afterAll() {
        mockServer.stop()
        ThreadLocalAccessInfo.afterExecuteProcess(processUUID)
    }

    @BeforeEach
    fun beforeEach() {
        mockServer.resetAll()
    }

    @Test
    fun `lagPdf serialiserer request body korrekt`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("pdf")
            )
        )

        dokgenClient.lagPdf("mangelbrev_bruker", getMangelbrevBruker(), false, false) shouldNotBe null

        // Asserts kritiske serialiseringsegenskaper: enum-felter som rene strenger (ikke {kode,term}-objekter)
        // og dato-felter i ISO-8601-format (ikke arrays).
        mockServer.verify(
            postRequestedFor(urlPathEqualTo("/dokgen/mal/mangelbrev_bruker/lag-pdf"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .withRequestBody(matchingJsonPath("$.sakstype", equalTo("EU_EOS")))
                .withRequestBody(matchingJsonPath("$.sakstema", equalTo("MEDLEMSKAP_LOVVALG")))
                .withRequestBody(matchingJsonPath("$.dagensDato", matching("\\d{4}-\\d{2}-\\d{2}T.+")))
        )
    }

    @Test
    fun `lagPdf skal bestille brev`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("pdf")
            )
        )

        dokgenClient.lagPdf("mangelbrev_bruker", getMangelbrevBruker(), false, false) shouldNotBe null

        mockServer.verify(
            postRequestedFor(urlPathEqualTo("/dokgen/mal/mangelbrev_bruker/lag-pdf"))
                .withQueryParam("somKopi", equalTo("false"))
                .withQueryParam("utkast", equalTo("false"))
        )
    }

    @Test
    fun `lagPdf skal bestille brev som kopi`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("pdf")
            )
        )

        dokgenClient.lagPdf("mangelbrev_bruker", getMangelbrevBruker(), true, false) shouldNotBe null

        mockServer.verify(
            postRequestedFor(urlPathEqualTo("/dokgen/mal/mangelbrev_bruker/lag-pdf"))
                .withQueryParam("somKopi", equalTo("true"))
                .withQueryParam("utkast", equalTo("false"))
        )
    }

    @Test
    fun `lagPdf skal bestille brev som utkast`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("pdf")
            )
        )

        dokgenClient.lagPdf("mangelbrev_bruker", getMangelbrevBruker(), true, true) shouldNotBe null

        mockServer.verify(
            postRequestedFor(urlPathEqualTo("/dokgen/mal/mangelbrev_bruker/lag-pdf"))
                .withQueryParam("somKopi", equalTo("true"))
                .withQueryParam("utkast", equalTo("true"))
        )
    }

    @Test
    fun `lagPdfForStandardvedlegg med data skal bestille brev`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("pdf")
            )
        )

        val standardvedlegg = InnvilgelseRettigheterPlikterStandardvedlegg("Hei")

        dokgenClient.lagPdfForStandardvedlegg("standardvedlegg", standardvedlegg) shouldNotBe null
    }

    @Test
    fun `lagPdfForStandardvedlegg uten data skal bestille brev`() {
        mockServer.stubFor(
            any(anyUrl()).willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("pdf")
            )
        )

        dokgenClient.lagPdfForStandardvedlegg("standardvedlegg", null) shouldNotBe null
    }

    private fun getMangelbrevBruker() = MangelbrevBruker.av(
        MangelbrevBrevbestilling.Builder()
            .medBehandling(lagBehandling())
            .medPersonDokument(lagPersondokument().dokument as Persondata)
            .medPersonMottaker(lagPersondokument().dokument as Persondata)
            .build(), Instant.now()
    )

    private fun lagBehandling() = Behandling.forTest {
        fagsak {
            type = Sakstyper.EU_EOS
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            status = Saksstatuser.OPPRETTET
        }
        type = Behandlingstyper.FØRSTEGANG
        saksopplysninger = mutableSetOf(lagPersondokument())
    }

    private fun lagPersondokument() = Saksopplysning().apply {
        dokument = PersonDokument()
        type = SaksopplysningType.PERSOPL
    }
}
