package no.nav.melosys.service.oppgave.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class TilbakeleggingDto (
    @JvmField
    var behandlingID: Long,

    @JsonProperty("venterPaaDokumentasjon")
    var isVenterPåDokumentasjon: Boolean
)
