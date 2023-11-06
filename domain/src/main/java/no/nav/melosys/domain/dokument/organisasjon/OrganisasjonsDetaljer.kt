package no.nav.melosys.domain.dokument.organisasjon

import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.adresse.UstrukturertAdresse
import no.nav.melosys.domain.dokument.jaxb.LocalDateXmlAdapter
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk.Epost
import no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk.Telefonnummer
import org.springframework.util.StringUtils
import java.time.LocalDate
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

@XmlAccessorType(XmlAccessType.FIELD)
class OrganisasjonsDetaljer {
    var orgnummer: String? = null

    @XmlElement(name = "organisasjonsnavn")
    var navn: MutableList<Organisasjonsnavn?>? = ArrayList()
    @JvmField
    var forretningsadresse: MutableList<GeografiskAdresse> = ArrayList()
    @JvmField
    var postadresse: MutableList<GeografiskAdresse> = ArrayList()
    var telefon: MutableList<Telefonnummer?>? = ArrayList()
    var epostadresse: MutableList<Epost?>? = ArrayList()
    var naering: MutableList<String?>? = ArrayList() //"http://nav.no/kodeverk/Kodeverk/Næringskoder"

    @JvmField
    @XmlJavaTypeAdapter(LocalDateXmlAdapter::class)
    var opphoersdato: LocalDate? = null


    fun hentStrukturertPostadresse(): StrukturertAdresse? {
        val adresse = hentFørsteGyldigePostadresse()
        return konverterTilStrukturertAdresse(adresse)
    }

    fun hentStrukturertForretningsadresse(): StrukturertAdresse? {
        val adresse = hentFørsteGyldigeForretningsadresse()
        return konverterTilStrukturertAdresse(adresse)
    }

    fun hentUstrukturertForretningsadresse(): UstrukturertAdresse? {
        val adresse = hentFørsteGyldigeForretningsadresse()
        return konverterTilUstrukturertAdresse(adresse)
    }

    private fun hentFørsteGyldigeForretningsadresse(): GeografiskAdresse? {
        return hentFørsteGyldigeAdresse(forretningsadresse)
    }

    private fun hentFørsteGyldigePostadresse(): GeografiskAdresse? {
        return hentFørsteGyldigeAdresse(postadresse)
    }

    private fun hentFørsteGyldigeAdresse(adresser: MutableList<GeografiskAdresse>): GeografiskAdresse? {
        for (adresse in adresser) {
            val gyldighetsperiode = adresse.gyldighetsperiode
            if (gyldighetsperiode!!.erGyldig()) {
                return adresse
            }
        }
        return null
    }

    private fun konverterTilUstrukturertAdresse(adresse: GeografiskAdresse?): UstrukturertAdresse? {
        if (adresse == null) {
            return null
        }
        val ustrukturertAdresse: UstrukturertAdresse?
        ustrukturertAdresse = if (adresse is SemistrukturertAdresse) {
            val sAdresse = adresse as SemistrukturertAdresse?
            UstrukturertAdresse.av(sAdresse)
        } else {
            // Enhetsregistret har bare SemistrukturertAdresser
            throw IllegalArgumentException("Adresse ikke støttet " + adresse.javaClass.getSimpleName())
        }
        return ustrukturertAdresse
    }

    private fun konverterTilStrukturertAdresse(adresse: GeografiskAdresse?): StrukturertAdresse? {
        if (adresse == null) {
            return null
        }
        val strukturertAdresse = StrukturertAdresse()
        if (adresse is SemistrukturertAdresse) {
            val sAdresse = adresse as SemistrukturertAdresse?
            val stringBuilder = StringBuilder()
            if (sAdresse!!.adresselinje1 != null) {
                stringBuilder.append(sAdresse.adresselinje1)
            }
            if (sAdresse.adresselinje2 != null) {
                stringBuilder.append(" ")
                stringBuilder.append(adresse.adresselinje2)
            }
            if (sAdresse.adresselinje3 != null) {
                stringBuilder.append(" ")
                stringBuilder.append(sAdresse.adresselinje3)
            }
            val adresseLinje = stringBuilder.toString()
            strukturertAdresse.gatenavn = adresseLinje.replace("\\s+".toRegex(), " ")
            strukturertAdresse.landkode = sAdresse.landkode
            strukturertAdresse.postnummer = sAdresse.postnr
            if (sAdresse.erUtenlandsk()) {
                strukturertAdresse.poststed =
                    if (StringUtils.isEmpty(sAdresse.poststedUtland)) sAdresse.poststed else sAdresse.poststedUtland
                // Utenlandsk adresse kan ha postnummer som en del av poststed
                if (strukturertAdresse.postnummer == null) {
                    strukturertAdresse.postnummer = " "
                }
            } else {
                strukturertAdresse.poststed = if (sAdresse.poststed == null) "" else sAdresse.poststed
            }
        } else {
            // Enhetsregistret har bare SemistrukturertAdresser
            throw IllegalArgumentException("GeografiskAdresse ikke støttet " + adresse.javaClass.getSimpleName())
        }
        return strukturertAdresse
    }
}
