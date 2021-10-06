package no.nav.melosys.integrasjon.inngangsvilkar;

import java.util.Set;

import no.nav.melosys.domain.ErPeriode;


record VurderInngangsvilkaarRequest(
    Set<String> statsborgerskap,
    Set<String> arbeidsland,
    boolean erUkjenteEllerAlleEosLand,
    ErPeriode periode
) {
}
