package no.nav.melosys.melosysmock.tps

import no.nav.melosys.melosysmock.person.PersonRepo
import no.nav.melosys.melosysmock.utils.tilXmlGregorianCalendar
import no.nav.tjeneste.virksomhet.aktoer.v2.HentAktoerIdForIdent
import no.nav.tjeneste.virksomhet.aktoer.v2.HentAktoerIdForIdentResponse
import no.nav.tjeneste.virksomhet.aktoer.v2.HentIdentForAktoerId
import no.nav.tjeneste.virksomhet.aktoer.v2.HentIdentForAktoerIdResponse
import no.nav.tjeneste.virksomhet.person.v3.HentPerson
import no.nav.tjeneste.virksomhet.person.v3.HentPersonResponse
import no.nav.tjeneste.virksomhet.person.v3.HentPersonhistorikk
import no.nav.tjeneste.virksomhet.person.v3.HentPersonhistorikkResponse
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.feil.PersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
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
import java.time.LocalDate

@Endpoint
class TpsApi {

    @PayloadRoot(namespace = "http://nav.no/tjeneste/virksomhet/person/v3", localPart = "hentPerson")
    @ResponsePayload
    fun hentPerson(@RequestPayload hentPerson: HentPerson): HentPersonResponse {
        val ident = (hentPerson.request.aktoer as PersonIdent).ident.ident
        val person = PersonRepo.repo[ident] ?: throw HentPersonPersonIkkeFunnet("Fant ikke person", PersonIkkeFunnet())

        return HentPersonResponse()
            .withResponse(
                no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse()
                    .withPerson(
                        Bruker()
                            .withFoedselsdato(
                                Foedselsdato()
                                    .withFoedselsdato(
                                        tilXmlGregorianCalendar(person.foedselsdato)
                                    )
                            )
                            .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent(ident)))
                            .withPersonnavn(
                                Personnavn()
                                    .withFornavn(person.fornavn)
                                    .withEtternavn(person.etternavn)
                                    .withSammensattNavn("${person.fornavn} ${person.etternavn}")
                            )
                            .withStatsborgerskap(Statsborgerskap().withLand(Landkoder().withValue(person.statsborgerskap[0])))
                            .withKjoenn(Kjoenn().withKjoenn(Kjoennstyper().withValue(person.kjønn)))
                            .withPersonstatus(Personstatus().withPersonstatus(Personstatuser().withValue("BOSA"))) //BOSA = Bosatt
                            .withPostadresse(
                                Postadresse().withUstrukturertAdresse(
                                    UstrukturertAdresse()
                                        .withAdresselinje1("Adresselinje 1")
                                        .withAdresselinje2("Adresselinje 2")
                                        .withAdresselinje3("Adresselinje 3")
                                        .withAdresselinje4("Adresselinje 4")
                                        .withPostnr("0010")
                                        .withPoststed("Oslo")
                                        .withLandkode(Landkoder().withValue("NOR"))
                                )
                            )
                            .withBostedsadresse(
                                Bostedsadresse().withStrukturertAdresse(
                                    Gateadresse()
                                        .withGatenavn("Gatenavn")
                                        .withGatenummer(1)
                                        .withHusbokstav("a")
                                        .withLandkode(Landkoder().withValue("NOR"))
                                        .withPoststed(Postnummer().withValue("0010"))
                                )
                            )
                            .withGjeldendePostadressetype(Postadressetyper().withValue("BOSTEDSADRESSE"))
                    )
            )
    }

    @PayloadRoot(namespace = "http://nav.no/tjeneste/virksomhet/person/v3", localPart = "hentPersonhistorikk")
    @ResponsePayload
    fun hentPersonhistorikk(@RequestPayload hentPersonhistorikk: HentPersonhistorikk): HentPersonhistorikkResponse {
        val person = PersonRepo.repo[(hentPersonhistorikk.request.aktoer as PersonIdent).ident.ident]!!
        return HentPersonhistorikkResponse()
            .withResponse(
                no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonhistorikkResponse()
                    .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent(person.ident)))
                    .withStatsborgerskapListe(
                        StatsborgerskapPeriode()
                            .withPeriode(
                                Periode().withFom(
                                    tilXmlGregorianCalendar(person.foedselsdato)
                                )
                            )
                            .withStatsborgerskap(
                                Statsborgerskap().withLand(Landkoder().withValue(person.statsborgerskap[0]))
                            )
                    )
            )

    }
}

@Endpoint
class AktørregisterApi {

    @PayloadRoot(namespace = "http://nav.no/tjeneste/virksomhet/aktoer/v2", localPart = "hentAktoerIdForIdent")
    @ResponsePayload
    fun hentAktoerIdForIdent(@RequestPayload hentAktoerIdForIdent: HentAktoerIdForIdent): HentAktoerIdForIdentResponse {
        val person = PersonRepo.repo[hentAktoerIdForIdent.hentAktoerIdForIdentRequest.ident]!!
        val response = HentAktoerIdForIdentResponse()
        response.hentAktoerIdForIdentResponse =
            no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentResponse()
        response.hentAktoerIdForIdentResponse.aktoerId = person.aktørId
        return response
    }

    @PayloadRoot(namespace = "http://nav.no/tjeneste/virksomhet/aktoer/v2", localPart = "hentIdentForAktoerId")
    @ResponsePayload
    fun hentIdentForAktoerId(@RequestPayload hentIdentForAktoerId: HentIdentForAktoerId): HentIdentForAktoerIdResponse {
        val person = PersonRepo.aktørIdRepo[hentIdentForAktoerId.hentIdentForAktoerIdRequest.aktoerId]!!
        val response = HentIdentForAktoerIdResponse()
        response.hentIdentForAktoerIdResponse =
            no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdResponse()
        response.hentIdentForAktoerIdResponse.ident = person.ident
        return response
    }

}

@Configuration
class TpsApiConfiguration {

    @Bean(name = ["hentPerson"])
    fun hentPersonDefinition(hentPersonSchema: XsdSchema): DefaultWsdl11Definition {
        val wsdl11Definition = DefaultWsdl11Definition()
        wsdl11Definition.setPortTypeName("Person_v3")
        wsdl11Definition.setLocationUri("/soap/services/tps/v3")
        wsdl11Definition.setTargetNamespace("http://nav.no/tjeneste/virksomhet/person/v3")
        wsdl11Definition.setSchema(hentPersonSchema)
        return wsdl11Definition
    }

    @Bean(name = ["aktoerReg"])
    fun aktoerIdDefinition(aktørRegisterSchema: XsdSchema): DefaultWsdl11Definition {
        val wsdl11Definition = DefaultWsdl11Definition()
        wsdl11Definition.setPortTypeName("Aktoer_v2")
        wsdl11Definition.setLocationUri("/soap/services/aktoer/v2/Aktoer_v2")
        wsdl11Definition.setTargetNamespace("http://nav.no/tjeneste/virksomhet/aktoer/v2")
        wsdl11Definition.setSchema(aktørRegisterSchema)
        return wsdl11Definition
    }

    @Bean
    fun hentPersonSchema(): XsdSchema {
        return SimpleXsdSchema(ClassPathResource("wsdl/no/nav/tjeneste/virksomhet/person/v3/meldinger/meldinger.xsd"))
    }

    @Bean
    fun aktørRegisterSchema(): XsdSchema {
        return SimpleXsdSchema(ClassPathResource("wsdl/no/nav/tjeneste/virksomhet/aktoer/v2/meldinger/meldinger.xsd"))
    }
}
