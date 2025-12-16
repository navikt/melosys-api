package no.nav.melosys.itest.mock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.extension.Extension
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.integrasjon.faktureringskomponenten.NyFakturaserieResponseDto
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Container-based equivalent of AvgiftFaktureringTestBase.
 *
 * Uses:
 * - Melosys-mock container for journalføring
 * - WireMock (in-process) for trygdeavgiftsberegning and fakturering APIs
 */
abstract class ContainerAvgiftFaktureringTestBase(
    extensionForWireMock: Extension? = null
) : ContainerJournalfoeringBase(extensionForWireMock) {

    protected abstract val fakturaserieReferanse: String

    private var originalSubjectHandler: SubjectHandler? = null

    @BeforeEach
    fun beforeMockForAvgiftFaktureringTestBase() {
        originalSubjectHandler = SubjectHandler.getInstance()

        val mockHandler = mockk<SpringSubjectHandler>()
        SubjectHandler.set(mockHandler)
        every { mockHandler.userID } returns "Z123456"
        every { mockHandler.userName } returns "test"

        mockServer.stubFor(
            WireMock.post("/api/v2/eos-pensjonist/beregn")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withTransformers("dynamisk-trygdeavgiftsberegning-eos-pensjonist-transformer")
                )
        )

        mockServer.addMockServiceRequestListener { request, response ->
            println("Request URL: ${request.url}")
            println("Request Body: ${request.bodyAsString}")
            println("Response Status: ${response.status}")
        }


        mockServer.stubFor(
            WireMock.post("/api/v2/beregn")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withTransformers("dynamisk-trygdeavgiftsberegning-transformer")
                )
        )

        val fakturaResponse = NyFakturaserieResponseDto(fakturaserieReferanse)

        mockServer.stubFor(
            WireMock.post("/fakturaserier")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(fakturaResponse.toJsonNode.toString())
                )
        )
        mockServer.stubFor(
            WireMock.delete(WireMock.urlMatching("/fakturaserier/.*"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(fakturaResponse.toJsonNode.toString())
                )
        )
        mockServer.stubFor(
            WireMock.post(WireMock.urlMatching("/fakturaer"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(fakturaResponse.toJsonNode.toString())
                )
        )
    }

    @AfterEach
    fun afterMockForAvgiftFaktureringTestBase() {
        SubjectHandler.set(originalSubjectHandler)
    }
}
