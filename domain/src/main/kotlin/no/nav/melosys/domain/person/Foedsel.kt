package no.nav.melosys.domain.person

import java.time.LocalDate

@JvmRecord
data class Foedsel(
    @get:JvmName("getFødselsdato") val fødselsdato: LocalDate?,
    @get:JvmName("getFødselsår") val fødselsår: Int?,
    @get:JvmName("getFødeland") val fødeland: String?,
    @get:JvmName("getFødested") val fødested: String?
)