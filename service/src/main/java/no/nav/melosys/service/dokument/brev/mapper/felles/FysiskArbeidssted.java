package no.nav.melosys.service.dokument.brev.mapper.felles;

import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Yrkesgrupper;

public class FysiskArbeidssted extends Arbeidssted {
    public final StrukturertAdresse adresse;

    public FysiskArbeidssted(String navn, String idnummer, StrukturertAdresse adresse) {
        super(navn, idnummer, adresse.landkode);
        this.adresse = adresse;
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
