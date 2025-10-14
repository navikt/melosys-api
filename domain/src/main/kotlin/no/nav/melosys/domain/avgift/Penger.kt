package no.nav.melosys.domain.avgift

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.math.BigDecimal
import java.util.*

@Embeddable
class Penger(
    @Column(name = "trygdeavgift_beloep_mnd_verdi")
    val verdi: BigDecimal,

    @Column(name = "trygdeavgift_beloep_mnd_valuta")
    val valuta: String = NOK
) {
    constructor() : this(BigDecimal.ZERO, NOK)

    constructor(verdi: Double) : this(BigDecimal.valueOf(verdi))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Penger
        return verdi.compareTo(that.verdi) == 0 && Objects.equals(valuta, that.valuta)
    }

    override fun hashCode(): Int = Objects.hash(
        // https://jira.adeo.no/browse/FAGSYSTEM-401031
        // Det oppstår tilfeller der verdi er null, til tross for at den har en ikke-nullable type.
        // Håndteres midlertidig med en safe call her.
        verdi?.stripTrailingZeros(),
        valuta
    )

    override fun toString(): String = "Penger{$verdi $valuta}"

    companion object {
        private const val NOK = "NOK"
    }
}
