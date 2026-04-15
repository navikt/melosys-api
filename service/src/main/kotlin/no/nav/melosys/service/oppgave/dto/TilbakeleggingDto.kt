package no.nav.melosys.service.oppgave.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

data class TilbakeleggingDto (
    val behandlingID: Long,

    @JsonProperty("venterPaaDokumentasjon")
    @JsonSetter(nulls = Nulls.SKIP)
    val isVenterPåDokumentasjon: Boolean = false
)
