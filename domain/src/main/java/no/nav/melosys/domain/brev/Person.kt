package no.nav.melosys.domain.brev

import java.time.LocalDate

@JvmRecord
data class Person(
    val navn: String?,
    val foedselsdato: LocalDate?,
    val fnr: String?,
    val dnr: String?
)
