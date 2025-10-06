package no.nav.melosys.domain.eessi

import no.nav.melosys.domain.AnmodningsperiodeSvar
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper

/**
 * Representerer et svar på anmodning om unntak i EESSI-kontekst.
 */
data class SvarAnmodningUnntak(
    val beslutning: Beslutning? = null,
    val begrunnelse: String? = null,
    val delvisInnvilgetPeriode: Periode? = null
) {
    enum class Beslutning {
        INNVILGELSE,
        DELVIS_INNVILGELSE,
        AVSLAG
    }

    companion object {
        @JvmStatic
        fun av(anmodningsperiodeSvar: AnmodningsperiodeSvar) = SvarAnmodningUnntak(
            beslutning = hentBeslutningForSvartype(anmodningsperiodeSvar.anmodningsperiodeSvarType),
            begrunnelse = anmodningsperiodeSvar.begrunnelseFritekst,
            delvisInnvilgetPeriode = if (anmodningsperiodeSvar.innvilgetFom != null || anmodningsperiodeSvar.innvilgetTom != null) {
                Periode(anmodningsperiodeSvar.innvilgetFom, anmodningsperiodeSvar.innvilgetTom)
            } else null
        )

        private fun hentBeslutningForSvartype(anmodningsperiodeSvarType: Anmodningsperiodesvartyper): Beslutning =
            when (anmodningsperiodeSvarType) {
                Anmodningsperiodesvartyper.INNVILGELSE -> Beslutning.INNVILGELSE
                Anmodningsperiodesvartyper.DELVIS_INNVILGELSE -> Beslutning.DELVIS_INNVILGELSE
                Anmodningsperiodesvartyper.AVSLAG -> Beslutning.AVSLAG
            }
    }
}
