package no.nav.melosys.domain.dokument.organisasjon

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonView
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.dokument.DokumentView.FrontendApi
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import java.time.LocalDate

class OrganisasjonDokument : SaksopplysningDokument {
    var orgnummer: String? = null

    var navn: List<String>? = null // TODO: Dette kan være en string. Fiks når vi fjerner soap/jaxb integrasjon

    var oppstartsdato: LocalDate? = null

    var enhetstype: String? = null //"http://nav.no/kodeverk/Kodeverk/EnhetstyperJuridiskEnhet"
    var organisasjonDetaljer: OrganisasjonsDetaljer? = null
    var sektorkode: String? = null //"http://nav.no/kodeverk/Kodeverk/Sektorkoder"

    @JsonProperty("navn")
    fun getSammenslåttNavn(): String = lagSammenslåttNavn()

    @JsonProperty("navn")
    fun setNavn(navn :String) {
        this.navn = listOf(navn)
    }

    @JsonView(FrontendApi::class)
    fun getForretningsadresse(): StrukturertAdresse? = organisasjonDetaljer?.hentStrukturertForretningsadresse()

    @JsonView(FrontendApi::class)
    fun getPostadresse(): StrukturertAdresse? = organisasjonDetaljer?.hentStrukturertPostadresse()

    // Hvis man ikke har bruk for historikk på navn så er det best å bruke navn på nivå organisasjon.
     internal fun lagSammenslåttNavn(): String = navn?.joinToString(" ") ?: "UKJENT"

    fun harRegistrertPostadresse(): Boolean = getPostadresse()?.erGyldig() ?: false

    fun harRegistrertForretningsadresse(): Boolean = getForretningsadresse()?.erGyldig() ?: false

    fun hentTilgjengeligAdresse(): StrukturertAdresse? = if (harRegistrertPostadresse()) getPostadresse() else getForretningsadresse()

    fun harRegistrertAdresse(): Boolean = harRegistrertPostadresse() || harRegistrertForretningsadresse()
}
