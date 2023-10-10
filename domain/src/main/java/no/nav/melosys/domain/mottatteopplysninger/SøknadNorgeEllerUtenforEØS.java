package no.nav.melosys.domain.mottatteopplysninger;

import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.RepresentantIUtlandet;

import static com.google.common.collect.MoreCollectors.onlyElement;

public class SøknadNorgeEllerUtenforEØS extends MottatteOpplysningerData {
    private Trygdedekninger trygdedekning;
    private RepresentantIUtlandet representantIUtlandet;

    public Trygdedekninger getTrygdedekning() {
        return trygdedekning;
    }

    public RepresentantIUtlandet getRepresentantIUtlandet() {
        return representantIUtlandet;
    }

    public void setTrygdedekning(Trygdedekninger trygdedekning) {
        this.trygdedekning = trygdedekning;
    }

    public void setRepresentantIUtlandet(RepresentantIUtlandet representantIUtlandet) {
        this.representantIUtlandet = representantIUtlandet;
    }

    public String hentArbeidsland() {
        return soeknadsland.landkoder.stream().collect(onlyElement());
    }
}
