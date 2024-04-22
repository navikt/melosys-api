package no.nav.melosys.service.oppgave.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class TilbakeleggingDto (
    val behandlingID: Long,

    @JsonProperty("venterPaaDokumentasjon")
    val isVenterPåDokumentasjon: Boolean
)
