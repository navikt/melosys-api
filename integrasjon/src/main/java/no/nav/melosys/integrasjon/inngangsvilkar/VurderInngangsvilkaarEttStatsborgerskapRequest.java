package no.nav.melosys.integrasjon.inngangsvilkar;

import java.util.Set;

import no.nav.melosys.domain.ErPeriode;

@Deprecated
class VurderInngangsvilkaarEttStatsborgerskapRequest {
    private String statsborgerskap;
    private Set<String> arbeidsland;
    private ErPeriode periode;

    VurderInngangsvilkaarEttStatsborgerskapRequest(String statsborgerskap, Set<String> arbeidsland, ErPeriode periode) {
        this.statsborgerskap = statsborgerskap;
        this.arbeidsland = arbeidsland;
        this.periode = periode;
    }

    public String getStatsborgerskap() {
        return statsborgerskap;
    }

    public Set<String> getArbeidsland() {
        return arbeidsland;
    }

    public ErPeriode getPeriode() {
        return periode;
    }
}
