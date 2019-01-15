package no.nav.melosys.service.dokument.brev.mapper.felles;

import no.nav.melosys.domain.kodeverk.Yrkesgrupper;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;

public class Arbeidssted {
    public Arbeidssted(String navn, String landKode, Yrkesgrupper type) {
        this.navn = navn;
        this.landKode = landKode;
        this.yrkesgruppe = type;
        this.adresse = null;
    }

    public Arbeidssted(String navn, StrukturertAdresse adresse) {
        this.navn = navn;
        this.landKode = adresse.landKode;
        this.adresse = adresse;
        this.yrkesgruppe = Yrkesgrupper.ORDINAER;
    }

    public Arbeidssted(String navn, String landKode) {
        this.navn = navn;
        this.landKode = landKode;
        this.adresse = null;
        this.yrkesgruppe = Yrkesgrupper.ORDINAER;
    }

    public boolean erFysisk() {
        return adresse != null;
    }

    public final String navn;
    public final String landKode;
    public final Yrkesgrupper yrkesgruppe;
    public final StrukturertAdresse adresse;
}
