package no.nav.melosys.domain.mottatteopplysninger;

import no.nav.melosys.domain.kodeverk.Land_iso2;

public class AnmodningEllerAttest extends MottatteOpplysningerData {
    private Land_iso2 avsenderland;
    private Land_iso2 lovvalgsland;

    public Land_iso2 getAvsenderland() {
        return avsenderland;
    }

    public void setAvsenderland(Land_iso2 avsenderland) {
        this.avsenderland = avsenderland;
    }

    public Land_iso2 getLovvalgsland() {
        return lovvalgsland;
    }

    public void setLovvalgsland(Land_iso2 lovvalgsland) {
        this.lovvalgsland = lovvalgsland;
    }
}
