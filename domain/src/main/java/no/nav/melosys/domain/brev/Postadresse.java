package no.nav.melosys.domain.brev;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import no.nav.melosys.domain.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.adresse.StrukturertAdresse;

import static java.util.Arrays.asList;
import static no.nav.melosys.domain.adresse.Adresse.sammenslå;

public record Postadresse(
    String coAdressenavn,
    String adresselinje1,
    String adresselinje2,
    String adresselinje3,
    String adresselinje4,
    String postnr,
    String poststed,
    String landkode,
    String region
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

    public List<String> lagPostadresseListe() {
        return Stream.of(coAdressenavn, adresselinje1, adresselinje2, adresselinje3, adresselinje4, postnr, poststed)
            .filter(Objects::nonNull)
            .toList();
    }

    public static Postadresse lagPostadresse(String coAdressenavn, StrukturertAdresse strukturertAdresse) {
        return new Postadresse(
            coAdressenavn,
            sammenslå(strukturertAdresse.getGatenavn(), strukturertAdresse.getHusnummerEtasjeLeilighet()),
            strukturertAdresse.getPostboks(),
            null,
            null,
            strukturertAdresse.getPostnummer(),
            strukturertAdresse.getPoststed(),
            strukturertAdresse.getLandkode(),
            strukturertAdresse.getRegion());
    }

    public static Postadresse lagPostadresse(String coAdressenavn, SemistrukturertAdresse semistrukturertAdresse) {
        return new Postadresse(
            coAdressenavn,
            semistrukturertAdresse.adresselinje1(),
            semistrukturertAdresse.adresselinje2(),
            semistrukturertAdresse.adresselinje3(),
            null,
            semistrukturertAdresse.postnr(),
            semistrukturertAdresse.poststed(),
            semistrukturertAdresse.landkode(),
            null
        );
    }
}
