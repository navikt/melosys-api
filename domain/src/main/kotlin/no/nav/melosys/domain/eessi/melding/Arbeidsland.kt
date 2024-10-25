package no.nav.melosys.domain.eessi.melding

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

data class Arbeidsland (
    val land: String,
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val arbeidssted: List<Arbeidssted> = emptyList(),
)
