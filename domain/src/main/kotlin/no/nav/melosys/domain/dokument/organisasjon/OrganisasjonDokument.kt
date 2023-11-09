package no.nav.melosys.domain.dokument.organisasjon

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonView
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.dokument.DokumentView.FrontendApi
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.jaxb.LocalDateXmlAdapter
import java.time.LocalDate
import javax.xml.bind.annotation.*
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
class OrganisasjonDokument : SaksopplysningDokument {
    var orgnummer: String? = null

    @XmlElementWrapper(name = "navn")
    @XmlElement(name = "navnelinje")
    @JsonIgnore
    var navn: List<String>? = null

    @XmlJavaTypeAdapter(LocalDateXmlAdapter::class)
    var oppstartsdato: LocalDate? = null

    var enhetstype: String? = null //"http://nav.no/kodeverk/Kodeverk/EnhetstyperJuridiskEnhet"
    var organisasjonDetaljer: OrganisasjonsDetaljer? = null
    var sektorkode: String? = null //"http://nav.no/kodeverk/Kodeverk/Sektorkoder"

    @JsonProperty("navn")
    fun getSammenslåttNavn(): String? {
        return lagSammenslåttNavn()
    }

    @JsonView(FrontendApi::class)
    fun getForretningsadresse(): StrukturertAdresse? {
        return organisasjonDetaljer?.hentStrukturertForretningsadresse()
    }

    @JsonView(FrontendApi::class)
    fun getPostadresse(): StrukturertAdresse? {
        return organisasjonDetaljer?.hentStrukturertPostadresse()
    }

    // Hvis man ikke har bruk for historikk på navn så er det best å bruke navn på nivå organisasjon.
    fun lagSammenslåttNavn(): String? {
        return if (navn == null) "UKJENT" else java.lang.String.join(" ", navn)
    }

    fun harRegistrertPostadresse(): Boolean {
        return getPostadresse() != null && getPostadresse()!!.erGyldig()
    }

    fun harRegistrertForretningsadresse(): Boolean {
        return getForretningsadresse() != null && getForretningsadresse()!!.erGyldig()
    }

    fun hentTilgjengeligAdresse(): StrukturertAdresse? {
        return if (harRegistrertPostadresse()) getPostadresse() else getForretningsadresse()
    }

    fun harRegistrertAdresse(): Boolean {
        return harRegistrertPostadresse() || harRegistrertForretningsadresse()
    }
}
