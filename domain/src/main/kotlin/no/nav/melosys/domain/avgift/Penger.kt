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

        return when {
            verdi == null && other.verdi == null -> valuta == other.valuta
            verdi == null || other.verdi == null -> false
            else -> verdi.compareTo(other.verdi) == 0 && valuta == other.valuta
        }
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
