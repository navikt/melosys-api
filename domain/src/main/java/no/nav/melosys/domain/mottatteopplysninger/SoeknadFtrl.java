package no.nav.melosys.domain.mottatteopplysninger;

import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public class SoeknadFtrl extends Soeknad {
    private Trygdedekninger trygdedekning;

    public Trygdedekninger getTrygdedekning() {
        return trygdedekning;
    }

    public void setTrygdedekning(Trygdedekninger trygdedekning) {
        this.trygdedekning = trygdedekning;
    }
}
