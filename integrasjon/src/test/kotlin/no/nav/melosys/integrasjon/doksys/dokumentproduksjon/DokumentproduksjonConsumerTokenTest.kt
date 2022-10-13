package no.nav.melosys.integrasjon.doksys.dokumentproduksjon

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.nav.melosys.integrasjon.ConsumerWireMockTestBase
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.sikkerhet.sts.*
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentRequest
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentResponse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Import(
    StsLoginConfig::class,
    StsProdWrapper::class,
    StsTestWrapper::class,
    OAuthMockServer::class,

    DokumentproduksjonConsumerConfig::class,
    DokumentproduksjonConsumerProducer::class
)
@WebMvcTest
@AutoConfigureWebClient
@ActiveProfiles("wiremock-test")

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

    @Test
    fun authorizationSkalKommeFraSystem() {
        stsMockServer.stubFor(
            defaultSecurityServiceWireMockMappings()
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
        val binarySecurityToken = """
                <wsse:BinarySecurityToken
                        xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
                        EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
                        ValueType="urn:ietf:params:oauth:token-type:jwt">LS10b2tlbi1mcm9tLXVzZXItLQ==
                </wsse:BinarySecurityToken>
        """.trimIndent()

        stsMockServer.stubFor(
            defaultSecurityServiceWireMockMappings()
                .withRequestBody(
                    matchingXPath(
                        "/Envelope/Body/RequestSecurityToken/OnBehalfOf/BinarySecurityToken",
                        equalToXml(binarySecurityToken)
                    )
                )
        )
        verifyHeaders(
            soapActionHeader()
        )
        executeFromController()
    }

    @Test
    fun authorizationSkalKommeFraSystemNårHverkenSystemEllerBrukerErKilde() {
        stsMockServer.stubFor(
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

    override fun executeRequest() =
        dokumentproduksjonConsumer.produserIkkeredigerbartDokument(ProduserIkkeredigerbartDokumentRequest())

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
