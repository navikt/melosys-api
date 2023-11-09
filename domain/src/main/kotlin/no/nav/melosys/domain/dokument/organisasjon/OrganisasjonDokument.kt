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
    fun getSammenslåttNavn(): String = lagSammenslåttNavn()

    @JsonView(FrontendApi::class)
    fun getForretningsadresse(): StrukturertAdresse? = organisasjonDetaljer?.hentStrukturertForretningsadresse()

    @JsonView(FrontendApi::class)
    fun getPostadresse(): StrukturertAdresse? = organisasjonDetaljer?.hentStrukturertPostadresse()

    // Hvis man ikke har bruk for historikk på navn så er det best å bruke navn på nivå organisasjon.
    fun lagSammenslåttNavn(): String = navn?.joinToString(" ") ?: "UKJENT"

    fun harRegistrertPostadresse(): Boolean = getPostadresse()?.erGyldig() ?: false

    fun harRegistrertForretningsadresse(): Boolean = getForretningsadresse()?.erGyldig() ?: false

    fun hentTilgjengeligAdresse(): StrukturertAdresse? = if (harRegistrertPostadresse()) getPostadresse() else getForretningsadresse()

    fun harRegistrertAdresse(): Boolean = harRegistrertPostadresse() || harRegistrertForretningsadresse()
}
