package no.nav.melosys.domain.dokument.felles;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresseUtland;

public class UstrukturertAdresse extends Adresse {

    public final List<String> adresselinjer = new ArrayList<>();

    public UstrukturertAdresse(MidlertidigPostadresseUtland adresse) {
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

    public UstrukturertAdresse(SemistrukturertAdresse sAdresse) {
        if (sAdresse.getAdresselinje1() != null) {
            adresselinjer.add(sAdresse.getAdresselinje1());
        }
        if (sAdresse.getAdresselinje2() != null) {
            adresselinjer.add(sAdresse.getAdresselinje2());
        }
        if (sAdresse.getAdresselinje3() != null) {
            adresselinjer.add(sAdresse.getAdresselinje3());
        }
        landkode = sAdresse.getLandkode();

        if (sAdresse.erUtenlandsk()) {
            adresselinjer.add(sAdresse.getPoststedUtland());
        } else {
            String _poststed = sAdresse.getPoststed() == null ? "" : " " + sAdresse.getPoststed();
            adresselinjer.add(sAdresse.getPostnr() + _poststed);
        }
    }

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
