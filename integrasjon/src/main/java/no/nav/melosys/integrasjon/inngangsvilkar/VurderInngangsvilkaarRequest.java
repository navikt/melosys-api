package no.nav.melosys.integrasjon.inngangsvilkar;

import java.util.Collection;

import no.nav.melosys.domain.ErPeriode;


class VurderInngangsvilkaarRequest {
    private String statsborgerskap;
    private Collection<String> arbeidsland;
    private ErPeriode periode;

    VurderInngangsvilkaarRequest(String statsborgerskap, Collection<String> arbeidsland, ErPeriode periode) {
        this.statsborgerskap = statsborgerskap;
        this.arbeidsland = arbeidsland;
        this.periode = periode;
    }

    public String getStatsborgerskap() {
        return statsborgerskap;
    }

    public Collection<String> getArbeidsland() {
        return arbeidsland;
    }

    public ErPeriode getPeriode() {
        return periode;
    }
}
