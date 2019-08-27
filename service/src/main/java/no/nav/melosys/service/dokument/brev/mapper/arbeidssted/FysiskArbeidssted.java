package no.nav.melosys.service.dokument.brev.mapper.arbeidssted;

import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;

public class FysiskArbeidssted extends AbstractArbeidssted {
    public final StrukturertAdresse adresse;

    public FysiskArbeidssted(String navn, String idnummer, StrukturertAdresse adresse) {
        super(navn, idnummer, adresse.landkode);
        this.adresse = adresse;
    }

    public StrukturertAdresse getAdresse() {
        return adresse;
    }

    @Override
    public boolean erFysisk() {
        return true;
    }

    @Override
    public Yrkesgrupper getYrkesgruppe() {
        return Yrkesgrupper.ORDINAER;
    }
}
