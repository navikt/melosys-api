package no.nav.melosys.domain.avgift

import jakarta.persistence.*
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.Medlemskapsperiode
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "trygdeavgiftsperiode")
class Trygdeavgiftsperiode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "periode_fra", nullable = false)
    val periodeFra: LocalDate,

    @Column(name = "periode_til", nullable = false)
    val periodeTil: LocalDate,

    @Embedded
    val trygdeavgiftsbeløpMd: Penger,

    @Column(name = "trygdesats", nullable = false)
    val trygdesats: BigDecimal,

    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "inntektsperiode_id")
    val grunnlagInntekstperiode: Inntektsperiode? = null,

    @ManyToOne
    @JoinColumn(name = "medlemskapsperiode_id")
    val grunnlagMedlemskapsperiode: Medlemskapsperiode? = null,

    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "skatteforhold_id")
    val grunnlagSkatteforholdTilNorge: SkatteforholdTilNorge? = null
) : ErPeriode {

    fun harAvgift(): Boolean {
        return BigDecimal.ZERO.compareTo(trygdesats) != 0 &&
            BigDecimal.ZERO.compareTo(trygdeavgiftsbeløpMd.verdi) != 0
    }

    override fun getFom(): LocalDate = periodeFra

    override fun getTom(): LocalDate = periodeTil

    fun copyEntity(
        id: Long? = this.id,
        periodeFra: LocalDate = this.periodeFra,
        periodeTil: LocalDate = this.periodeTil,
        trygdeavgiftsbeløpMd: Penger = this.trygdeavgiftsbeløpMd,
        trygdesats: BigDecimal = this.trygdesats,
        grunnlagInntekstperiode: Inntektsperiode? = this.grunnlagInntekstperiode,
        grunnlagMedlemskapsperiode: Medlemskapsperiode? = this.grunnlagMedlemskapsperiode,
        grunnlagSkatteforholdTilNorge: SkatteforholdTilNorge? = this.grunnlagSkatteforholdTilNorge
    ): Trygdeavgiftsperiode {
        return Trygdeavgiftsperiode(
            id = id,
            periodeFra = periodeFra,
            periodeTil = periodeTil,
            trygdeavgiftsbeløpMd = trygdeavgiftsbeløpMd,
            trygdesats = trygdesats,
            grunnlagInntekstperiode = grunnlagInntekstperiode,
            grunnlagMedlemskapsperiode = grunnlagMedlemskapsperiode,
            grunnlagSkatteforholdTilNorge = grunnlagSkatteforholdTilNorge
        )
    }

    override fun toString(): String {
        return "Trygdeavgiftsperiode(id=$id, periodeFra=$periodeFra, periodeTil=$periodeTil, " +
            "trygdeavgiftsbeløpMd=$trygdeavgiftsbeløpMd, trygdesats=$trygdesats)"
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as Trygdeavgiftsperiode
        return Objects.equals(periodeFra, that.periodeFra) && Objects.equals(periodeTil, that.periodeTil) && Objects.equals(
            trygdeavgiftsbeløpMd,
            that.trygdeavgiftsbeløpMd
        ) && Objects.equals(trygdesats, that.trygdesats) && Objects.equals(grunnlagInntekstperiode, that.grunnlagInntekstperiode) && Objects.equals(
            grunnlagMedlemskapsperiode,
            that.grunnlagMedlemskapsperiode
        ) && Objects.equals(grunnlagSkatteforholdTilNorge, that.grunnlagSkatteforholdTilNorge)
    }

    override fun hashCode(): Int {
        return Objects.hash(
            periodeFra,
            periodeTil,
            trygdeavgiftsbeløpMd,
            trygdesats,
            grunnlagInntekstperiode,
            grunnlagMedlemskapsperiode,
            grunnlagSkatteforholdTilNorge
        )
    }
}
