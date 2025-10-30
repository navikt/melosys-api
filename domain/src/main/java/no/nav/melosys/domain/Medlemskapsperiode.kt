package no.nav.melosys.domain

import jakarta.persistence.*
import no.nav.melosys.domain.avgift.AvgiftspliktigPeriode
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.jpa.MedlemskapBestemmelsekonverter
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.time.LocalDate

@Entity
@Table(name = "medlemskapsperiode")
class Medlemskapsperiode : HarBestemmelse<Bestemmelse?>, AvgiftspliktigPeriode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(optional = false)
    @JoinColumn(name = "behandlingsresultat_id", nullable = false, updatable = false)
    var behandlingsresultat: Behandlingsresultat? = null

    @Column(name = "fom_dato", nullable = false)
    private var fom: LocalDate? = null

    @Column(name = "tom_dato")
    private var tom: LocalDate? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "innvilgelse_resultat", nullable = false)
    var innvilgelsesresultat: InnvilgelsesResultat? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "medlemskapstype", nullable = false)
    var medlemskapstype: Medlemskapstyper? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "trygde_dekning", nullable = false)
    var trygdedekning: Trygdedekninger? = null

    @Column(name = "bestemmelse", nullable = false)
    @Convert(converter = MedlemskapBestemmelsekonverter::class)
    private var bestemmelse: Bestemmelse? = null

    @OneToMany(
        mappedBy = "grunnlagMedlemskapsperiode",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER,
        orphanRemoval = true
    )
    var trygdeavgiftsperioder: MutableSet<Trygdeavgiftsperiode> = HashSet(1)

    @Column(name = "medlperiode_id")
    var medlPeriodeID: Long? = null

    override fun getFom(): LocalDate? = fom

    fun setFom(fom: LocalDate?) {
        this.fom = fom
    }

    override fun getTom(): LocalDate? = tom

    fun setTom(tom: LocalDate?) {
        this.tom = tom
    }

    override fun getBestemmelse(): Bestemmelse? = bestemmelse

    fun setBestemmelse(bestemmelse: Bestemmelse?) {
        this.bestemmelse = bestemmelse
    }

    fun hentId() = id ?: error("id er påkrevd for Medlemskapsperiode")
    fun hentBehandlingsresultat() = behandlingsresultat ?: error("behandlingsresultat er påkrevd for Medlemskapsperiode")
    fun hentInnvilgelsesresultat() = innvilgelsesresultat ?: error("innvilgelsesresultat er påkrevd for Medlemskapsperiode")
    fun hentMedlemskapstype() = medlemskapstype ?: error("medlemskapstype er påkrevd for Medlemskapsperiode")
    override fun hentTrygdedekning() = trygdedekning ?: error("trygdedekning er påkrevd for Medlemskapsperiode")
    fun hentBestemmelse() = bestemmelse ?: error("bestemmelse er påkrevd for Medlemskapsperiode")
    fun hentFom() = fom ?: error("fom er påkrevd for Medlemskapsperiode")
    fun hentTom() = tom ?: error("tom er påkrevd for Medlemskapsperiode")
    fun hentMedlPeriodeID() = medlPeriodeID ?: error("medlPeriodeID er påkrevd for Medlemskapsperiode")

    fun erInnvilget(): Boolean = innvilgelsesresultat == InnvilgelsesResultat.INNVILGET

    fun erOpphørt(): Boolean = innvilgelsesresultat == InnvilgelsesResultat.OPPHØRT

    fun erAvslaatt(): Boolean = innvilgelsesresultat == InnvilgelsesResultat.AVSLAATT

    fun erFrivillig(): Boolean = medlemskapstype == Medlemskapstyper.FRIVILLIG

    fun erPliktig(): Boolean = medlemskapstype == Medlemskapstyper.PLIKTIG

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Medlemskapsperiode) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "Medlemskapsperiode{" +
        "id=" + id +
        ", behandlingsresultat=" + behandlingsresultat +
        ", fom=" + fom +
        ", tom=" + tom +
        ", innvilgelsesresultat=" + innvilgelsesresultat +
        ", medlemskapstype=" + medlemskapstype +
        ", trygdedekning=" + trygdedekning +
        ", medlPeriodeID=" + medlPeriodeID +
        '}'

    fun addTrygdeavgiftsperiode(trygdeavgiftsperiode: Trygdeavgiftsperiode) {
        trygdeavgiftsperiode.grunnlagMedlemskapsperiode = this
        trygdeavgiftsperioder.add(trygdeavgiftsperiode)
    }

    fun avkortTomDato(gjelderÅr: Int) {
        if (this.overlapperMedÅr(gjelderÅr) && this.hentTom().year > gjelderÅr) {
            this.tom = LocalDate.of(gjelderÅr, 12, 31)
        }
    }

    fun avkortFomDato(gjelderÅr: Int) {
        if (this.overlapperMedÅr(gjelderÅr) && this.hentFom().year < gjelderÅr) {
            this.fom = LocalDate.of(gjelderÅr, 1, 1)
        }
    }

    fun clearTrygdeavgiftsperioder() {
        trygdeavgiftsperioder.forEach { it.grunnlagMedlemskapsperiode = null }
        trygdeavgiftsperioder.clear()
    }
}
