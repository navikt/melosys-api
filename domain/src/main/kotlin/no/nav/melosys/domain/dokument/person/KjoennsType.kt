package no.nav.melosys.domain.dokument.person

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty


data class KjoennsType @JsonCreator constructor(
    @JsonProperty("kode") val kode: String
)
