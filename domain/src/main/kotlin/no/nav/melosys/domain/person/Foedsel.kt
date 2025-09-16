package no.nav.melosys.domain.person

import java.time.LocalDate

data class Foedsel(
    val fødselsdato: LocalDate?,
    val fødselsår: Int?,
    val fødeland: String?,
    val fødested: String?
)