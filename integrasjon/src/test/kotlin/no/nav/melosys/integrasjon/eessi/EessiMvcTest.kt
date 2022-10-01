package no.nav.melosys.integrasjon.eessi

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.UrlPattern
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.FieldsEqualityCheckConfig
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.arkiv.Vedlegg
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.eessi.sed.SedDataDto
import no.nav.melosys.domain.eessi.sed.SedGrunnlagA003Dto
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.ConsumerWireMockTestBase
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto
import no.nav.melosys.integrasjon.felles.GenericContextClientRequestInterceptor
import no.nav.melosys.integrasjon.felles.GenericContextExchangeFilter
import no.nav.melosys.integrasjon.reststs.RestStsClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.ActiveProfiles
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.util.*

@WebMvcTest(
    value = [
        StsRestTemplateProducer::class,
        RestStsClient::class,

        GenericContextExchangeFilter::class,
        EessiConsumerProducer::class,
        GenericContextClientRequestInterceptor::class
    ]
)
@ActiveProfiles("wiremock-test")
@AutoConfigureWebClient
class EessiMvcTest(
    @Autowired private val eessiConsumer: EessiConsumer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
    @Value("\${mockserver.security.port}") mockSecurityPort: Int
) : ConsumerWireMockTestBase<String, OpprettSedDto>(mockServiceUnderTestPort, mockSecurityPort) {

    @Test
    fun opprettBucOgSed() {
        setupWireMock(
            WireMock.post("/api/buc/LA_BUC_01?sendAutomatisk=true&oppdaterEksisterende=true")
        )
        executeFromSystemFunc {
            val opprettSedDto = eessiConsumer.opprettBucOgSed(
                SedDataDto(),
                setOf(Vedlegg("pdf".toByteArray(), "tittel")),
                BucType.LA_BUC_01,
                true,
                true
            )
            opprettSedDto.rinaSaksnummer.shouldBe("12345")
        }
    }

    @Test
    fun opprettBucOgSed_forventException() {
        setupWireMock(
            WireMock.post("/api/buc/LA_BUC_01?sendAutomatisk=true&oppdaterEksisterende=true"),
            response = WireMock.aResponse().withStatus(500)
        )
        executeFromSystemFunc {
            shouldThrow<TekniskException> {
                eessiConsumer.opprettBucOgSed(
                    SedDataDto(), null, BucType.LA_BUC_01, true, true
                )
            }.message.shouldContain("kall til eessi feilet")
        }
    }

    @Test
    fun sendSedPåEksisterendeBuc() {
        setupWireMock(
            WireMock.post("/api/buc/12345/sed/A001")
        )
        executeFromSystemFunc {
            eessiConsumer.sendSedPåEksisterendeBuc(
                SedDataDto(),
                "12345",
                SedType.A001
            )
        }
    }

    @Test
    fun hentMottakerinstitusjoner() {
        setupWireMock(
            WireMock.get("/api/buc/LA_BUC_01/institusjoner?land=DE,PL"),
            data = "[{\"id\":\"NO:NAVT002\",\"navn\":\"NAVT002\",\"landkode\":\"NO\"}]"
        )
        executeFromSystemFunc {
            val institusjoner = eessiConsumer.hentMottakerinstitusjoner("LA_BUC_01", listOf("DE", "PL"))
            institusjoner
                .shouldHaveSize(1)
                .first().apply {
                    id.shouldBe("NO:NAVT002")
                    navn.shouldBe("NAVT002")
                    landkode.shouldBe("NO")
                }
        }
    }

    @Test
    fun hentMelosysEessiMeldingFraJournalpostID() {
        val journalpostID = "115314"
        val melosysEessiMelding = MelosysEessiMelding().apply {
            sedType = "A009"
            journalpostId = journalpostID
        }

        setupWireMock(
            WireMock.get("/api/journalpost/$journalpostID/eessimelding"),
            data = ObjectMapper().writeValueAsString(melosysEessiMelding)
        )

        executeFromSystemFunc {
            val response = eessiConsumer.hentMelosysEessiMeldingFraJournalpostID(journalpostID)
            response.apply {
                sedType.shouldBe(melosysEessiMelding.sedType)
                journalpostId.shouldBe(melosysEessiMelding.journalpostId)
            }
        }
    }

    @Test
    fun lagreSaksrelasjon() {
        val saksrelasjonDto = SaksrelasjonDto(123L, "123", "123")

        setupWireMock(
            WireMock.post("/api/sak")
                .withRequestBody(WireMock.equalToJson(ObjectMapper().writeValueAsString(saksrelasjonDto))),
        )

        executeFromSystemFunc {
            eessiConsumer.lagreSaksrelasjon(saksrelasjonDto)
        }
    }

    @Test
    fun hentSakForRinasaksnummer() {
        val saksrelasjon = SaksrelasjonDto().apply {
            rinaSaksnummer = "114422"
            gsakSaksnummer = 123L
            bucType = "LA_BUC_04"
        }
        setupWireMock(
            WireMock.get("/api/sak?rinaSaksnummer=${saksrelasjon.rinaSaksnummer}"),
            data = ObjectMapper().writeValueAsString(listOf(saksrelasjon))
        )

        executeFromSystemFunc {
            val saksRelasjoner: MutableList<SaksrelasjonDto> =
                eessiConsumer.hentSakForRinasaksnummer(saksrelasjon.rinaSaksnummer)
            saksRelasjoner
                .shouldHaveSize(1)
                .first()
                .shouldBeEqualToComparingFields(saksrelasjon, FieldsEqualityCheckConfig(ignorePrivateFields = false))
        }
    }

    @Test
    fun genererSedPdf() {
        val PDF = "pdf"

        setupWireMock(
            WireMock.post("/api/sed/A001/pdf"),
            data = PDF
        )

        executeFromSystemFunc {
            val pdf: ByteArray = eessiConsumer.genererSedPdf(SedDataDto(), SedType.A001)
            pdf.shouldBe(PDF.toByteArray())
        }
    }

    @Test
    fun hentTilknyttedeBucer_medEnStatus_forventBucer() {
        val uri = Objects.requireNonNull(javaClass.classLoader.getResource("mock/eux/bucer.json")).toURI()
        val json = String(Files.readAllBytes(Paths.get(uri)))
        setupWireMock(
            WireMock.get("/api/sak/1/bucer?statuser=UTKAST"),
            data = json
        )
        executeFromSystemFunc {
            val hentTilknyttedeBucer = eessiConsumer.hentTilknyttedeBucer(1L, listOf("UTKAST"))

            hentTilknyttedeBucer
                .shouldHaveSize(2)
                .apply {
                    first().apply {
                        id.shouldBe("111111")
                        bucType.shouldBe("LA_BUC_03")
                        erÅpen().shouldBe(true)
                        opprettetDato.shouldBe(LocalDate.of(2019, 4, 4))
                        seder.shouldBeSingleton {
                            it.sedId.shouldBe("22223333")
                            it.sedType.shouldBe("A008")
                            it.status.shouldBe("UTKAST")
                        }
                    }
                    last().apply {
                        id.shouldBe("222222")
                        bucType.shouldBe("LA_BUC_01")
                        erÅpen().shouldBe(false)
                        opprettetDato.shouldBe(LocalDate.of(2019, 4, 4))
                        seder.shouldHaveSize(2).apply {
                            first().apply {
                                sedId.shouldBe("11221122")
                                sedType.shouldBe("A002")
                                status.shouldBe("UTKAST")
                            }
                            last().apply {
                                sedId shouldBe ("11332233")
                                sedType.shouldBe("A001")
                                status.shouldBe("UTKAST")
                            }
                        }
                    }
                }
        }
    }

    @Test
    fun hentTilknyttedeBucer_medFlereStatuser_forventRettSti() {
        setupWireMock(
            WireMock.get("/api/sak/1/bucer?statuser=UTKAST,MOTTATT,SENDT"),
            data = "[]"
        )
        executeFromSystemFunc {
            eessiConsumer.hentTilknyttedeBucer(1L, listOf("UTKAST", "MOTTATT", "SENDT"))
        }
    }

    @Test
    fun hentTilknyttedeBucer_medToStatuser_forventRettSti() {
        setupWireMock(
            WireMock.get("/api/sak/1/bucer?statuser=SENDT,UTKAST"),
            data = "[]"
        )
        executeFromSystemFunc {
            eessiConsumer.hentTilknyttedeBucer(1L, listOf("SENDT", "UTKAST"))
        }
    }

    @Test
    fun hentSedGrunnlag() {
        setupWireMock(
            WireMock.get("/api/buc/1234/sed/abcdef/grunnlag"),
            data = "{\"sedType\": \"A003\"}"
        )
        executeFromSystemFunc {
            val rinaSaksnummer = "1234"
            val rinaDokumentID = "abcdef"
            val response = eessiConsumer.hentSedGrunnlag(rinaSaksnummer, rinaDokumentID)
            response.shouldBeInstanceOf<SedGrunnlagA003Dto>()
        }
    }

    @Test
    fun lukkBuc() {
        val rinaSaksnummer = "1424"
        setupWireMock(
            WireMock.post("/api/buc/$rinaSaksnummer/lukk")
        )
        executeFromSystemFunc {
            eessiConsumer.lukkBuc(rinaSaksnummer)
        }
    }


    @Test
    fun skalBrukeErrorFilterOgGiRiktigFeilmelding() {
        setupWireMock(
            WireMock.post(UrlPattern.ANY)
        )
        executeErrorFromServer { error ->
            Assertions.assertThat(error).startsWith("kall til eessi feilet")
        }
    }

    override fun getMockData(): String {
        return "{\"rinaSaksnummer\":\"12345\",\"rinaUrl\":\"localhost:3000\"}"
    }

    override fun executeRequest(): OpprettSedDto {
        return eessiConsumer.opprettBucOgSed(
            SedDataDto(),
            setOf(Vedlegg("pdf".toByteArray(), "tittel")),
            BucType.LA_BUC_01,
            true,
            true
        )
    }
}
