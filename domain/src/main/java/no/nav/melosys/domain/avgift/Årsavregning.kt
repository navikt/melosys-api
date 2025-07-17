package no.nav.melosys.domain.avgift

import jakarta.persistence.*
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.kodeverk.EndeligAvgiftValg
import java.math.BigDecimal

@Entity
@Table(name = "aarsavregning")
data class Årsavregning(
    @Id
    var id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "behandlingsresultat_id")
    var behandlingsresultat: Behandlingsresultat,

    @Column(name = "aar", nullable = false, updatable = false)
    var aar: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tidligere_resultat_id")
    var tidligereBehandlingsresultat: Behandlingsresultat? = null,

    @Column(name = "tidligere_fakturert_beloep")
    var tidligereFakturertBeloep: BigDecimal? = null,

    @Column(name = "beregnet_avgift_belop")
    var beregnetAvgiftBelop: BigDecimal? = null,

    @Column(name = "til_fakturering_beloep")
    var tilFaktureringBeloep: BigDecimal? = null,

    @Column(name = "har_trygdeavgift_fra_avgiftssystemet")
    var harTrygdeavgiftFraAvgiftssystemet: Boolean? = null,

    @Column(name = "trygdeavgift_fra_avgiftssystemet")
    var trygdeavgiftFraAvgiftssystemet: BigDecimal? = null,

    @Column(name = "manuelt_avgift_beloep")
    var manueltAvgiftBeloep: BigDecimal? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "endelig_avgift_valg")
    var endeligAvgiftValg: EndeligAvgiftValg? = null,

    @Column(name = "har_skjoennsfastsatt_inntektsgrunnlag")
    var harSkjoennsfastsattInntektsgrunnlag: Boolean = false
) {
    fun beregnTilFaktureringsBeloep() {
        if (beregnetAvgiftBelop == null && manueltAvgiftBeloep == null) return

        val grunnlag = manueltAvgiftBeloep ?: beregnetAvgiftBelop
        val tidligereFakturert = tidligereFakturertBeloep ?: BigDecimal.ZERO
        val avgiftFraSystemet = trygdeavgiftFraAvgiftssystemet ?: BigDecimal.ZERO
        val tidligereSystemavgift = tidligereBehandlingsresultat?.årsavregning?.trygdeavgiftFraAvgiftssystemet ?: BigDecimal.ZERO

        tilFaktureringBeloep = grunnlag
            ?.subtract(tidligereFakturert)
            ?.subtract(avgiftFraSystemet)
            ?.add(tidligereSystemavgift)
    }
}
