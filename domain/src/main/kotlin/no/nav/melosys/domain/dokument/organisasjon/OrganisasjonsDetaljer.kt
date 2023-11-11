package no.nav.melosys.domain.dokument.organisasjon

import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.adresse.UstrukturertAdresse
import no.nav.melosys.domain.dokument.jaxb.LocalDateXmlAdapter
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk.Epost
import no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk.Telefonnummer
import java.time.LocalDate
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

@XmlAccessorType(XmlAccessType.FIELD)
open class OrganisasjonsDetaljer { // Needs to be open because of mocking : TODO: rewrite tests to kotlin
    var orgnummer: String? = null

    @XmlElement(name = "organisasjonsnavn")
    var navn: List<Organisasjonsnavn?>? = ArrayList() //TODO: use emptyList when we remove JAXB code
    var forretningsadresse: List<GeografiskAdresse> = ArrayList()
    var postadresse: List<GeografiskAdresse> = ArrayList()
    var telefon: List<Telefonnummer?>? = ArrayList()
    var epostadresse: List<Epost?>? = ArrayList()
    var naering: List<String?>? = ArrayList() //"http://nav.no/kodeverk/Kodeverk/Næringskoder"

    @XmlJavaTypeAdapter(LocalDateXmlAdapter::class)
    var opphoersdato: LocalDate? = null


    fun hentStrukturertPostadresse(): StrukturertAdresse? {
        val adresse = hentFørsteGyldigePostadresse()
        return konverterTilStrukturertAdresse(adresse)
    }

    open fun hentStrukturertForretningsadresse(): StrukturertAdresse? { // Needs to be open because of mocking : TODO: rewrite tests to kotlin
        val adresse = hentFørsteGyldigeForretningsadresse()
        return konverterTilStrukturertAdresse(adresse)
    }

    fun hentUstrukturertForretningsadresse(): UstrukturertAdresse? {
        val adresse = hentFørsteGyldigeForretningsadresse()
        return konverterTilUstrukturertAdresse(adresse)
    }

    private fun hentFørsteGyldigeForretningsadresse(): GeografiskAdresse? = hentFørsteGyldigeAdresse(forretningsadresse)

    private fun hentFørsteGyldigePostadresse(): GeografiskAdresse? = hentFørsteGyldigeAdresse(postadresse)

    private fun hentFørsteGyldigeAdresse(adresser: List<GeografiskAdresse>): GeografiskAdresse? =
        adresser.firstOrNull { it.gyldighetsperiode?.erGyldig() == true }

    private fun konverterTilUstrukturertAdresse(adresse: GeografiskAdresse?): UstrukturertAdresse? {
        if (adresse == null) return null
        require(adresse is SemistrukturertAdresse) { "Adresse ikke støttet " + adresse.javaClass.getSimpleName() }
        return UstrukturertAdresse.av(adresse)
    }

    internal fun konverterTilStrukturertAdresse(adresse: GeografiskAdresse?): StrukturertAdresse? {
        if (adresse == null) return null
        require(adresse is SemistrukturertAdresse) { "Adresse ikke støttet ${adresse.javaClass.simpleName}" }

        return StrukturertAdresse().apply {
            gatenavn = listOfNotNull(adresse.adresselinje1, adresse.adresselinje2, adresse.adresselinje3)
                .joinToString(" ")
                .replace("\\s+".toRegex(), " ")

            landkode = adresse.landkode

            // Utenlandsk adresse kan ha postnummer som en del av poststed
            postnummer = adresse.postnr ?: if (adresse.erUtenlandsk()) " " else null

            poststed = if (adresse.erUtenlandsk()) {
                adresse.poststedUtland ?: adresse.poststed ?: ""
            } else {
                adresse.poststed ?: ""
            }
        }
    }
}
