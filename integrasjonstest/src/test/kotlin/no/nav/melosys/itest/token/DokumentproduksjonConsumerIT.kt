package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.nav.melosys.integrasjon.doksys.dokumentproduksjon.DokumentproduksjonConsumer
import no.nav.melosys.integrasjon.doksys.dokumentproduksjon.DokumentproduksjonConsumerConfig
import no.nav.melosys.integrasjon.doksys.dokumentproduksjon.DokumentproduksjonConsumerImpl
import no.nav.melosys.integrasjon.doksys.dokumentproduksjon.DokumentproduksjonConsumerProducer
import no.nav.melosys.integrasjon.reststs.RestStsClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import no.nav.melosys.sikkerhet.sts.StsLogin
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentRequest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.test.web.client.MockRestServiceServer

@RestClientTest(
    value = [
        StsRestTemplateProducer::class,
        RestStsClient::class,
        WebClientAutoConfiguration::class,

        DokumentproduksjonConsumerImpl::class,
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
) : ConsumerTestBase<String>(server, mockPort) {

    private val securityWireMockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(mockPort + 1))

    @BeforeAll
    fun beforeAll2() {
        securityWireMockServer.start()
    }

    @Test
    fun test() {
        dokumentproduksjonConsumer.produserIkkeredigerbartDokument(ProduserIkkeredigerbartDokumentRequest())
    }

    override fun createWireMock(): MappingBuilder {
        return WireMock.post("/soap/services/dokumentproduksjon/v3")
    }

    @Test
    fun authorizationSkalKommeFraSystem() {
        securityWireMockServer.start()

        securityWireMockServer.stubFor(WireMock.get("")
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""{"jwt":"token"}"""))
        )


        executeFromSystem {
            verifyHeaders(
                mapOf<String, StringValuePattern>(
//                    Pair("Authorization", WireMock.equalTo("Basic dGVzdDp0ZXN0")),
                )
            )
        }
    }

    @Test
    fun authorizationSkalKommeFraBruker() {
        executeFromController {
            verifyHeaders(
                mapOf<String, StringValuePattern>(
                    Pair("Authorization", WireMock.equalTo("Bearer --token-from-user--")),
                )
            )
        }
    }

    @Test
    fun authorizationSkalKommeFraSystemNårHverkenSystemEllerBrukerErKilde() {
        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Basic dGVzdDp0ZXN0")),
            )
        )
        executeRequest()
    }

    override fun getMockData(): String {
        return """
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
        """
    }

    override fun executeRequest() {
        dokumentproduksjonConsumer.produserIkkeredigerbartDokument(ProduserIkkeredigerbartDokumentRequest())
    }

}
