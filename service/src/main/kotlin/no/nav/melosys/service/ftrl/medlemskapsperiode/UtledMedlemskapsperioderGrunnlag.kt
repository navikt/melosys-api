package no.nav.melosys.service.ftrl.medlemskapsperiode

import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import java.time.LocalDate

data class UtledMedlemskapsperioderGrunnlag(
    val søknadsperiode: ErPeriode,
    val trygdedekning: Trygdedekninger,
    val mottaksdatoSøknad: LocalDate?,
    val bestemmelse: Bestemmelse,
    val behandlingstema: Behandlingstema? = null,
) {
    val mottaksdatoSøknadNotNull: LocalDate
        get() = mottaksdatoSøknad ?: throw NullPointerException("mottakdatoSøknad kan ikke være null")
}
