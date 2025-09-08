package no.nav.melosys.domain.helseutgiftdekkesperiode

import jakarta.persistence.*
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Land_iso2
import java.time.LocalDate


/*
    I denne konteksten bruker vi verbet "dekker/dekkes" fremfor substantivet "dekning" (som i f.eks. trygdedekning).
    Dette skyldes at lovvalgsbestemmelsene henviser til at helseutgiftene dekkes av kompetent stat (i dette tilfelle Norge)
*/
@Entity
@Table(name = "helseutgift_dekkes_periode")
class HelseutgiftDekkesPeriode(
    @OneToOne(optional = false)
    @JoinColumn(name = "beh_resultat_id", nullable = false)
    var behandlingsresultat: Behandlingsresultat,

    @Column(name = "fom_dato", nullable = false)
    var fomDato: LocalDate,

    @Column(name = "tom_dato", nullable = false)
    var tomDato: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(name = "bosted_landkode", nullable = false)
    var bostedLandkode: Land_iso2
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToMany(mappedBy = "grunnlagHelseutgiftDekkesPeriode", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    var trygdeavgiftsperioder: MutableSet<Trygdeavgiftsperiode> = HashSet(1)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HelseutgiftDekkesPeriode) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    fun clearTrygdeavgiftsperioder() {
        trygdeavgiftsperioder.forEach { it.grunnlagHelseutgiftDekkesPeriode = null }
        trygdeavgiftsperioder.clear()
    }

    companion object //For å kunne legge på forTest DSL.
}
