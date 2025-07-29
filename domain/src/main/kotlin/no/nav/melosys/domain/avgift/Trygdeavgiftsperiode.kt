package no.nav.melosys.domain.avgift

import jakarta.persistence.*
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import java.math.BigDecimal
import java.time.LocalDate

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
    var grunnlagMedlemskapsperiode: Medlemskapsperiode? = null,

    @ManyToOne
    @JoinColumn(name = "helseutgift_dekkes_periode_id")
    var grunnlagHelseutgiftDekkesPeriode: HelseutgiftDekkesPeriode? = null,

    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "skatteforhold_id")
    val grunnlagSkatteforholdTilNorge: SkatteforholdTilNorge? = null
) : ErPeriode {

    val grunnlagMedlemskapsperiodeNotNull: Medlemskapsperiode
        get() = grunnlagMedlemskapsperiode ?: throw IllegalStateException("grunnlagMedlemskapsperiode er null")

    fun harAvgift(): Boolean =
        BigDecimal.ZERO.compareTo(trygdesats) != 0 && BigDecimal.ZERO.compareTo(trygdeavgiftsbeløpMd.verdi) != 0

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
        grunnlagHelseutgiftDekkesPeriode: HelseutgiftDekkesPeriode? = this.grunnlagHelseutgiftDekkesPeriode,
        grunnlagSkatteforholdTilNorge: SkatteforholdTilNorge? = this.grunnlagSkatteforholdTilNorge
    ) = Trygdeavgiftsperiode(
        id = id,
        periodeFra = periodeFra,
        periodeTil = periodeTil,
        trygdeavgiftsbeløpMd = trygdeavgiftsbeløpMd,
        trygdesats = trygdesats,
        grunnlagInntekstperiode = grunnlagInntekstperiode,
        grunnlagMedlemskapsperiode = grunnlagMedlemskapsperiode,
        grunnlagHelseutgiftDekkesPeriode = grunnlagHelseutgiftDekkesPeriode,
        grunnlagSkatteforholdTilNorge = grunnlagSkatteforholdTilNorge
    )

    override fun toString(): String {
        return "Trygdeavgiftsperiode(id=$id, periodeFra=$periodeFra, periodeTil=$periodeTil, " +
            "trygdeavgiftsbeløpMd=$trygdeavgiftsbeløpMd, trygdesats=$trygdesats)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Trygdeavgiftsperiode) return false

        return periodeFra == other.periodeFra &&
            periodeTil == other.periodeTil &&
            trygdeavgiftsbeløpMd == other.trygdeavgiftsbeløpMd &&
            trygdesats.compareTo(other.trygdesats) == 0 &&
            grunnlagInntekstperiode == other.grunnlagInntekstperiode &&
            grunnlagMedlemskapsperiode == other.grunnlagMedlemskapsperiode &&
            grunnlagSkatteforholdTilNorge == other.grunnlagSkatteforholdTilNorge
    }

    override fun hashCode(): Int {
        return listOf(
            periodeFra,
            periodeTil,
            trygdeavgiftsbeløpMd,
            trygdesats.stripTrailingZeros(),
            grunnlagInntekstperiode,
            grunnlagMedlemskapsperiode,
            grunnlagSkatteforholdTilNorge
        ).hashCode()
    }
}
