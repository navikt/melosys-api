package no.nav.melosys.service.hendelser

import com.fasterxml.jackson.annotation.JsonAnySetter
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
open class HendelseMelding


data class VedtakHendelseMelding(
    val folkeregisterIdent: String,
    val sakstype: Sakstyper,
    val sakstema: Sakstemaer
) : HendelseMelding()

class UkjentMelding : HendelseMelding() {
    val properties: MutableMap<String, Any> = mutableMapOf()

    @JsonAnySetter
    fun setAdditionalProperty(name: String, value: Any) {
        properties[name] = value
    }
}

