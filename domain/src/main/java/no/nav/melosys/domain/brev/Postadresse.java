package no.nav.melosys.domain.brev;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.adresse.StrukturertAdresse;

import static java.util.Arrays.asList;
import static no.nav.melosys.domain.adresse.Adresse.sammenslå;

public record Postadresse(
    String adresselinje1,
    String adresselinje2,
    String adresselinje3,
    String adresselinje4,
    String postnr,
    String poststed,
    String landkode,
    String coAdressenavn
) {
    public List<String> adresselinjer() {
        List<String> adresselinjer = new ArrayList<>();
        if (coAdressenavn != null) {
            adresselinjer.add(coAdressenavn);
        }
        adresselinjer.addAll(asList(
            adresselinje1,
            adresselinje2,
            adresselinje3,
            adresselinje4
        ));
        return adresselinjer;
    }

    public static Postadresse lagPostadresse(StrukturertAdresse strukturertAdresse, String coAdressenavn) {
        return new Postadresse(
            sammenslå(strukturertAdresse.getGatenavn(), strukturertAdresse.getHusnummerEtasjeLeilighet()),
            strukturertAdresse.getPostboks(),
            strukturertAdresse.getRegion(),
            null,
            strukturertAdresse.getPostnummer(),
            strukturertAdresse.getPoststed(),
            strukturertAdresse.getLandkode(),
            coAdressenavn);
    }

    public static Postadresse lagPostadresse(SemistrukturertAdresse semistrukturertAdresse, String coAdressenavn) {
        return new Postadresse(semistrukturertAdresse.adresselinje1(),
            semistrukturertAdresse.adresselinje2(),
            semistrukturertAdresse.adresselinje3(),
            null,
            semistrukturertAdresse.postnr(),
            semistrukturertAdresse.poststed(),
            semistrukturertAdresse.landkode(),
            coAdressenavn);
    }
}
