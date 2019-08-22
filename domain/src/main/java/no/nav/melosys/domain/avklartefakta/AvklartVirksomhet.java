package no.nav.melosys.domain.avklartefakta;

import no.nav.melosys.domain.dokument.felles.Adresse;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;

public class AvklartVirksomhet {

    public final String navn;
    public final String orgnr;
    public final Adresse adresse;
    public final boolean adresseErOgsåArbeidssted;
    public final Yrkesaktivitetstyper yrkesaktivitet;

    public AvklartVirksomhet(ForetakUtland foretak) {
        this.navn = foretak.navn;
        this.orgnr = foretak.orgnr;
        this.adresse = foretak.adresse;
        this.adresseErOgsåArbeidssted = false;
        this.yrkesaktivitet = null;
    }

    public AvklartVirksomhet(String navn, String orgnr, Adresse adresse, Yrkesaktivitetstyper yrkesaktivitet) {
        this.navn = navn;
        this.orgnr = orgnr;
        this.adresse = adresse;
        this.adresseErOgsåArbeidssted = false;
        this.yrkesaktivitet = yrkesaktivitet;
    }

    public boolean isSelvstendigForetak() {
        return yrkesaktivitet == Yrkesaktivitetstyper.SELVSTENDIG;
    }
}