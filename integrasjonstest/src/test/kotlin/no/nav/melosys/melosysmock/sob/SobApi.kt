package no.nav.melosys.melosysmock.sob

import no.nav.tjeneste.virksomhet.sakogbehandling.v1.FinnSakOgBehandlingskjedeListe
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.FinnSakOgBehandlingskjedeListeResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.ws.server.endpoint.annotation.Endpoint
import org.springframework.ws.server.endpoint.annotation.PayloadRoot
import org.springframework.ws.server.endpoint.annotation.RequestPayload
import org.springframework.ws.server.endpoint.annotation.ResponsePayload
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition
import org.springframework.xml.xsd.SimpleXsdSchema
import org.springframework.xml.xsd.XsdSchema

@Endpoint
class SobApi {

    @PayloadRoot(
        namespace = "http://nav.no/tjeneste/virksomhet/sakOgBehandling/v1",
        localPart = "finnSakOgBehandlingskjedeListe"
    )
    @ResponsePayload
    fun finnSakOgBehandlingskjedeListeResponse(@RequestPayload finnSakOgBehandlingskjedeListe: FinnSakOgBehandlingskjedeListe): FinnSakOgBehandlingskjedeListeResponse {
        return FinnSakOgBehandlingskjedeListeResponse().apply {
            response = no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.FinnSakOgBehandlingskjedeListeResponse()
        }
    }
}

@Configuration
class SobConfig {
    @Bean(name = ["sob"])
    fun organisasjonDefinition(sobSchema: XsdSchema): DefaultWsdl11Definition {
        val wsdl11Definition = DefaultWsdl11Definition()
        wsdl11Definition.setPortTypeName("SakOgBehandling_v1")
        wsdl11Definition.setLocationUri("/soap/services/sakOgBehandling/v1")
        wsdl11Definition.setTargetNamespace("http://nav.no/tjeneste/virksomhet/sakOgBehandling/v1")
        wsdl11Definition.setSchema(sobSchema)
        return wsdl11Definition
    }

    @Bean
    fun sobSchema(): XsdSchema {
        return SimpleXsdSchema(ClassPathResource("wsdl/no/nav/tjeneste/virksomhet/sakOgBehandling/v1/meldinger/meldinger.xsd"))
    }
}
