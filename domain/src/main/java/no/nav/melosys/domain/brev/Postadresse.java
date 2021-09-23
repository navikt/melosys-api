package no.nav.melosys.domain.brev;

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
    String landkode
) {
    public List<String> adresselinjer() {
        return asList(
            adresselinje1,
            adresselinje2,
            adresselinje3,
            adresselinje4
        );
    }

    public static Postadresse lagPostadresse(StrukturertAdresse strukturertAdresse) {
        return new Postadresse(
            sammenslå(strukturertAdresse.getGatenavn(), strukturertAdresse.getHusnummerEtasjeLeilighet()),
            strukturertAdresse.getPostboks(),
            strukturertAdresse.getRegion(),
            null,
            strukturertAdresse.getPostnummer(),
            strukturertAdresse.getPoststed(),
            strukturertAdresse.getLandkode());
    }

    public static Postadresse lagPostadresse(SemistrukturertAdresse semistrukturertAdresse) {
        return new Postadresse(semistrukturertAdresse.adresselinje1(),
            semistrukturertAdresse.adresselinje2(),
            semistrukturertAdresse.adresselinje3(),
            null,
            semistrukturertAdresse.postnr(),
            semistrukturertAdresse.poststed(),
            semistrukturertAdresse.landkode());
    }
}
