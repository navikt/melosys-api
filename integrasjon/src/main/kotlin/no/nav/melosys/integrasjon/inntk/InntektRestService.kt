package no.nav.melosys.integrasjon.inntk

import mu.KotlinLogging
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningKildesystem
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.dokument.DokumentFactory
import no.nav.melosys.domain.dokument.XmlFormaterer
import no.nav.melosys.exception.IntegrasjonException
import no.nav.melosys.exception.SikkerhetsbegrensningException
import no.nav.melosys.integrasjon.inntk.inntekt.InntektConsumer
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkUgyldigInput
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.*
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkRequest
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkResponse
import java.io.StringWriter
import java.time.YearMonth
import javax.xml.bind.JAXBException
import javax.xml.datatype.DatatypeConfigurationException
import javax.xml.datatype.DatatypeConstants
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar
import javax.xml.ws.soap.SOAPFaultException

private val log = KotlinLogging.logger { }
open class InntektRestService(
    private val inntektConsumer: InntektConsumer,
    private val dokumentFactory: DokumentFactory
) : InntektFasade {
    private val objectFactory: ObjectFactory = ObjectFactory()

    override fun hentInntektListe(personID: String, fom: YearMonth, tom: YearMonth): Saksopplysning {
        log.info("hentInntektListe: personID=$personID, fom=$fom, tom=$tom")
        val response = hentInntektListeBolkResponse(personID, fom, tom)
        val xmlWriter = StringWriter()
        try {
            val xmlRoot = no.nav.tjeneste.virksomhet.inntekt.v3.HentInntektListeBolkResponse()
            xmlRoot.response = response
            dokumentFactory.createMarshaller().marshal(xmlRoot, xmlWriter)
        } catch (e: JAXBException) {
            throw IntegrasjonException(e)
        }
        val saksopplysning = Saksopplysning()
        val dokumentXml = XmlFormaterer.formaterXml(xmlWriter.toString())
            ?: throw IntegrasjonException("DokumentXML er null!")
        saksopplysning.leggTilKildesystemOgMottattDokument(
            SaksopplysningKildesystem.INNTK, dokumentXml
        )
        saksopplysning.type = SaksopplysningType.INNTK
        saksopplysning.versjon = INNTEKT_VERSJON
        dokumentFactory.lagDokument(saksopplysning)
        return saksopplysning
    }

    private fun hentInntektListeBolkResponse(
        personID: String,
        fom: YearMonth,
        tom: YearMonth
    ): HentInntektListeBolkResponse {
        var fom = fom
        if (fom.isBefore(JANUAR_2015)) {
            if (tom.isBefore(JANUAR_2015)) {
                log.info(
                    "Hele perioden er fra før {} som inntektskomponenten ikke støtter. Lager en tom respons",
                    JANUAR_2015
                )
                return lagTomInntektListeBolkResponse()
            }
            log.info(
                "Periode har fom dato {} som inntektskomponent ikke støtter, henter inntekt med fom {}",
                fom,
                JANUAR_2015
            )
            fom = JANUAR_2015
        }
        val request = HentInntektListeBolkRequest()
        val personIdent = objectFactory.createPersonIdent()
        personIdent.personIdent = personID
        request.identListe.add(personIdent)
        request.uttrekksperiode = lagUttrekksperiode(fom, tom)
        request.ainntektsfilter = lagAinntektsfilter()
        request.formaal = lagFormaal()
        return try {
            inntektConsumer.hentInntektListeBolk(request)
        } catch (e: HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter) {
            throw SikkerhetsbegrensningException(e)
        } catch (e: HentInntektListeBolkUgyldigInput) {
            throw IntegrasjonException(e)
        } catch (e: SOAPFaultException) {
            throw IntegrasjonException(e)
        }
    }

    private fun lagUttrekksperiode(fom: YearMonth, tom: YearMonth): Uttrekksperiode {
        val uttrekksperiode = objectFactory.createUttrekksperiode()
        try {
            uttrekksperiode.maanedFom = convertToXMLGregorianCalendar(fom)
            uttrekksperiode.maanedTom = convertToXMLGregorianCalendar(tom)
        } catch (e: DatatypeConfigurationException) {
            throw IllegalStateException(e)
        }
        return uttrekksperiode
    }

    private fun lagAinntektsfilter(): Ainntektsfilter {
        val ainntektsfilter = objectFactory.createAinntektsfilter()
        ainntektsfilter.value = FILTER
        ainntektsfilter.kodeRef = FILTER
        ainntektsfilter.kodeverksRef = FILTER_URI
        return ainntektsfilter
    }

    private fun lagFormaal(): Formaal {
        val formaal = objectFactory.createFormaal()
        formaal.value = FORMAALSKODE
        formaal.kodeRef = FORMAALSKODE
        formaal.kodeverksRef = FORMAALSKODE_URI
        return formaal
    }

    private fun lagTomInntektListeBolkResponse(): HentInntektListeBolkResponse {
        val hentInntektListeBolkResponse = HentInntektListeBolkResponse()
        hentInntektListeBolkResponse.arbeidsInntektIdentListe.add(ArbeidsInntektIdent())
        return hentInntektListeBolkResponse
    }

    companion object {
        private const val INNTEKT_VERSJON = "3.2"
        const val FILTER = "MedlemskapA-inntekt"
        const val FILTER_URI = "http://nav.no/kodeverk/Kode/A-inntektsfilter/MedlemskapA-inntekt?v=6"
        const val FORMAALSKODE = "Medlemskap"
        const val FORMAALSKODE_URI = "http://nav.no/kodeverk/Kode/Formaal/Medlemskap?v=5"
        private val JANUAR_2015 = YearMonth.of(2015, 1)

        @Throws(DatatypeConfigurationException::class)
        private fun convertToXMLGregorianCalendar(yearMonth: YearMonth?): XMLGregorianCalendar? {
            return if (yearMonth == null) {
                null
            } else DatatypeFactory.newInstance().newXMLGregorianCalendar(
                yearMonth.year,
                yearMonth.monthValue,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED
            )
        }
    }
}
