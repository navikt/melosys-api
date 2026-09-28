package no.nav.melosys.domain.avgift

import jakarta.persistence.*
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.kodeverk.EndeligAvgiftValg
import java.math.BigDecimal

@Entity
@Table(name = "aarsavregning")
class Årsavregning(
    @Id
    var id: Long = 0,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "behandlingsresultat_id")
    var behandlingsresultat: Behandlingsresultat? = null,

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

    @Column(name = "har_innbetalt_trygdeavgift")
    var harInnbetaltTrygdeavgift: Boolean? = null,

    @Column(name = "innbetalt_trygdeavgift")
    var innbetaltTrygdeavgift: BigDecimal? = null,

    @Column(name = "manuelt_avgift_beloep")
    var manueltAvgiftBeloep: BigDecimal? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "endelig_avgift_valg")
    var endeligAvgiftValg: EndeligAvgiftValg? = null,

    @Column(name = "har_skjoennsfastsatt_inntektsgrunnlag")
    var harSkjoennsfastsattInntektsgrunnlag: Boolean = false
) {
    val hentBehandlingsresultat: Behandlingsresultat
        get() = behandlingsresultat ?: error("behandlingsresultat er ikke satt for årsavregning med id: $id")

    val hentTidligereBehandlingsresultat: Behandlingsresultat
        get() = tidligereBehandlingsresultat ?: error("tidligereBehandlingsresultat er ikke satt for årsavregning med id: $id")

    val hentTilFaktureringBeloep: BigDecimal
        get() = tilFaktureringBeloep ?: error("tilFaktureringBeloep er ikke satt for årsavregning med id: $id")

    /**
     * Det som er betalt for året før denne årsavregningen: tidligere fakturert pluss innbetalt i Avgiftssystemet, minus
     * innbetaling som allerede inngikk i tidligere fakturert. Utledes som endelig avgift minus [tilFaktureringBeloep],
     * slik at tallet som vises alltid stemmer med beløpet som faktureres.
     */
    val hentTidligereBetaltTotalt: BigDecimal
        get() = (manueltAvgiftBeloep ?: beregnetAvgiftBelop ?: error("Endelig avgift er ikke satt for årsavregning med id: $id"))
            .subtract(hentTilFaktureringBeloep)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Årsavregning) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    /**
     * [tidligereÅrsavregningInnbetalt] er innbetalt beløp fra Avgiftssystemet i siste vedtatte årsavregning for samme år,
     * og skal bare oppgis når [tidligereFakturertBeloep] er hentet fra den samme årsavregningen. Da inngår innbetalingen
     * allerede i tidligere fakturert, og legges tilbake slik at bare ny innbetaling trekkes fra. Ellers null.
     */
    fun beregnTilFaktureringsBeloep(tidligereÅrsavregningInnbetalt: BigDecimal?) {
        if (beregnetAvgiftBelop == null && manueltAvgiftBeloep == null) return

        tilFaktureringBeloep = (manueltAvgiftBeloep ?: beregnetAvgiftBelop)!!
            .subtract(tidligereFakturertBeloep ?: BigDecimal.ZERO)
            .subtract(innbetaltTrygdeavgift ?: BigDecimal.ZERO)
            .add(tidligereÅrsavregningInnbetalt ?: BigDecimal.ZERO)
    }

    companion object // for å kunne legge på test forTest DSL
}
