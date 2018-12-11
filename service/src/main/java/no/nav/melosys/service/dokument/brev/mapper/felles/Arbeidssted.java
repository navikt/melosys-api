package no.nav.melosys.service.dokument.brev.mapper.felles;

import no.nav.melosys.domain.YrkesgruppeType;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;

public class Arbeidssted {
    public Arbeidssted(String navn, String landKode, YrkesgruppeType type) {
        this.navn = navn;
        this.landKode = landKode;
        this.yrkesgruppe = type;
        this.adresse = null;
    }

    public Arbeidssted(String navn, StrukturertAdresse adresse) {
        this.navn = navn;
        this.landKode = adresse.landKode;
        this.adresse = adresse;
        this.yrkesgruppe = YrkesgruppeType.ORDINAER;
    }

    public Arbeidssted(String navn, String landKode) {
        this.navn = navn;
        this.landKode = landKode;
        this.adresse = null;
        this.yrkesgruppe = YrkesgruppeType.ORDINAER;
    }

    public final String navn;
    public final String landKode;
    public final YrkesgruppeType yrkesgruppe;
    public final StrukturertAdresse adresse;
}
