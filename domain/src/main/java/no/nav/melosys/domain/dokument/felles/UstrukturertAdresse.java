package no.nav.melosys.domain.dokument.felles;

import java.util.ArrayList;
import java.util.List;

public class UstrukturertAdresse {

    public UstrukturertAdresse() {}

    public UstrukturertAdresse(StrukturertAdresse adresse) {
        adresselinjer.add(adresse.gatenavn);
        adresselinjer.add(adresse.husnummer);
        adresselinjer.add(adresse.postnummer);
        adresselinjer.add(adresse.poststed);
        adresselinjer.add(adresse.region);
        landKode = adresse.landKode;
    }

    public List<String> adresselinjer = new ArrayList<>();

    public String landKode;
}
