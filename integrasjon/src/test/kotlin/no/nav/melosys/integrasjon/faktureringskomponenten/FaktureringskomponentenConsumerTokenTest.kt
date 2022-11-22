package no.nav.melosys.integrasjon.faktureringskomponenten

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.UrlPattern
import no.nav.melosys.integrasjon.ConsumerWireMockTestBase
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaserieDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaseriePeriodeDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FaktureringsIntervall
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FullmektigDto
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
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate

@WebMvcTest(
    value = [
        StsRestTemplateProducer::class,
        RestStsClient::class,
        FaktureringskomponentenConsumer::class,
        GenericContextExchangeFilter::class,
        FaktureringskomponentenConsumerProducer::class,
        GenericContextClientRequestInterceptor::class
    ]
)
@ActiveProfiles("wiremock-test")
@AutoConfigureWebClient
class FaktureringskomponentenConsumerTokenTest(
    @Autowired private val faktureringskomponentenConsumer: FaktureringskomponentenConsumer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
    @Value("\${mockserver.security.port}") mockSecurityPort: Int
) : ConsumerWireMockTestBase<String, String>(mockServiceUnderTestPort, mockSecurityPort) {

    @Test
    fun authorizationSkalKommeFraSystem() {
        verifyHeaders(
            mapOf(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
                Pair(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE)),
                Pair(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
            )
        )
        executeFromSystem()
    }

    @Test
    fun authorizationSkalKommeFraSystemNårHverkenSystemEllerBrukerErKilde() {
        verifyHeaders(
            mapOf(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-system--")),
                Pair(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE)),
                Pair(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
            )
        )
        executeRequest()
    }

    @Test
    fun correlationIdLeggesPåRequest() {
        verifyHeaders(
            mapOf(
                Pair("X-Correlation-ID", WireMock.matching(UUID_REGEX)),
            )
        )
        executeRequest()
    }

    @Test
    fun skalBrukeErrorFilterOgGiRiktigFeilmelding() {
        executeErrorFromServer { error ->
            Assertions.assertThat(error).contains(errorFromServerMessage())
        }
    }

    override fun createWireMock(): MappingBuilder {
        return WireMock.post(UrlPattern.ANY)
    }

    override fun getMockData(): String = "[]"

    override fun executeRequest() =
        faktureringskomponentenConsumer.lagFakturaSerie(lagFakturaserieDto())


    private fun lagFakturaserieDto(
        vedtaksnummer: String = "MEL-123",
        fodselsnummer: String = "12345678911",
        fullmektig: FullmektigDto = FullmektigDto("11987654321", "123456789", "Ole Brum"),
        referanseBruker: String = "Nasse Nøff",
        referanseNav: String = "NAV Medlemskap og avgift",
        intervall: FaktureringsIntervall = FaktureringsIntervall.KVARTAL,
        fakturaseriePeriode: List<FakturaseriePeriodeDto> = listOf(
            FakturaseriePeriodeDto(
                BigDecimal.valueOf(123),
                LocalDate.now(),
                LocalDate.now(),
                "Beskrivelse"
            )
        ),
    ): FakturaserieDto {
        return FakturaserieDto(
            vedtaksnummer,
            fodselsnummer,
            fullmektig,
            referanseBruker,
            referanseNav,
            intervall,
            fakturaseriePeriode
        )
    }
}

