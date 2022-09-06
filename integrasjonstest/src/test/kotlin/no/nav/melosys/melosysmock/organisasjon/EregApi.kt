package no.nav.melosys.melosysmock.organisasjon

import no.nav.melosys.melosysmock.utils.tilXmlGregorianCalendar
import no.nav.tjeneste.virksomhet.organisasjon.v4.HentOrganisasjon
import no.nav.tjeneste.virksomhet.organisasjon.v4.HentOrganisasjonResponse
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.*
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.SemistrukturertAdresse
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
import java.time.LocalDateTime

@Endpoint
class EregApi {

    @PayloadRoot(
        namespace = "http://nav.no/tjeneste/virksomhet/organisasjon/v4",
        localPart = "hentOrganisasjon"
    )
    @ResponsePayload
    fun hentOrganisasjon(@RequestPayload hentOrganisasjon: HentOrganisasjon): HentOrganisasjonResponse {
        val organisasjonModell = OrganisasjonRepo.repo[hentOrganisasjon.request.orgnummer]
            ?: throw NoSuchElementException("Ingen organisasjon med orgnr ${hentOrganisasjon.request.orgnummer}")

        val res = HentOrganisasjonResponse().apply {
            response = no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonResponse().apply {
                organisasjon = JuridiskEnhet()
                    .apply {
                        navn = UstrukturertNavn().apply { navnelinje.add(organisasjonModell.navn) }
                        orgnummer = organisasjonModell.orgnr
                        juridiskEnhetDetaljer = JuridiskEnhetDetaljer().apply {
                                this.sektorkode = Sektorkoder().apply { kodeRef = "2100" }
                                this.enhetstype = EnhetstyperJuridiskEnhet().apply { kodeRef = "AS" }
                        }
                        organisasjonDetaljer = OrganisasjonsDetaljer().apply {
                            registreringsDato = tilXmlGregorianCalendar(LocalDate.now().minusYears(1))
                            datoSistEndret = tilXmlGregorianCalendar(LocalDate.now().minusYears(1))
                            orgnummer = organisasjonModell.orgnr
                            forretningsadresse.add(tilSemistrukturertAdresse(organisasjonModell.forretningsadresse))
                            postadresse.add(tilSemistrukturertAdresse(organisasjonModell.postadresse))
                            navn.add(Organisasjonsnavn().apply {
                                navn = UstrukturertNavn().apply { navnelinje.add(organisasjonModell.navn) }
                                redigertNavn = organisasjonModell.navn
                                fomBruksperiode = tilXmlGregorianCalendar(LocalDate.now().minusYears(1))
                                fomGyldighetsperiode = tilXmlGregorianCalendar(LocalDate.now().minusYears(1))
                            })
                            telefon.add(Telefonnummer().apply {
                                fomGyldighetsperiode = tilXmlGregorianCalendar(LocalDate.now().minusYears(1))
                                fomBruksperiode = tilXmlGregorianCalendar(LocalDate.now().minusYears(1))
                                identifikator = "+47 12 34 56 78"
                                type = Telefontyper().apply { this.value = "ARBT" }
                            })
                            navSpesifikkInformasjon = NAVSpesifikkInformasjon().apply { isErIA = false }
                            naering.add(Naering().apply {
                                fomBruksperiode = tilXmlGregorianCalendar(LocalDate.now().minusYears(1))
                                fomGyldighetsperiode = tilXmlGregorianCalendar(LocalDate.now().minusYears(1))
                                naeringskode = Naeringskoder().apply { kodeRef = "81.210" }
                            })
                        }
                    }
            }
        }

        return res

    }

    fun tilSemistrukturertAdresse(intern: no.nav.melosys.melosysmock.organisasjon.SemistrukturertAdresse): SemistrukturertAdresse {
        return SemistrukturertAdresse().apply {
            adresseId = "adresseID"
            fomGyldighetsperiode =
                tilXmlGregorianCalendar(LocalDateTime.now().minusYears(1))
            landkode = Landkoder().apply { kodeRef = intern.landkode }
            adresseledd.addAll(listOf(
                NoekkelVerdiAdresse().apply {
                    verdi = intern.adresselinje1
                    noekkel = NoeklerAdresseleddSemistrukturerteAdresser().apply { this.kodeRef = "adresselinje1" }
                },
                NoekkelVerdiAdresse().apply {
                    verdi = intern.postnummer
                    noekkel = NoeklerAdresseleddSemistrukturerteAdresser().apply { this.kodeRef = "postnr" }
                },
                NoekkelVerdiAdresse().apply {
                    verdi = intern.poststed
                    noekkel = NoeklerAdresseleddSemistrukturerteAdresser().apply { this.kodeRef = "poststed" }
                },
                NoekkelVerdiAdresse().apply {
                    verdi = intern.kommunenr
                    noekkel = NoeklerAdresseleddSemistrukturerteAdresser().apply { this.kodeRef = "kommunenr" }
                }
            ))
        }
    }
}

@Configuration
class EregConfig {

    @Bean(name = ["ereg"])
    fun organisasjonDefinition(organisasjonSchema: XsdSchema): DefaultWsdl11Definition {
        val wsdl11Definition = DefaultWsdl11Definition()
        wsdl11Definition.setPortTypeName("Organisasjon_v4")
        wsdl11Definition.setLocationUri("/soap/services/organisasjon/v4")
        wsdl11Definition.setTargetNamespace("http://nav.no/tjeneste/virksomhet/organisasjon/v4")
        wsdl11Definition.setSchema(organisasjonSchema)
        return wsdl11Definition
    }

    @Bean
    fun organisasjonSchema(): XsdSchema {
        return SimpleXsdSchema(ClassPathResource("wsdl/no/nav/tjeneste/virksomhet/organisasjon/v4/meldinger/meldinger.xsd"))
    }
}
