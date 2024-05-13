package no.nav.melosys.domain.eessi.melding

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class AnmodningUnntak @JsonCreator constructor(
    @JsonProperty("unntakFraLovvalgsland")
    var unntakFraLovvalgsland: String?,
    @JsonProperty("unntakFraLovvalgsbestemmelse")
    var unntakFraLovvalgsbestemmelse: String?
)

