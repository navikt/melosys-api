package no.nav.melosys.domain.mottatteopplysninger;

import no.nav.melosys.domain.kodeverk.Landkoder;

public class AnmodningEllerAttest extends MottatteOpplysningerData{
    private Landkoder avsenderland;
    private Landkoder lovvalgsland;

    public Landkoder getAvsenderland() {
        return avsenderland;
    }

    public void setAvsenderland(Landkoder avsenderland) {
        this.avsenderland = avsenderland;
    }

    public Landkoder getLovvalgsland() {
        return lovvalgsland;
    }

    public void setLovvalgsland(Landkoder lovvalgsland) {
        this.lovvalgsland = lovvalgsland;
    }
}
