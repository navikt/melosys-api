package no.nav.melosys.domain.dokument.organisasjon

import com.fasterxml.jackson.annotation.JsonView
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.dokument.DokumentView.FrontendApi
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import java.time.LocalDate

class OrganisasjonDokument : SaksopplysningDokument {
    var orgnummer: String? = null
    var navn: String? = null
    var oppstartsdato: LocalDate? = null
    var enhetstype: String? = null
    var organisasjonDetaljer: OrganisasjonsDetaljer? = null
    var sektorkode: String? = null

    @JsonView(FrontendApi::class)
    fun getForretningsadresse(): StrukturertAdresse? = organisasjonDetaljer?.hentStrukturertForretningsadresse()

    @JsonView(FrontendApi::class)
    fun getPostadresse(): StrukturertAdresse? = organisasjonDetaljer?.hentStrukturertPostadresse()

    // Hvis man ikke har bruk for historikk på navn så er det best å bruke navn på nivå organisasjon.
    fun harRegistrertPostadresse(): Boolean = getPostadresse()?.erGyldig() ?: false

    fun harRegistrertForretningsadresse(): Boolean = getForretningsadresse()?.erGyldig() ?: false

    fun hentTilgjengeligAdresse(): StrukturertAdresse? = if (harRegistrertPostadresse()) getPostadresse() else getForretningsadresse()

    fun harRegistrertAdresse(): Boolean = harRegistrertPostadresse() || harRegistrertForretningsadresse()
}
