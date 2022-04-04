package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.UrlPattern
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.integrasjon.aareg.AaregService
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdContextExchangeFilter
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdRestConsumer
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdRestConsumerConfig
import no.nav.melosys.integrasjon.kodeverk.Kode
import no.nav.melosys.integrasjon.kodeverk.KodeOppslag
import no.nav.melosys.integrasjon.reststs.RestStsClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.test.web.client.MockRestServiceServer
import java.time.LocalDate

@RestClientTest(
    value = [
        StsRestTemplateProducer::class,
        RestStsClient::class,
        WebClientAutoConfiguration::class,

        AaregService::class,
        ArbeidsforholdRestConsumer::class,
        ArbeidsforholdRestConsumerConfig::class,
        ArbeidsforholdContextExchangeFilter::class,
    ],
    properties = ["spring.profiles.active:itest-token"]
)
abstract class AaregServiceTestBase(
    server: MockRestServiceServer,
    private val aaregService: AaregService,
    mockPort: Int
) : ConsumerTestBase<String>(server, mockPort) {

    override fun createWireMock(): MappingBuilder {
        return WireMock.get(UrlPattern.ANY)
    }

    override fun getMockData(): String {
        return "[]"
    }

    override fun executeRequest() {
        val start = LocalDate.of(2022, 3, 26)
        val stop = LocalDate.of(2022, 3, 27)
        aaregService.finnArbeidsforholdPrArbeidstaker("121", start, stop)
    }

    class KodeOppslagImpl : KodeOppslag {
        override fun getTermFraKodeverk(kodeverk: FellesKodeverk?, kode: String?): String {
            return FellesKodeverk.ARBEIDSTIDSORDNINGER.name
        }

        override fun getTermFraKodeverk(kodeverk: FellesKodeverk?, kode: String?, dato: LocalDate?): String {
            return FellesKodeverk.ARBEIDSTIDSORDNINGER.name
        }
        override fun getTermFraKodeverk(
            kodeverk: FellesKodeverk?,
            kode: String?,
            dato: LocalDate?,
            kodeperioder: MutableList<Kode>?
        ): String {
            return FellesKodeverk.ARBEIDSTIDSORDNINGER.name
        }
    }

}

