package no.nav.melosys.integrasjon.medl.api.v1

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Sporingsinformasjon(
    var versjon: Int? = 0,
    var registrert: LocalDate? = null,
    var besluttet: LocalDate? = null,
    var kilde: String? = null,
    var kildedokument: String? = null,
    var opprettet: LocalDateTime? = null,
    var opprettetAv: String? = null,
    var sistEndret: LocalDateTime? = null,
    var sistEndretAv: String? = null,
)
