package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.integrasjon.aareg.AaregService
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdContextExchangeFilter
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdRestConsumerConfig
import no.nav.melosys.integrasjon.felles.AutoContextExchangeFilter
import no.nav.melosys.integrasjon.kodeverk.Kode
import no.nav.melosys.integrasjon.kodeverk.KodeOppslag
import no.nav.melosys.integrasjon.reststs.RestStsClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.client.MockRestServiceServer
import java.time.LocalDate
import java.util.*

@RestClientTest(
    value = [
        AaregService::class,
        ArbeidsforholdRestConsumerConfig::class,
        ArbeidsforholdContextExchangeFilter::class,
        AutoContextExchangeFilter::class,
        StsRestTemplateProducer::class,
        RestStsClient::class
    ],
    properties = ["spring.profiles.active:itest-token"]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AaregServiceIT(
    @Autowired private val server: MockRestServiceServer,
    @Autowired private val aaregService: AaregService,
    @Value("\${mockserver.port}") mockPort: Int,
) : ConsumerTestBase<String>(server, mockPort) {

    override fun createWireMock(): MappingBuilder {
        return WireMock.get("/?regelverk=A_ORDNINGEN&ansettelsesperiodeFom=2022-03-26&ansettelsesperiodeTom=2022-03-27")
    }

    override fun getMockData(): String {
        return "[]"
    }

    @TestConfiguration
    class TestConfig {
        @Bean
        fun kodeOppslag(): KodeOppslag {
            return KodeOppslagImpl()
        }
    }

    @Test
    fun testRequestFromFront() {
        ThreadLocalAccessInfo.preHandle("request")
        runTest()
        ThreadLocalAccessInfo.afterCompletion("request")
    }

    @Test
    fun testRequestFromProsess() {
        val uuid = UUID.randomUUID()
        ThreadLocalAccessInfo.beforExecuteProcess(uuid, "prossesSteg")
        runTest()
        ThreadLocalAccessInfo.afterExecuteProcess(uuid)
    }

    private fun runTest() {
        SpringSubjectHandler.set(TestSubjectHandler())

        verifyHeaders(
            mapOf<String, StringValuePattern>(
                Pair("Authorization", WireMock.equalTo("Bearer --token-from-service--")),
                Pair("Nav-Personident", WireMock.equalTo("121"))
            )
        )

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
