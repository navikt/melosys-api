package no.nav.melosys.melosysmock.utbetal

import no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjon
import no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonResponse
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
class UtbetalApi {
    @PayloadRoot(
        namespace = "http://nav.no/tjeneste/virksomhet/utbetaling/v1",
        localPart = "hentUtbetalingsinformasjon"
    )
    @ResponsePayload
    fun hentUtbetalingsinformasjon(@RequestPayload hentUtbetalingsinformasjon: HentUtbetalingsinformasjon) : HentUtbetalingsinformasjonResponse {
        return HentUtbetalingsinformasjonResponse().apply {
            this.hentUtbetalingsinformasjonResponse = no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.WSHentUtbetalingsinformasjonResponse()
        }
    }
}

@Configuration
class UtbetalConfig {
    @Bean(name = ["utbetal"])
    fun inntektDefinition(utbetalSchema: XsdSchema): DefaultWsdl11Definition {
        val wsdl11Definition = DefaultWsdl11Definition()
        wsdl11Definition.setPortTypeName("Utbetaling_v1")
        wsdl11Definition.setLocationUri("/soap/services/utbetaling/v1")
        wsdl11Definition.setTargetNamespace("http://nav.no/tjeneste/virksomhet/utbetaling/v1")
        wsdl11Definition.setSchema(utbetalSchema)
        return wsdl11Definition
    }

    @Bean
    fun utbetalSchema(): XsdSchema {
        return SimpleXsdSchema(ClassPathResource("wsdl/utbetaling/no/nav/tjeneste/virksomhet/utbetaling/v1/meldinger/meldinger.xsd"))
    }
}
