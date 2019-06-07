package no.nav.melosys.domain.dokument.felles;

import java.util.ArrayList;
import java.util.List;

public class UstrukturertAdresse extends Adresse {

    public List<String> adresselinjer = new ArrayList<>();

    public UstrukturertAdresse() { }

    public UstrukturertAdresse(no.nav.melosys.domain.dokument.person.UstrukturertAdresse adresse) {
        if (adresse.adresselinje1 != null) {
            adresselinjer.add(adresse.adresselinje1);
        }
        if (adresse.adresselinje2 != null) {
            adresselinjer.add(adresse.adresselinje2);
        }
        if (adresse.adresselinje3 != null) {
            adresselinjer.add(adresse.adresselinje3);
        }
        if (adresse.adresselinje4 != null) {
            adresselinjer.add(adresse.adresselinje4);
        }
        if (adresse.land != null) {
            landkode = adresse.land.getKode();
        }
    }
}
