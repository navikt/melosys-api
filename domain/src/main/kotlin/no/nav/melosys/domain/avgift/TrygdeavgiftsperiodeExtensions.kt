package no.nav.melosys.domain.avgift

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import java.util.*

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
fun Trygdeavgiftsperiode.setGrunnlagFromReplica(
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
        else -> throw IllegalStateException(
            "Ingen grunnlag satt på original trygdeavgiftsperiode med id ${trygdeavgiftsperiodeOriginal.id}"
        )
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
fun Trygdeavgiftsperiode.setGrunnlagByUUID(
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

/**
 * Returnerer grunnlaget (avgiftspliktig periode) for denne trygdeavgiftsperioden.
 * Sjekker alle mulige grunnlagstyper.
 *
 * @return Avgiftspliktig periode som denne trygdeavgiftsperioden er basert på
 * @throws IllegalStateException hvis ingen grunnlag er satt
 */
fun Trygdeavgiftsperiode.hentGrunnlag(): AvgiftspliktigPeriode {
    return grunnlagMedlemskapsperiode
        ?: grunnlagLovvalgsPeriode
        ?: grunnlagHelseutgiftDekkesPeriode
        ?: throw IllegalStateException("Ingen grunnlag satt på trygdeavgiftsperiode med id $id")
}

/**
 * Knytter denne trygdeavgiftsperioden til riktig avgiftspliktig periode basert på eksisterende grunnlag.
 * Denne metoden forutsetter at grunnlag allerede er satt på trygdeavgiftsperioden.
 *
 * @return Denne trygdeavgiftsperioden (for chaining)
 */
fun Trygdeavgiftsperiode.addToGrunnlag(): Trygdeavgiftsperiode = apply {
    hentGrunnlag().addTrygdeavgiftsperiode(this)
}
