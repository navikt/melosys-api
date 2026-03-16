package no.nav.melosys.integrasjon.dokgen

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.kotest.matchers.shouldNotBe
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.integrasjon.dokgen.dto.standardvedlegg.StandardvedleggDto
import org.springframework.http.codec.json.JacksonJsonEncoder
import tools.jackson.databind.json.JsonMapper
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
class DokgenClientTest {

    private lateinit var wireMockServer: WireMockServer
    private lateinit var dokgenClient: DokgenClient

    @BeforeAll
    fun setup() {
        wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
        wireMockServer.start()

        val webClient = WebClient.builder()
            .baseUrl("http://localhost:" + wireMockServer.port())
            .build()

        dokgenClient = DokgenClient(webClient)
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


        dokgenClient.lagPdf("mangelbrev_bruker", getMangelbrevBruker(), false, false) shouldNotBe null
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


        dokgenClient.lagPdf("mangelbrev_bruker", getMangelbrevBruker(), true, false) shouldNotBe null
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


        dokgenClient.lagPdf("mangelbrev_bruker", getMangelbrevBruker(), true, true) shouldNotBe null
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


        dokgenClient.lagPdfForStandardvedlegg("standardvedlegg", standardvedlegg) shouldNotBe null
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


        dokgenClient.lagPdfForStandardvedlegg("standardvedlegg", null) shouldNotBe null
    }

    @Test
    fun `JacksonJsonEncoder serialiserer Kodeverk-enum som plain string, ikke KodeDto-objekt`() {
        // Verifiserer at DokgenClientProducer sin JacksonJsonEncoder(ukonfigurertObjectMapper) faktisk
        // brukes av WebClient ved serialisering av request-body — ikke Spring Boot sin default encoder.
        // Hvis MelosysModule hadde vært aktiv ville Behandlingsmaate blitt serialisert som
        // {"kode":"MANUELT","term":"..."} i stedet for "MANUELT", og dokgen-kallet ville feilet.
        val dokgenObjectMapper = JsonMapper.builder().build()
        val webClientMedEncoder = WebClient.builder()
            .baseUrl("http://localhost:" + wireMockServer.port())
            .codecs { it.defaultCodecs().jacksonJsonEncoder(JacksonJsonEncoder(dokgenObjectMapper)) }
            .build()
        val dokgenClientMedEncoder = DokgenClient(webClientMedEncoder)

        wireMockServer.stubFor(
            post(urlPathEqualTo("/mal/testmal/lag-pdf"))
                .withRequestBody(matchingJsonPath("$.behandlingsmaate", equalTo("MANUELT")))
                .willReturn(aResponse().withStatus(200).withBody("pdf".toByteArray()))
        )

        dokgenClientMedEncoder.lagPdfForStandardvedlegg("testmal", TestDtoMedKodeverk(Behandlingsmaate.MANUELT)) shouldNotBe null
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

    private data class TestDtoMedKodeverk(val behandlingsmaate: Behandlingsmaate) : StandardvedleggDto
}
