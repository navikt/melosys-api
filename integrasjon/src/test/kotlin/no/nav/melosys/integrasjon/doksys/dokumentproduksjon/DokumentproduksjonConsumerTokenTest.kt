package no.nav.melosys.integrasjon.doksys.dokumentproduksjon

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.nav.melosys.integrasjon.ConsumerWireMockTestBase
import no.nav.melosys.integrasjon.MetricsTestConfig
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.reststs.RestSTSService
import no.nav.melosys.integrasjon.reststs.SecurityTokenServiceConsumer
import no.nav.melosys.integrasjon.reststs.StsWebClientProducer
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentRequest
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentResponse
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ActiveProfiles("wiremock-test")
@ContextConfiguration(
    classes = [
        OAuthMockServer::class,

        StsWebClientProducer::class,
        SecurityTokenServiceConsumer::class,
        RestSTSService::class,
        MetricsTestConfig::class,
        DokumentproduksjonConsumerConfig::class,
        DokumentproduksjonConsumerProducer::class
    ]
)
@AutoConfigureWebClient
class DokumentproduksjonConsumerTokenTest(
    @Autowired private val dokumentproduksjonConsumer: DokumentproduksjonConsumer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
    @Value("\${mockserver.security.port}") mockSecurityPort: Int,
    @Autowired oAuthMockServer: OAuthMockServer
) : ConsumerWireMockTestBase<String, ProduserIkkeredigerbartDokumentResponse>(
    mockServiceUnderTestPort,
    mockSecurityPort, oAuthMockServer
) {
    override fun createWireMock(): MappingBuilder = post("/soap/services/dokumentproduksjon/v3")

    override fun defaultStsWireMockStub() {}

    @AfterAll
    fun after() {
        SubjectHandler.set(SpringSubjectHandler(SpringTokenValidationContextHolder()))
    }

    @Test
    fun authorizationSkalKommeFraSystem() {
        stsMockServer.stubFor(
            defaultSecurityServiceWireMockMappingForSystem()
                .withRequestBody(
                    notMatching(".*BinarySecurityToken.*")

                )
        )
        verifyHeaders(
            soapActionHeader()
        )
        executeFromSystem()
    }

    @Test
    fun authorizationSkalKommeFraBruker() {
        SubjectHandler.set(object : SubjectHandler() {
            override fun getOidcTokenString() = "--token-from-user--"
            override fun getUserID() = ""
            override fun getUserName() = ""
            override fun getGroups() = mutableListOf<String>()
        })
        stsMockServer.stubFor(
            defaultSecurityServiceWireMockMappingForBruker()
                .withRequestBody(
                    notMatching(".*BinarySecurityToken.*")

                )
        )
        verifyHeaders(
            soapActionHeader()
        )
        verifyHeaders(
            soapActionHeader()
        )
        executeFromController()
    }

    @Test
    fun authorizationSkalKommeFraSystemNårHverkenSystemEllerBrukerErKilde() {
        stsMockServer.stubFor(
            defaultSecurityServiceWireMockMappingForSystem()
                .withRequestBody(
                    notMatching(".*BinarySecurityToken.*")
                )
        )
        verifyHeaders(
            soapActionHeader()
        )
        executeRequest()
    }

    private fun soapActionHeader() = mapOf<String, StringValuePattern>(
        Pair(
            "SOAPAction",
            equalTo("\"http://nav.no/tjeneste/virksomhet/dokumentproduksjon/v3/Dokumentproduksjon_v3/produserIkkeredigerbartDokumentRequest\"")
        )
    )

    override fun getMockData(): String = """
        <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
            <SOAP-ENV:Header/>
            <SOAP-ENV:Body>
                <ns3:produserIkkeredigerbartDokumentResponse
                        xmlns:ns3="http://nav.no/tjeneste/virksomhet/dokumentproduksjon/v3">
                    <response>
                        <journalpostId>989808</journalpostId>
                        <dokumentId>22616</dokumentId>
                    </response>
                </ns3:produserIkkeredigerbartDokumentResponse>
            </SOAP-ENV:Body>
        </SOAP-ENV:Envelope>
    """.trimIndent()

    override fun executeRequest() =
        dokumentproduksjonConsumer.produserIkkeredigerbartDokument(ProduserIkkeredigerbartDokumentRequest())

    private fun defaultSecurityServiceWireMockMappingForSystem(): MappingBuilder =
        get("/samltoken")
            .withHeader("Authorization", EqualToPattern("Basic dGVzdDp0ZXN0"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(sts_response)
            )

    private fun defaultSecurityServiceWireMockMappingForBruker(): MappingBuilder =
        post("/token/exchange?grant_type=urn:ietf:params:oauth:grant-type:token-exchange&requested_token_type=urn:ietf:params:oauth:token-type:saml2&subject_token_type=urn:ietf:params:oauth:token-type:access_token&subject_token=--token-from-user--")
            .withHeader("Authorization", EqualToPattern("Basic dGVzdDp0ZXN0"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(sts_response)
            )

    private val sts_response = """{
          "access_token": "PHNhbWxwOlJlc3BvbnNlIHhtbG5zOnNhbWxwPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6cHJvdG9jb2wiIHhtbG5zOnNhbWw9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphc3NlcnRpb24iIElEPSJfOGU4ZGM1ZjY5YTk4Y2M0YzFmZjM0MjdlNWNlMzQ2MDZmZDY3MmY5MWU2IiBWZXJzaW9uPSIyLjAiIElzc3VlSW5zdGFudD0iMjAxNC0wNy0xN1QwMTowMTo0OFoiIERlc3RpbmF0aW9uPSJodHRwOi8vc3AuZXhhbXBsZS5jb20vZGVtbzEvaW5kZXgucGhwP2FjcyIgSW5SZXNwb25zZVRvPSJPTkVMT0dJTl80ZmVlM2IwNDYzOTVjNGU3NTEwMTFlOTdmODkwMGI1MjczZDU2Njg1Ij4KICA8c2FtbDpJc3N1ZXI+aHR0cDovL2lkcC5leGFtcGxlLmNvbS9tZXRhZGF0YS5waHA8L3NhbWw6SXNzdWVyPgogIDxzYW1scDpTdGF0dXM+CiAgICA8c2FtbHA6U3RhdHVzQ29kZSBWYWx1ZT0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOnN0YXR1czpTdWNjZXNzIi8+CiAgPC9zYW1scDpTdGF0dXM+CiAgPHNhbWw6QXNzZXJ0aW9uIHhtbG5zOnhzaT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS9YTUxTY2hlbWEtaW5zdGFuY2UiIHhtbG5zOnhzPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSIgSUQ9Il9kNzFhM2E4ZTlmY2M0NWM5ZTlkMjQ4ZWY3MDQ5MzkzZmM4ZjA0ZTVmNzUiIFZlcnNpb249IjIuMCIgSXNzdWVJbnN0YW50PSIyMDE0LTA3LTE3VDAxOjAxOjQ4WiI+CiAgICA8c2FtbDpJc3N1ZXI+aHR0cDovL2lkcC5leGFtcGxlLmNvbS9tZXRhZGF0YS5waHA8L3NhbWw6SXNzdWVyPgogICAgPHNhbWw6U3ViamVjdD4KICAgICAgPHNhbWw6TmFtZUlEIFNQTmFtZVF1YWxpZmllcj0iaHR0cDovL3NwLmV4YW1wbGUuY29tL2RlbW8xL21ldGFkYXRhLnBocCIgRm9ybWF0PSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6bmFtZWlkLWZvcm1hdDp0cmFuc2llbnQiPl9jZTNkMjk0OGI0Y2YyMDE0NmRlZTBhMGIzZGQ2ZjY5YjZjZjg2ZjYyZDc8L3NhbWw6TmFtZUlEPgogICAgICA8c2FtbDpTdWJqZWN0Q29uZmlybWF0aW9uIE1ldGhvZD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmNtOmJlYXJlciI+CiAgICAgICAgPHNhbWw6U3ViamVjdENvbmZpcm1hdGlvbkRhdGEgTm90T25PckFmdGVyPSIyMDI0LTAxLTE4VDA2OjIxOjQ4WiIgUmVjaXBpZW50PSJodHRwOi8vc3AuZXhhbXBsZS5jb20vZGVtbzEvaW5kZXgucGhwP2FjcyIgSW5SZXNwb25zZVRvPSJPTkVMT0dJTl80ZmVlM2IwNDYzOTVjNGU3NTEwMTFlOTdmODkwMGI1MjczZDU2Njg1Ii8+CiAgICAgIDwvc2FtbDpTdWJqZWN0Q29uZmlybWF0aW9uPgogICAgPC9zYW1sOlN1YmplY3Q+CiAgICA8c2FtbDpDb25kaXRpb25zIE5vdEJlZm9yZT0iMjAxNC0wNy0xN1QwMTowMToxOFoiIE5vdE9uT3JBZnRlcj0iMjAyNC0wMS0xOFQwNjoyMTo0OFoiPgogICAgICA8c2FtbDpBdWRpZW5jZVJlc3RyaWN0aW9uPgogICAgICAgIDxzYW1sOkF1ZGllbmNlPmh0dHA6Ly9zcC5leGFtcGxlLmNvbS9kZW1vMS9tZXRhZGF0YS5waHA8L3NhbWw6QXVkaWVuY2U+CiAgICAgIDwvc2FtbDpBdWRpZW5jZVJlc3RyaWN0aW9uPgogICAgPC9zYW1sOkNvbmRpdGlvbnM+CiAgICA8c2FtbDpBdXRoblN0YXRlbWVudCBBdXRobkluc3RhbnQ9IjIwMTQtMDctMTdUMDE6MDE6NDhaIiBTZXNzaW9uTm90T25PckFmdGVyPSIyMDI0LTA3LTE3VDA5OjAxOjQ4WiIgU2Vzc2lvbkluZGV4PSJfYmU5OTY3YWJkOTA0ZGRjYWUzYzBlYjQxODlhZGJlM2Y3MWUzMjdjZjkzIj4KICAgICAgPHNhbWw6QXV0aG5Db250ZXh0PgogICAgICAgIDxzYW1sOkF1dGhuQ29udGV4dENsYXNzUmVmPnVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphYzpjbGFzc2VzOlBhc3N3b3JkPC9zYW1sOkF1dGhuQ29udGV4dENsYXNzUmVmPgogICAgICA8L3NhbWw6QXV0aG5Db250ZXh0PgogICAgPC9zYW1sOkF1dGhuU3RhdGVtZW50PgogICAgPHNhbWw6QXR0cmlidXRlU3RhdGVtZW50PgogICAgICA8c2FtbDpBdHRyaWJ1dGUgTmFtZT0idWlkIiBOYW1lRm9ybWF0PSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXR0cm5hbWUtZm9ybWF0OmJhc2ljIj4KICAgICAgICA8c2FtbDpBdHRyaWJ1dGVWYWx1ZSB4c2k6dHlwZT0ieHM6c3RyaW5nIj50ZXN0PC9zYW1sOkF0dHJpYnV0ZVZhbHVlPgogICAgICA8L3NhbWw6QXR0cmlidXRlPgogICAgICA8c2FtbDpBdHRyaWJ1dGUgTmFtZT0ibWFpbCIgTmFtZUZvcm1hdD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmF0dHJuYW1lLWZvcm1hdDpiYXNpYyI+CiAgICAgICAgPHNhbWw6QXR0cmlidXRlVmFsdWUgeHNpOnR5cGU9InhzOnN0cmluZyI+dGVzdEBleGFtcGxlLmNvbTwvc2FtbDpBdHRyaWJ1dGVWYWx1ZT4KICAgICAgPC9zYW1sOkF0dHJpYnV0ZT4KICAgICAgPHNhbWw6QXR0cmlidXRlIE5hbWU9ImVkdVBlcnNvbkFmZmlsaWF0aW9uIiBOYW1lRm9ybWF0PSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXR0cm5hbWUtZm9ybWF0OmJhc2ljIj4KICAgICAgICA8c2FtbDpBdHRyaWJ1dGVWYWx1ZSB4c2k6dHlwZT0ieHM6c3RyaW5nIj51c2Vyczwvc2FtbDpBdHRyaWJ1dGVWYWx1ZT4KICAgICAgICA8c2FtbDpBdHRyaWJ1dGVWYWx1ZSB4c2k6dHlwZT0ieHM6c3RyaW5nIj5leGFtcGxlcm9sZTE8L3NhbWw6QXR0cmlidXRlVmFsdWU+CiAgICAgIDwvc2FtbDpBdHRyaWJ1dGU+CiAgICA8L3NhbWw6QXR0cmlidXRlU3RhdGVtZW50PgogIDwvc2FtbDpBc3NlcnRpb24+Cjwvc2FtbHA6UmVzcG9uc2U+",
          "issued_token_type": "urn:ietf:params:oauth:token-type:saml2",
          "token_type": "Bearer",
          "expires_in": 3162
        }
        """.trimIndent()
}
