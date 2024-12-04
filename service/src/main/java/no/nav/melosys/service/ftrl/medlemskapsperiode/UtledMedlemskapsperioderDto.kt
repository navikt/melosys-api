package no.nav.melosys.service.ftrl.medlemskapsperiode;

import java.time.LocalDate;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.kodeverk.Bestemmelse;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public class UtledMedlemskapsperioderDto {

    private final ErPeriode søknadsperiode;
    private final Trygdedekninger trygdedekning;
    private final LocalDate mottaksdatoSøknad;
    private final Bestemmelse bestemmelse;

    public UtledMedlemskapsperioderDto(ErPeriode søknadsperiode,
                                       Trygdedekninger trygdedekning,
                                       LocalDate mottaksdatoSøknad,
                                       Bestemmelse bestemmelse) {
        this.søknadsperiode = søknadsperiode;
        this.trygdedekning = trygdedekning;
        this.mottaksdatoSøknad = mottaksdatoSøknad;
        this.bestemmelse = bestemmelse;
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

    public Bestemmelse getBestemmelse() {
        return bestemmelse;
    }
}
