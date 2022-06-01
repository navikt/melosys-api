package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.nav.melosys.integrasjon.doksys.dokumentproduksjon.DokumentproduksjonConsumer
import no.nav.melosys.integrasjon.doksys.dokumentproduksjon.DokumentproduksjonConsumerConfig
import no.nav.melosys.integrasjon.doksys.dokumentproduksjon.DokumentproduksjonConsumerProducer
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import no.nav.melosys.sikkerhet.sts.StsLogin
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.test.web.client.MockRestServiceServer

@RestClientTest(
    value = [
        StsRestTemplateProducer::class,

        DokumentproduksjonConsumerConfig::class,
        DokumentproduksjonConsumerProducer::class,
        StsLogin::class,
    ],
    properties = ["spring.profiles.active:itest-token"]
)
class DokumentproduksjonConsumerIT(
    @Autowired private val dokumentproduksjonConsumer: DokumentproduksjonConsumer,
    @Autowired server: MockRestServiceServer,
    @Value("\${mockserver.port}") mockPort: Int,
    @Value("\${mockserver.security.port}") mockSecurityUrl: Int
) : ConsumerTestBase<String>(server, mockPort) {

    private val securityWireMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(mockSecurityUrl))

    @BeforeAll
    fun beforeAllForDokumentproduksjonConsumer() {
        securityWireMockServer.start()
    }

    @AfterAll
    fun afterAllForDokumentproduksjonConsumer() {
        securityWireMockServer.stop()
    }

    @BeforeEach
    fun setupForDokumentproduksjonConsumer() {
        securityWireMockServer.resetAll()
    }

    override fun createWireMock(): MappingBuilder = post("/soap/services/dokumentproduksjon/v3")

    @Test
    fun authorizationSkalKommeFraSystem() {
        securityWireMockServer.stubFor(
            defaultSecurityServiceWireMockMappings()
                .withRequestBody(
                    notMatching(".*BinarySecurityToken.*")
                )
        )
        executeFromSystem {
            verifyHeaders(
                soapActionHeader()
            )
        }
    }

    @Test
    fun authorizationSkalKommeFraBruker() {
        val binarySecurityToken = """
                <wsse:BinarySecurityToken
                        xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
                        EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
                        ValueType="urn:ietf:params:oauth:token-type:jwt">LS10b2tlbi1mcm9tLXVzZXItLQ==
                </wsse:BinarySecurityToken>
        """.trimIndent()

        securityWireMockServer.stubFor(
            defaultSecurityServiceWireMockMappings()
                .withRequestBody(
                    matchingXPath(
                        "/Envelope/Body/RequestSecurityToken/OnBehalfOf/BinarySecurityToken",
                        equalToXml(binarySecurityToken)
                    )
                )
        )
        executeFromController {
            verifyHeaders(
                soapActionHeader()
            )
        }
    }

    @Test
    fun authorizationSkalKommeFraSystemNårHverkenSystemEllerBrukerErKilde() {
        securityWireMockServer.stubFor(
            defaultSecurityServiceWireMockMappings()
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

    override fun executeRequest() {
        dokumentproduksjonConsumer.produserIkkeredigerbartDokument(ProduserIkkeredigerbartDokumentRequest())
    }

    private fun defaultSecurityServiceWireMockMappings(): MappingBuilder =
        post("/SecurityTokenServiceProvider/")
            .withRequestBody(
                matchingXPath(
                    "/Envelope/Header/Security/UsernameToken/Username",
                    equalToXml("<wsse:Username>srvmelosys</wsse:Username>")
                )
            )
            .withRequestBody(
                matchingXPath(
                    "/Envelope/Header/Security/UsernameToken/Password",
                    equalToXml(
                        """
                        <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText">
                            dummy
                        </wsse:Password>
                    """.trimIndent()
                    )
                )
            )
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(sts_response)
            )

    private val sts_response = """
        <?xml version="1.0" encoding="UTF-8"?>
        <soapenv:Envelope xmlns:wsa="http://www.w3.org/2005/08/addressing" xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
            <soapenv:Header>
                <wsa:MessageID>urn:uuid:5e22dd04-7a8e-494a-a05e-2fff16ecf883</wsa:MessageID>
                <wsa:Action>http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTRC/IssueFinal</wsa:Action>
                <wsa:To>http://www.w3.org/2005/08/addressing/anonymous</wsa:To>
            </soapenv:Header>
            <soapenv:Body>
                <wst:RequestSecurityTokenResponseCollection xmlns:wst="http://docs.oasis-open.org/ws-sx/ws-trust/200512">
                    <wst:RequestSecurityTokenResponse Context="supportLater">
                     <wst:TokenType>http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0</wst:TokenType>
                        <wst:RequestedSecurityToken>
                            <saml2:Assertion Version="2.0" ID="SAML-8d11de08-b17f-45ba-bd18-68098a4d28ce" IssueInstant="2018-09-06T10:28:45Z"
                                             xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">
                                <saml2:Issuer>SomeOne</saml2:Issuer>
                            </saml2:Assertion>
                        </wst:RequestedSecurityToken>
                    </wst:RequestSecurityTokenResponse>
                </wst:RequestSecurityTokenResponseCollection>
            </soapenv:Body>
        </soapenv:Envelope>
        """.trimIndent()
}
