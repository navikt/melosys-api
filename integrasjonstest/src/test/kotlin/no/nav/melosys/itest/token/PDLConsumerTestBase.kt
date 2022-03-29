package no.nav.melosys.itest.token

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.melosys.integrasjon.pdl.PDLAuthFilter
import no.nav.melosys.integrasjon.pdl.PDLAuthFilterProducer
import no.nav.melosys.integrasjon.pdl.PDLConsumerImpl
import no.nav.melosys.integrasjon.pdl.PDLConsumerProducer
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

        PDLConsumerImpl::class,
        PDLConsumerProducer::class,
        PDLAuthFilter::class,
        PDLAuthFilterProducer::class,
    ],
    properties = ["spring.profiles.active:itest-token"]
)
open class PDLConsumerTestBase(
    server: MockRestServiceServer,
    mockPort: Int
) : ConsumerTestBase(server, mockPort){

    override fun createWireMock(): MappingBuilder {
        return WireMock.post("/graphql")
    }

    override fun getMockData(): String {
        return """{
          "data": {
            "hentIdenter": {
              "identer": [
                {
                  "ident": "99026522600",
                  "gruppe": "FOLKEREGISTERIDENT"
                },
                {
                  "ident": "9834873315250",
                  "gruppe": "AKTORID"
                }
              ]
            }
          }
        }
        """
    }
}
