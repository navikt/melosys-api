package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import com.github.tomakehurst.wiremock.matching.UrlPattern
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class ConsumerWireMockTestBase<T>(
    mockRestServerProvider: MockRestServerProvider,
    mockPort: Int,
) : StsTestBase<T>(mockRestServerProvider) {
    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(mockPort))

    open fun createWireMock(): MappingBuilder = WireMock.get(UrlPattern.ANY)

    abstract fun getMockData(): T

    fun verifyHeaders(headers: Map<String, StringValuePattern>) {
        val wireMock = createWireMock()
        headers.forEach {
            wireMock.withHeader(it.key, it.value)
        }

        val response = WireMock.aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")

        val data = getMockData()
        if (data is String) response.withBody(data)
        if (data is ByteArray) response.withBody(data)

        wireMockServer.stubFor(
            wireMock.willReturn(response)
        )
    }

    @BeforeAll
    fun beforeAll_ConsumerWireMockTestBase() {
        wireMockServer.start()
    }

    @AfterAll
    fun afterAll_ConsumerWireMockTestBase() {
        wireMockServer.stop()
    }

    override fun stubError() {
        wireMockServer.stubFor(
            createWireMock()
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"melding\": \"Internal Server Error\"}")
                )
        )
    }
}
