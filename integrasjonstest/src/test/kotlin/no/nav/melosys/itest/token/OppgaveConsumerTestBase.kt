package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.melosys.integrasjon.felles.SystemContextExchangeFilter
import no.nav.melosys.integrasjon.felles.UserContextExchangeFilter
import no.nav.melosys.integrasjon.oppgave.konsument.OppgaveConsumerImpl
import no.nav.melosys.integrasjon.oppgave.konsument.OppgaveConsumerProducer
import no.nav.melosys.integrasjon.reststs.RestStsClient
import no.nav.melosys.integrasjon.reststs.StsRestTemplateProducer
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.test.web.client.MockRestServiceServer

@RestClientTest(
    value = [
        StsRestTemplateProducer::class,
        RestStsClient::class,
        WebClientAutoConfiguration::class,

        OppgaveConsumerImpl::class,
        OppgaveConsumerProducer::class,
        UserContextExchangeFilter::class,
        SystemContextExchangeFilter::class,
    ],
    properties = ["spring.profiles.active:itest-aareg"]
)
open class OppgaveConsumerTestBase(
    server: MockRestServiceServer,
    mockPort: Int
) : ConsumerTestBase(server, mockPort) {
    override fun createWireMock(): MappingBuilder {
        return WireMock.get("/api/v1/oppgaver/1")
    }

    override fun getMockData(): String {
        return """{
          "id": 11519,
          "tildeltEnhetsnr": "4530",
          "endretAvEnhetsnr": "4530",
          "journalpostId": "439654251",
          "behandlesAvApplikasjon": "FS38",
          "saksreferanse": "MEL-301",
          "aktoerId": "1332607802528",
          "tilordnetRessurs": "Z990757",
          "beskrivelse": " ",
          "tema": "MED",
          "behandlingstema": "ab0390",
          "oppgavetype": "BEH_SED",
          "versjon": 3,
          "opprettetAv": "srvmelosys",
          "endretAv": "Z990757",
          "prioritet": "NORM",
          "status": "AAPNET",
          "metadata": {},
          "fristFerdigstillelse": "2019-12-26",
          "aktivDato": "2019-10-03",
          "opprettetTidspunkt": "2019-10-03T12:27:26.566+02:00",
          "endretTidspunkt": "2019-10-03T18:24:55.697+02:00"
        }"""
    }
}
