package no.nav.melosys.service.medlemskapsperiode;

import java.time.LocalDate;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public class UtledMedlemskapsperioderDto {

    private final ErPeriode søknadsperiode;
    private final Trygdedekninger trygdedekning;
    private final LocalDate mottaksdatoSøknad;
    private final Folketrygdloven_kap2_bestemmelser bestemmelse;

    public UtledMedlemskapsperioderDto(ErPeriode søknadsperiode,
                                       Trygdedekninger trygdedekning,
                                       LocalDate mottaksdatoSøknad,
                                       Folketrygdloven_kap2_bestemmelser bestemmelse) {
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

    public Folketrygdloven_kap2_bestemmelser getBestemmelse() {
        return bestemmelse;
    }
}
