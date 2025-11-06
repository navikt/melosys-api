package no.nav.melosys.domain.avgift

import jakarta.persistence.Embeddable
import java.math.BigDecimal
import java.util.*

@Embeddable
class Penger(
    val verdi: BigDecimal?,

    val valuta: String = NOK
) {
    constructor() : this(BigDecimal.ZERO, NOK)

    constructor(verdi: Double) : this(BigDecimal.valueOf(verdi))

    fun hentVerdi() = verdi ?: error("Verdi er påkrevd for Penger")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Penger) return false
        if (valuta != other.valuta) return false

        // Capture values to local variables because all-open plugin makes properties open,
        // preventing smart casting. This allows us to null-check and use the values safely.
        val thisVerdi = this.verdi
        val otherVerdi = other.verdi

        if (thisVerdi == null && otherVerdi == null) return true
        if (thisVerdi == null || otherVerdi == null) return false
        // Both non-null - compare using BigDecimal.compareTo (handles scale differences)
        return thisVerdi.compareTo(otherVerdi) == 0
    }

    override fun hashCode(): Int = Objects.hash(
        verdi?.stripTrailingZeros(),
        valuta
    )

    override fun toString(): String = "Penger{$verdi $valuta}"

    companion object {
        private const val NOK = "NOK"
    }
}
