package no.nav.melosys.integrasjon.dokgen

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.kotest.matchers.shouldNotBe
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.integrasjon.dokgen.dto.MangelbrevBruker
import no.nav.melosys.integrasjon.dokgen.dto.standardvedlegg.InnvilgelseRettigheterPlikterStandardvedlegg
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.web.reactive.function.client.WebClient
import java.nio.charset.StandardCharsets
import java.time.Instant

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DokgenConsumerTest {

    private lateinit var wireMockServer: WireMockServer
    private lateinit var dokgenConsumer: DokgenConsumer

    @BeforeAll
    fun setup() {
        wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
        wireMockServer.start()

        val webClient = WebClient.builder()
            .baseUrl("http://localhost:" + wireMockServer.port())
            .build()

        dokgenConsumer = DokgenConsumer(webClient)
    }

    @AfterAll
    fun tearDown() {
        wireMockServer.stop()
    }

    @Test
    fun `lagPdf skal bestille brev`() {
        wireMockServer.stubFor(
            post(urlPathEqualTo("/mal/mangelbrev_bruker/lag-pdf"))
                .withQueryParam("somKopi", equalTo("false"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("pdf".toByteArray(StandardCharsets.UTF_8))
                )
        )


        dokgenConsumer.lagPdf("mangelbrev_bruker", getMangelbrevBruker(), false, false) shouldNotBe null
    }

    @Test
    fun `lagPdf skal bestille brev som kopi`() {
        wireMockServer.stubFor(
            post(urlPathEqualTo("/mal/mangelbrev_bruker/lag-pdf"))
                .withQueryParam("somKopi", equalTo("true"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("pdf".toByteArray(StandardCharsets.UTF_8))
                )
        )


        dokgenConsumer.lagPdf("mangelbrev_bruker", getMangelbrevBruker(), true, false) shouldNotBe null
    }

    @Test
    fun `lagPdf skal bestille brev som utkast`() {
        wireMockServer.stubFor(
            post(urlPathEqualTo("/mal/mangelbrev_bruker/lag-pdf"))
                .withQueryParam("somKopi", equalTo("true"))
                .withQueryParam("utkast", equalTo("true"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("pdf".toByteArray(StandardCharsets.UTF_8))
                )
        )


        dokgenConsumer.lagPdf("mangelbrev_bruker", getMangelbrevBruker(), true, true) shouldNotBe null
    }

    @Test
    fun `lagPdfForStandardvedlegg med data skal bestille brev`() {
        val standardvedlegg = InnvilgelseRettigheterPlikterStandardvedlegg("Hei")
        wireMockServer.stubFor(
            post(urlPathEqualTo("/mal/standardvedlegg/lag-pdf"))
                .withQueryParam("somKopi", equalTo("false"))
                .withQueryParam("utkast", equalTo("false"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("pdf".toByteArray(StandardCharsets.UTF_8))
                )
        )


        dokgenConsumer.lagPdfForStandardvedlegg("standardvedlegg", standardvedlegg) shouldNotBe null
    }

    @Test
    fun `lagPdfForStandardvedlegg uten data skal bestille brev`() {
        wireMockServer.stubFor(
            post(urlPathEqualTo("/mal/standardvedlegg/lag-pdf"))
                .withQueryParam("somKopi", equalTo("false"))
                .withQueryParam("utkast", equalTo("false"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("pdf".toByteArray(StandardCharsets.UTF_8))
                )
        )


        dokgenConsumer.lagPdfForStandardvedlegg("standardvedlegg", null) shouldNotBe null
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
