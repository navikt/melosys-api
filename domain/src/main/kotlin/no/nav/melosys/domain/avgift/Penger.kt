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
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Penger
        return verdi?.compareTo(that.verdi) == 0 && Objects.equals(valuta, that.valuta)
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
