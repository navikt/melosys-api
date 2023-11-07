package no.nav.melosys.domain.dokument.organisasjon

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonView
import no.nav.melosys.domain.AbstraktOrganisasjon
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.dokument.DokumentView.FrontendApi
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import java.time.LocalDate
import javax.xml.bind.annotation.*

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
class OrganisasjonDokument : AbstraktOrganisasjon(), SaksopplysningDokument {
    @XmlElementWrapper(name = "navn")
    @XmlElement(name = "navnelinje")
    var navn: List<String>? = null
    @JvmField
    var organisasjonDetaljer: OrganisasjonsDetaljer? = null
    @JvmField
    var sektorkode: String? = null //"http://nav.no/kodeverk/Kodeverk/Sektorkoder"
    var orgnummer: String?
        get() = super.orgnummer
        set(orgnummer) {
            this.orgnummer = orgnummer
        }

    override fun getNavn(): String {
        return lagSammenslåttNavn()
    }

    @JsonView(FrontendApi::class)
    override fun getForretningsadresse(): StrukturertAdresse {
        return if (organisasjonDetaljer == null) null else organisasjonDetaljer!!.hentStrukturertForretningsadresse()
    }

    @JsonView(FrontendApi::class)
    override fun getPostadresse(): StrukturertAdresse {
        return if (organisasjonDetaljer == null) null else organisasjonDetaljer!!.hentStrukturertPostadresse()
    }

    // Hvis man ikke har bruk for historikk på navn så er det best å bruke navn på nivå organisasjon.
    fun lagSammenslåttNavn(): String {
        return if (navn == null) "UKJENT" else java.lang.String.join(" ", navn)
    }

    fun setNavn(navn: List<String>?) {
        this.navn = navn
    }

    // Brukes til å deserialisere objektet fra databasen
    @JsonProperty("navn")
    fun setNavn(navn: String) {
        this.navn = ArrayList(java.util.List.of(navn))
    }

    var oppstartsdato: LocalDate?
        get() = super.oppstartsdato
        set(oppstartsdato) {
            this.oppstartsdato = oppstartsdato
        }
    var enhetstype: String?
        get() = super.enhetstype
        set(enhetstype) {
            this.enhetstype = enhetstype
        }

    fun harRegistrertPostadresse(): Boolean {
        return postadresse != null && postadresse.erGyldig()
    }

    fun harRegistrertForretningsadresse(): Boolean {
        return forretningsadresse != null && forretningsadresse.erGyldig()
    }

    fun hentTilgjengeligAdresse(): StrukturertAdresse {
        return if (harRegistrertPostadresse()) postadresse else forretningsadresse
    }

    fun harRegistrertAdresse(): Boolean {
        return harRegistrertPostadresse() || harRegistrertForretningsadresse()
    }
}
