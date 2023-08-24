package no.nav.melosys.service.medlemskapsperiode;

import java.time.LocalDate;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public class UtledMedlemskapsperioderRequest {

    private ErPeriode søknadsperiode;
    private final Trygdedekninger trygdedekning;
    private final LocalDate mottaksdatoSøknad;
    private final String arbeidsland;

    public UtledMedlemskapsperioderRequest(ErPeriode søknadsperiode,
                                           Trygdedekninger trygdedekning,
                                           LocalDate mottaksdatoSøknad,
                                           String arbeidsland) {
        this.søknadsperiode = søknadsperiode;
        this.trygdedekning = trygdedekning;
        this.mottaksdatoSøknad = mottaksdatoSøknad;
        this.arbeidsland = arbeidsland;
    }

    public void setSøknadsperiode(ErPeriode søknadsperiode) {
        this.søknadsperiode = søknadsperiode;
    }

    public ErPeriode getSøknadsperiode() {
        return søknadsperiode;
    }

    public Trygdedekninger getTrygdedekning() {
        return trygdedekning;
    }

    public LocalDate getMottaksdatoSøknad() {
        return mottaksdatoSøknad;
    }

    public String getArbeidsland() {
        return arbeidsland;
    }
}
