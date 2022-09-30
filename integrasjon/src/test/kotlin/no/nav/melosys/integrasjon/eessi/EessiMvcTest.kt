package no.nav.melosys.integrasjon.eessi

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.UrlPattern
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.FieldsEqualityCheckConfig
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.arkiv.Vedlegg
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.eessi.sed.SedDataDto
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
        executeFromSystem { opprettSedDto ->
            opprettSedDto.rinaSaksnummer.shouldBe("12345")
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
