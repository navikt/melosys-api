package no.nav.melosys.melosysmock.inntekt

import no.nav.tjeneste.virksomhet.inntekt.v3.HentInntektListeBolk
import no.nav.tjeneste.virksomhet.inntekt.v3.HentInntektListeBolkResponse
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Aktoer
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ArbeidsInntektIdent
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.PersonIdent
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
class InntektApi {

    @PayloadRoot(
        namespace = "http://nav.no/tjeneste/virksomhet/inntekt/v3",
        localPart = "hentInntektListeBolk"
    )
    @ResponsePayload
    fun hentInntektsliste(@RequestPayload hentInntektListeBolk: HentInntektListeBolk) : HentInntektListeBolkResponse {
        val res = HentInntektListeBolkResponse().apply {
            response = no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkResponse()
        }

        res.response.arbeidsInntektIdentListe.add(
            ArbeidsInntektIdent().apply {
                ident = PersonIdent().apply { personIdent = (hentInntektListeBolk.request.identListe[0] as PersonIdent).personIdent }
            }
        )
        res.response.sikkerhetsavvikListe
        return res
    }
}

@Configuration
class InntektConfig {
    @Bean(name = ["inntekt"])
    fun inntektDefinition(inntektSchema: XsdSchema): DefaultWsdl11Definition {
        val wsdl11Definition = DefaultWsdl11Definition()
        wsdl11Definition.setPortTypeName("Inntekt_v3")
        wsdl11Definition.setLocationUri("/soap/services/inntekt/v3")
        wsdl11Definition.setTargetNamespace("http://nav.no/tjeneste/virksomhet/inntekt/v3")
        wsdl11Definition.setSchema(inntektSchema)
        return wsdl11Definition
    }

    @Bean
    fun inntektSchema(): XsdSchema {
        return SimpleXsdSchema(ClassPathResource("wsdl/no/nav/tjeneste/virksomhet/inntekt/v3/meldinger/meldinger.xsd"))
    }
}
