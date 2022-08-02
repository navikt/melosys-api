package no.nav.melosys.integrasjon.medl.api.v1

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class MedlemskapsunntakForGet(
    var unntakId: Long? = null,
    var ident: String? = null,
    var fraOgMed: LocalDate? = null,
    var tilOgMed: LocalDate? = null,
    var status: String? = null,
    var statusaarsak: String? = null,
    var dekning: String? = null,
    var helsedel: Boolean? = null,
    var medlem: Boolean? = null,
    var lovvalgsland: String? = null,
    var lovvalg: String? = null,
    var grunnlag: String? = null,
    var sporingsinformasjon: Sporingsinformasjon? = null
)
