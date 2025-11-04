package no.nav.melosys.domain

import jakarta.persistence.*
import no.nav.melosys.domain.avgift.AvgiftspliktigPeriode
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.exception.FunksjonellException
import java.time.LocalDate

@Entity
@Table(name = "lovvalg_periode")
class Lovvalgsperiode : PeriodeOmLovvalg, AvgiftspliktigPeriode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(optional = false)
    @JoinColumn(name = "beh_resultat_id", nullable = false, updatable = false)
    private var behandlingsresultat: Behandlingsresultat? = null

    override fun getBehandlingsresultat(): Behandlingsresultat? = behandlingsresultat

    fun setBehandlingsresultat(behandlingsresultat: Behandlingsresultat?) {
        this.behandlingsresultat = behandlingsresultat
    }

    @Column(name = "fom_dato", nullable = false)
    private var fom: LocalDate? = null

    override fun getFom(): LocalDate? = fom

    fun setFom(fom: LocalDate?) {
        this.fom = fom
    }

    @Column(name = "tom_dato")
    private var tom: LocalDate? = null

    override fun getTom(): LocalDate? = tom

    fun setTom(tom: LocalDate?) {
        this.tom = tom
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "lovvalgsland")
    private var lovvalgsland: Land_iso2? = null

    override fun getLovvalgsland(): Land_iso2? = lovvalgsland

    fun setLovvalgsland(lovvalgsland: Land_iso2?) {
        this.lovvalgsland = lovvalgsland
    }

    @Column(name = "lovvalg_bestemmelse")
    @Convert(converter = LovvalgBestemmelsekonverterer::class)
    private var bestemmelse: LovvalgBestemmelse? = null

    override fun getBestemmelse(): LovvalgBestemmelse? = bestemmelse

    fun setBestemmelse(bestemmelse: LovvalgBestemmelse?) {
        this.bestemmelse = bestemmelse
    }

    @Column(name = "tillegg_bestemmelse")
    @Convert(converter = LovvalgBestemmelsekonverterer::class)
    private var tilleggsbestemmelse: LovvalgBestemmelse? = null

    override fun getTilleggsbestemmelse(): LovvalgBestemmelse? = tilleggsbestemmelse

    fun setTilleggsbestemmelse(tilleggsbestemmelse: LovvalgBestemmelse?) {
        this.tilleggsbestemmelse = tilleggsbestemmelse
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "innvilgelse_resultat", nullable = false)
    var innvilgelsesresultat: InnvilgelsesResultat? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "medlemskapstype")
    var medlemskapstype: Medlemskapstyper? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "trygde_dekning")
    private var dekning: Trygdedekninger? = null

    @OneToMany(mappedBy = "grunnlagLovvalgsPeriode", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    var trygdeavgiftsperioder: MutableSet<Trygdeavgiftsperiode> = HashSet(1)

    fun clearTrygdeavgiftsperioder() {
        trygdeavgiftsperioder.forEach { it.grunnlagLovvalgsPeriode = null }
        trygdeavgiftsperioder.clear()
    }

    fun hentMedlemskapstype(): Medlemskapstyper = medlemskapstype ?: throw FunksjonellException("Lovvalgsperiode mangler medlemskapstype.")
    override fun hentId(): Long = id ?: throw FunksjonellException("Lovvalgsperiode mangler ID.")
    override fun getDekning(): Trygdedekninger? = dekning

    fun setDekning(dekning: Trygdedekninger?) {
        this.dekning = dekning
    }

    @Column(name = "medlperiode_id")
    private var medlPeriodeID: Long? = null

    override fun getMedlPeriodeID(): Long? = medlPeriodeID

    fun setMedlPeriodeID(medlPeriodeID: Long?) {
        this.medlPeriodeID = medlPeriodeID
    }

    fun addTrygdeavgiftsperiode(trygdeavgiftsperiode: Trygdeavgiftsperiode) {
        trygdeavgiftsperiode.grunnlagLovvalgsPeriode = this
        trygdeavgiftsperioder.add(trygdeavgiftsperiode)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Lovvalgsperiode) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "Lovvalgsperiode(" +
        "id=$id, " +
        "behandlingsresultat=$behandlingsresultat, " +
        "fom=$fom, " +
        "tom=$tom, " +
        "lovvalgsland=$lovvalgsland, " +
        "bestemmelse=$bestemmelse, " +
        "tilleggsbestemmelse=$tilleggsbestemmelse, " +
        "innvilgelsesresultat=$innvilgelsesresultat, " +
        "medlemskapstype=$medlemskapstype, " +
        "dekning=$dekning, " +
        "medlPeriodeID=$medlPeriodeID" +
        ")"

    override fun hentTrygdedekning(): Trygdedekninger {
        return dekning ?: throw FunksjonellException("Lovvalgsperiode mangler dekning.")
    }

    override fun erInnvilget(): Boolean = innvilgelsesresultat == InnvilgelsesResultat.INNVILGET
    override fun erPliktig(): Boolean = medlemskapstype == Medlemskapstyper.PLIKTIG

    fun erAvslått(): Boolean = innvilgelsesresultat == InnvilgelsesResultat.AVSLAATT

    fun harUgyldigTilstand(): Boolean = !erInnvilget() && !erAvslått()

    fun hentBestemmelse() = bestemmelse ?: error("bestemmelse er påkrevd for Lovvalgsperiode")

    fun hentFom() = fom ?: error("fom er påkrevd for Lovvalgsperiode")

    fun hentTom() = tom ?: error("tom er påkrevd for Lovvalgsperiode")

    fun hentBehandlingsresultat(): Behandlingsresultat =
        behandlingsresultat ?: error("behandlingsresultat er påkrevd for Lovvalgsperiode")

    fun hentInnvilgelsesresultat(): InnvilgelsesResultat =
        innvilgelsesresultat ?: error("innvilgelsesresultat er påkrevd for Lovvalgsperiode")

    fun hentMedlPeriodeID() = medlPeriodeID ?: error("medlPeriodeID er påkrevd for Lovvalgsperiode")

    companion object {
        @JvmStatic
        fun av(
            anmodningsperiodeSvar: AnmodningsperiodeSvar?,
            medlemskapstype: Medlemskapstyper
        ): Lovvalgsperiode {
            requireNotNull(anmodningsperiodeSvar) {
                "Kan ikke opprette lovvalgsperiode fra anmodningsperiode uten at et svar er registrert!"
            }

            val anmodningsperiode = anmodningsperiodeSvar.anmodningsperiode

            return Lovvalgsperiode().apply {
                setBestemmelse(anmodningsperiode.bestemmelse)

                if (anmodningsperiodeSvar.anmodningsperiodeSvarType == Anmodningsperiodesvartyper.DELVIS_INNVILGELSE) {
                    setFom(anmodningsperiodeSvar.innvilgetFom)
                    setTom(anmodningsperiodeSvar.innvilgetTom)
                } else {
                    setFom(anmodningsperiode.fom)
                    setTom(anmodningsperiode.tom)
                }

                val innvilgelsesResultat = if (anmodningsperiodeSvar.anmodningsperiodeSvarType == Anmodningsperiodesvartyper.AVSLAG)
                    InnvilgelsesResultat.AVSLAATT
                else
                    InnvilgelsesResultat.INNVILGET

                this.innvilgelsesresultat = innvilgelsesResultat
                this.medlemskapstype = medlemskapstype
                setMedlPeriodeID(anmodningsperiode.medlPeriodeID)
                setTilleggsbestemmelse(anmodningsperiode.tilleggsbestemmelse)

                if (innvilgelsesResultat != InnvilgelsesResultat.AVSLAATT) {
                    setLovvalgsland(anmodningsperiode.lovvalgsland)
                }
                setDekning(anmodningsperiode.dekning)
            }
        }

        @JvmStatic
        fun av(utpekingsperiode: Utpekingsperiode): Lovvalgsperiode = Lovvalgsperiode().apply {
            setFom(utpekingsperiode.fom)
            setTom(utpekingsperiode.tom)
            setBestemmelse(utpekingsperiode.bestemmelse)
            setTilleggsbestemmelse(utpekingsperiode.tilleggsbestemmelse)
            setLovvalgsland(utpekingsperiode.lovvalgsland)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            setDekning(Trygdedekninger.UTEN_DEKNING)
            medlemskapstype = Medlemskapstyper.UNNTATT
            setMedlPeriodeID(utpekingsperiode.medlPeriodeID)
        }
    }
}
