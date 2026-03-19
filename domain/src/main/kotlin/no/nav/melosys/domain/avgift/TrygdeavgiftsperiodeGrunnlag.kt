package no.nav.melosys.domain.avgift

import jakarta.persistence.*
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode

@Entity
@Table(name = "trygdeavgiftsperiode_grunnlag")
class TrygdeavgiftsperiodeGrunnlag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trygdeavgiftsperiode_id", nullable = false)
    var trygdeavgiftsperiode: Trygdeavgiftsperiode,

    @ManyToOne
    @JoinColumn(name = "medlemskapsperiode_id")
    var medlemskapsperiode: Medlemskapsperiode? = null,

    @ManyToOne
    @JoinColumn(name = "lovvalgsperiode_id")
    var lovvalgsperiode: Lovvalgsperiode? = null,

    @ManyToOne
    @JoinColumn(name = "helseutgift_dekkes_periode_id")
    var helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode? = null,

    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "inntektsperiode_id")
    val inntektsperiode: Inntektsperiode,

    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "skatteforhold_id")
    val skatteforhold: SkatteforholdTilNorge,
) {

    fun hentAvgiftspliktigperiode(): AvgiftspliktigPeriode =
        medlemskapsperiode ?: lovvalgsperiode ?: helseutgiftDekkesPeriode
            ?: error("TrygdeavgiftsperiodeGrunnlag har ingen avgiftspliktig periode satt")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrygdeavgiftsperiodeGrunnlag) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String =
        "TrygdeavgiftsperiodeGrunnlag(id=$id, medlemskapsperiode=${medlemskapsperiode?.hentId()}, " +
            "lovvalgsperiode=${lovvalgsperiode?.hentId()}, helseutgiftDekkesPeriode=${helseutgiftDekkesPeriode?.id})"
}
