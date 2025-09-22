package no.nav.melosys.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.*

@Embeddable
class TidligereMedlemsperiodeId(
    @Column(name = "behandling_id")
    var behandlingId: Long? = null,

    @Column(name = "periode_id")
    var periodeId: Long? = null
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TidligereMedlemsperiodeId) return false

        return Objects.equals(behandlingId, other.behandlingId) &&
            Objects.equals(periodeId, other.periodeId)
    }

    override fun hashCode(): Int = Objects.hash(behandlingId, periodeId)
}
