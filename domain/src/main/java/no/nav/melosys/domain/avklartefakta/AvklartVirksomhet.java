package no.nav.melosys.domain.avklartefakta;

import no.nav.melosys.domain.adresse.Adresse;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
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
        this.yrkesaktivitet = (Boolean.TRUE.equals(foretak.selvstendigNæringsvirksomhet)) ?
            Yrkesaktivitetstyper.SELVSTENDIG : Yrkesaktivitetstyper.LOENNET_ARBEID;
    }

    public AvklartVirksomhet(String navn, String orgnr, Adresse adresse, Yrkesaktivitetstyper yrkesaktivitet) {
        this.navn = navn;
        this.orgnr = orgnr;
        this.adresse = adresse;
        this.adresseErOgsåArbeidssted = false;
        this.yrkesaktivitet = yrkesaktivitet;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public boolean erSelvstendigForetak() {
        return yrkesaktivitet == Yrkesaktivitetstyper.SELVSTENDIG;
    }

    public boolean erArbeidsgiver() {
        return yrkesaktivitet == Yrkesaktivitetstyper.LOENNET_ARBEID;
    }
}
