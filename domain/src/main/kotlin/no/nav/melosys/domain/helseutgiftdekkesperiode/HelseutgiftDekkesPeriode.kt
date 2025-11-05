package no.nav.melosys.domain.helseutgiftdekkesperiode

import jakarta.persistence.*
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.AvgiftspliktigPeriode
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.exception.FunksjonellException
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
) : AvgiftspliktigPeriode {
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

    fun addTrygdeavgiftsperiode(trygdeavgiftsperiode: Trygdeavgiftsperiode) {
        trygdeavgiftsperiode.grunnlagHelseutgiftDekkesPeriode = this
        trygdeavgiftsperioder.add(trygdeavgiftsperiode)
    }

    override fun getFom(): LocalDate? = fomDato

    override fun getTom(): LocalDate? = tomDato

    override fun erInnvilget(): Boolean = true

    override fun erPliktig(): Boolean = true
    override fun hentId(): Long = id ?: throw FunksjonellException("HelseutgiftDekkesPeriode mangler ID.")
    override fun erOpphørt(): Boolean = false

    fun hentMedlemskapstype(): Medlemskapstyper = Medlemskapstyper.PLIKTIG

    override fun hentTrygdedekning(): Trygdedekninger =
        // TODO: Bruker FULL_DEKNING inntil fag finner et mer passende verdi
        Trygdedekninger.FULL_DEKNING

    companion object //For å kunne legge på forTest DSL.
}
