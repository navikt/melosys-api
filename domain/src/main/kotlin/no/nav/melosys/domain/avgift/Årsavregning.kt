package no.nav.melosys.domain.avgift

import jakarta.persistence.*
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.kodeverk.EndeligAvgiftValg
import java.math.BigDecimal

@Entity
@Table(name = "aarsavregning")
open class Årsavregning() {
    @Id
    var id: Long? = null

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "behandlingsresultat_id")
    var behandlingsresultat: Behandlingsresultat? = null

    @Column(name = "aar", nullable = false, updatable = false)
    var aar: Int? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tidligere_resultat_id")
    var tidligereBehandlingsresultat: Behandlingsresultat? = null

    @Column(name = "tidligere_fakturert_beloep")
    var tidligereFakturertBeloep: BigDecimal? = null

    @Column(name = "beregnet_avgift_belop")
    var beregnetAvgiftBelop: BigDecimal? = null

    @Column(name = "til_fakturering_beloep")
    var tilFaktureringBeloep: BigDecimal? = null

    @Column(name = "har_trygdeavgift_fra_avgiftssystemet")
    var harTrygdeavgiftFraAvgiftssystemet: Boolean? = null

    @Column(name = "trygdeavgift_fra_avgiftssystemet")
    var trygdeavgiftFraAvgiftssystemet: BigDecimal? = null

    @Column(name = "manuelt_avgift_beloep")
    var manueltAvgiftBeloep: BigDecimal? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "endelig_avgift_valg")
    var endeligAvgiftValg: EndeligAvgiftValg? = null

    @Column(name = "har_skjoennsfastsatt_inntektsgrunnlag")
    var harSkjoennsfastsattInntektsgrunnlag: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Årsavregning) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    fun beregnTilFaktureringsBeloep() {
        if (beregnetAvgiftBelop == null && manueltAvgiftBeloep == null) return

        tilFaktureringBeloep = (manueltAvgiftBeloep ?: beregnetAvgiftBelop)!!
            .subtract(tidligereFakturertBeloep ?: BigDecimal.ZERO)
            .subtract(trygdeavgiftFraAvgiftssystemet ?: BigDecimal.ZERO)
            .add(hentTidligereTrygdeavgiftFraAvgiftssystemet())
    }

    private fun hentTidligereTrygdeavgiftFraAvgiftssystemet(): BigDecimal {
        if (tidligereBehandlingsresultat?.årsavregning == null) {
            return BigDecimal.ZERO
        }

        return tidligereBehandlingsresultat!!.årsavregning!!.trygdeavgiftFraAvgiftssystemet ?: BigDecimal.ZERO
    }
}
