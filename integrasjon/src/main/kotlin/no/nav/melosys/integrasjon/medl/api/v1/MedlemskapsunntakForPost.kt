package no.nav.melosys.integrasjon.medl.api.v1

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
class MedlemskapsunntakForPost {
    var ident: String? = null
    var fraOgMed: LocalDate? = null
    var tilOgMed: LocalDate? = null
    var status: String? = null
    var statusaarsak: String? = null
    var dekning: String? = null
    var lovvalgsland: String? = null
    var lovvalg: String? = null
    var grunnlag: String? = null
    var sporingsinformasjon: SporingsinformasjonForPost? = null

    @JsonIgnoreProperties(ignoreUnknown = true)
    class SporingsinformasjonForPost {
        var kildedokument: String? = null
    }
}
