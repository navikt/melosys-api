package no.nav.melosys.service.dokument.brev.mapper.felles;

import no.nav.melosys.domain.dokument.felles.Adresse;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;

public class Virksomhet {

    public final String navn;
    public final String orgnr;
    public final Adresse adresse;
    private boolean selvstendigForetak;

    public Virksomhet(ForetakUtland foretak) {
        this.navn = foretak.navn;
        this.orgnr = foretak.orgnr;
        this.adresse = foretak.adresse;
    }

    public Virksomhet(String navn, String orgnr, Adresse adresse) {
        this.navn = navn;
        this.orgnr = orgnr;
        this.adresse = adresse;
    }

    public boolean isSelvstendigForetak() {
        return selvstendigForetak;
    }

    public void setSelvstendigForetak(boolean selvstendigForetak) {
        this.selvstendigForetak = selvstendigForetak;
    }
}