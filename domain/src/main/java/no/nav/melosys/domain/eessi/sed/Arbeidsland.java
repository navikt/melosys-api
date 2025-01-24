package no.nav.melosys.domain.eessi.sed;

import java.util.List;

public class Arbeidsland {
    private String land;
    private List<Arbeidssted> arbeidssted;

    // SedGrunnlagMapperTest.lagSedGrunnlag trenger default konstruktør for deserialisere testdata til SedGrunnlagDto. Kan fjerne dette når vi konvertere til Kotlin
    public Arbeidsland() {}

    public Arbeidsland(String land, List<Arbeidssted> arbeidssted) {
        this.land = land;
        this.arbeidssted = arbeidssted;
    }

    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }

    public List<Arbeidssted> getArbeidssted() {
        return arbeidssted;
    }

    public Boolean harFastArbeidssted(){
        return arbeidssted.stream().anyMatch(arbSted -> arbSted.getAdresse().erGyldigAdresse());
    }

    public void setArbeidssted(List<Arbeidssted> arbeidssted) {
        this.arbeidssted = arbeidssted;
    }
}
