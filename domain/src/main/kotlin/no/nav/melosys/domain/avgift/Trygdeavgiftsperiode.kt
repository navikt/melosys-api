package no.nav.melosys.domain.avgift

import jakarta.persistence.*
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.exception.FunksjonellException
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

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
    @AttributeOverride(name = "verdi", column = Column(name = "trygdeavgift_beloep_mnd_verdi"))
    @AttributeOverride(name = "valuta", column = Column(name = "trygdeavgift_beloep_mnd_valuta"))
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

    @ManyToOne
    @JoinColumn(name = "lovvalg_periode_id")
    var grunnlagLovvalgsPeriode: Lovvalgsperiode? = null,

    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "skatteforhold_id")
    val grunnlagSkatteforholdTilNorge: SkatteforholdTilNorge? = null,

    ) : ErPeriode {

    val grunnlagMedlemskapsperiodeNotNull: Medlemskapsperiode
        get() = grunnlagMedlemskapsperiode ?: error("grunnlagMedlemskapsperiode er null")

    fun hentGrunnlagAvgiftsperiode(): AvgiftspliktigPeriode =
        grunnlagMedlemskapsperiode ?: grunnlagHelseutgiftDekkesPeriode ?: grunnlagLovvalgsPeriode ?: error("grunnlagAvgiftsperiode er null")

    fun hentGrunnlagInntekstperiode(): Inntektsperiode =
        grunnlagInntekstperiode ?: error("grunnlagInntekstperiode er påkrevd for Trygdeavgiftsperiode")

    fun hentGrunnlagSkatteforholdTilNorge(): SkatteforholdTilNorge =
        grunnlagSkatteforholdTilNorge ?: error("grunnlagSkatteforholdTilNorge er påkrevd for Trygdeavgiftsperiode")

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
        grunnlagLovvalgsPeriode: Lovvalgsperiode? = this.grunnlagLovvalgsPeriode,
        grunnlagSkatteforholdTilNorge: SkatteforholdTilNorge? = this.grunnlagSkatteforholdTilNorge,
    ) = Trygdeavgiftsperiode(
        id = id,
        periodeFra = periodeFra,
        periodeTil = periodeTil,
        trygdeavgiftsbeløpMd = trygdeavgiftsbeløpMd,
        trygdesats = trygdesats,
        grunnlagInntekstperiode = grunnlagInntekstperiode,
        grunnlagMedlemskapsperiode = grunnlagMedlemskapsperiode,
        grunnlagHelseutgiftDekkesPeriode = grunnlagHelseutgiftDekkesPeriode,
        grunnlagLovvalgsPeriode = grunnlagLovvalgsPeriode,
        grunnlagSkatteforholdTilNorge = grunnlagSkatteforholdTilNorge,
    )

    fun erLikForSatsendring(other: Trygdeavgiftsperiode): Boolean =
        periodeFra == other.periodeFra &&
            periodeTil == other.periodeTil &&
            trygdeavgiftsbeløpMd == other.trygdeavgiftsbeløpMd &&
            trygdesats.compareTo(other.trygdesats) == 0 &&
            grunnlagInntekstperiode == other.grunnlagInntekstperiode &&
            grunnlagMedlemskapsperiode == other.grunnlagMedlemskapsperiode &&
            grunnlagHelseutgiftDekkesPeriode == other.grunnlagHelseutgiftDekkesPeriode &&
            grunnlagLovvalgsPeriode == other.grunnlagLovvalgsPeriode &&
            grunnlagSkatteforholdTilNorge == other.grunnlagSkatteforholdTilNorge


    fun addGrunnlag(avgiftspliktigperiode: AvgiftspliktigPeriode) {
        val existingGrunnlagCount = listOfNotNull(
            grunnlagMedlemskapsperiode,
            grunnlagHelseutgiftDekkesPeriode,
            grunnlagLovvalgsPeriode
        ).size

        if (existingGrunnlagCount > 0) {
            error("Trygdeavgiftsperiode har allerede et grunnlag satt. Kan ikke ha flere grunnlag samtidig.")
        }

        when (avgiftspliktigperiode) {
            is Medlemskapsperiode -> grunnlagMedlemskapsperiode = avgiftspliktigperiode
            is HelseutgiftDekkesPeriode -> grunnlagHelseutgiftDekkesPeriode = avgiftspliktigperiode
            is Lovvalgsperiode -> grunnlagLovvalgsPeriode = avgiftspliktigperiode
            else -> throw FunksjonellException("Ukjent type: ${avgiftspliktigperiode::class.java.simpleName}")
        }
    }

    override fun toString(): String {
        return "Trygdeavgiftsperiode(id=$id, periodeFra=$periodeFra, periodeTil=$periodeTil, " +
            "trygdeavgiftsbeløpMd=$trygdeavgiftsbeløpMd, trygdesats=$trygdesats)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Trygdeavgiftsperiode) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    /**
     * Finner og setter riktig grunnlagsperiode fra behandlingsresultat basert på
     * originalens grunnlagstype. Dette unngår feilen med å anta at alle
     * avgiftspliktige perioder er av samme type.
     *
     * @param behandlingsresultatReplika Behandlingsresultatet som inneholder de nye periodene
     * @param trygdeavgiftsperiodeOriginal Den originale trygdeavgiftsperioden som har grunnlag-referansen
     * @return Denne trygdeavgiftsperioden (for chaining)
     * @throws IllegalStateException hvis grunnlagsperioden ikke finnes eller hvis ingen grunnlag er satt
     */
    fun setGrunnlagFromReplica(
        behandlingsresultatReplika: Behandlingsresultat,
        trygdeavgiftsperiodeOriginal: Trygdeavgiftsperiode
    ): Trygdeavgiftsperiode = apply {
        val grunnlag: AvgiftspliktigPeriode = when {
            trygdeavgiftsperiodeOriginal.grunnlagMedlemskapsperiode != null -> {
                behandlingsresultatReplika.medlemskapsperioder
                    .find { it.id == trygdeavgiftsperiodeOriginal.grunnlagMedlemskapsperiode?.id }
                    ?: throw IllegalStateException(
                        "Medlemskapsperiode med id ${trygdeavgiftsperiodeOriginal.grunnlagMedlemskapsperiode?.id} ikke funnet"
                    )
            }
            trygdeavgiftsperiodeOriginal.grunnlagLovvalgsPeriode != null -> {
                behandlingsresultatReplika.lovvalgsperioder
                    .find { it.id == trygdeavgiftsperiodeOriginal.grunnlagLovvalgsPeriode?.id }
                    ?: throw IllegalStateException(
                        "Lovvalgsperiode med id ${trygdeavgiftsperiodeOriginal.grunnlagLovvalgsPeriode?.id} ikke funnet"
                    )
            }
            trygdeavgiftsperiodeOriginal.grunnlagHelseutgiftDekkesPeriode != null -> {
                behandlingsresultatReplika.helseutgiftDekkesPeriode
                    ?: throw IllegalStateException("HelseutgiftDekkesPeriode ikke funnet i behandlingsresultat")
            }
            else -> error("Ingen grunnlag satt på original trygdeavgiftsperiode med id ${trygdeavgiftsperiodeOriginal.id}")
        }

        addGrunnlag(grunnlag)
    }

    /**
     * Setter grunnlag basert på UUID for avgiftspliktig periode.
     * Søker gjennom alle typer av avgiftspliktige perioder for å finne riktig periode.
     * Dette unngår feilen med å anta at alle perioder er av samme type.
     *
     * @param behandlingsresultat Behandlingsresultatet som inneholder periodene
     * @param avgiftspliktigPeriodeUUID UUID for den avgiftspliktige perioden
     * @param idMapper Funksjon som konverterer Long ID til UUID
     * @return Denne trygdeavgiftsperioden (for chaining)
     * @throws IllegalStateException hvis perioden ikke finnes
     */
    fun setGrunnlagByUUID(
        behandlingsresultat: Behandlingsresultat,
        avgiftspliktigPeriodeUUID: UUID,
        idMapper: (Long) -> UUID
    ): Trygdeavgiftsperiode = apply {
        val grunnlag: AvgiftspliktigPeriode = behandlingsresultat.medlemskapsperioder
            .firstOrNull { idMapper(it.hentId()) == avgiftspliktigPeriodeUUID }
            ?: behandlingsresultat.lovvalgsperioder
                .firstOrNull { idMapper(it.hentId()) == avgiftspliktigPeriodeUUID }
            ?: behandlingsresultat.helseutgiftDekkesPeriode?.takeIf { idMapper(it.hentId()) == avgiftspliktigPeriodeUUID }
            ?: throw IllegalStateException(
                "Fant ingen avgiftspliktig periode med UUID $avgiftspliktigPeriodeUUID"
            )

        addGrunnlag(grunnlag)
    }



    companion object // Tom - muliggjør utvidelsefunksjoner i tester
}
