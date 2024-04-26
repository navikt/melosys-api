package no.nav.melosys.integrasjon.hendelser

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper

data class MelosysHendelse(
    val melding: HendelseMelding
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = UkjentMelding::class)
@JsonSubTypes(
    JsonSubTypes.Type(value = HendelseMelding::class, name = "HendelseMelding"),
    JsonSubTypes.Type(value = VedtakHendelseMelding::class, name = "VedtakHendelseMelding")
)
@JsonIgnoreProperties(ignoreUnknown = true)
open class HendelseMelding

data class VedtakHendelseMelding(
    val folkeregisterIdent: String,
    val sakstype: Sakstyper,
    val sakstema: Sakstemaer
) : HendelseMelding()

data class UkjentMelding(
    val properties: MutableMap<String, Any> = mutableMapOf()
) : HendelseMelding() {

    @JsonAnySetter
    fun setAdditionalProperty(name: String, value: Any) {
        properties[name] = value
    }
}
