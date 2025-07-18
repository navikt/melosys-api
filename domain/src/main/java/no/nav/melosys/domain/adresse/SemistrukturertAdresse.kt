package no.nav.melosys.domain.adresse;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public record SemistrukturertAdresse(
    String adresselinje1,
    String adresselinje2,
    String adresselinje3,
    String adresselinje4,
    String postnr,
    String poststed,
    String landkode
) implements Adresse {
    @Override
    public String getLandkode() {
        return landkode();
    }

    @Override
    public boolean erTom() {
        return StringUtils.isAllEmpty(adresselinje1, adresselinje2, adresselinje3, adresselinje4, postnr, poststed,
            landkode);
    }

    @Override
    public List<String> toList() {
        return tilStrukturertAdresse().toList();
    }

    public StrukturertAdresse tilStrukturertAdresse() {
        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.setGatenavn(
            Adresse.sammenslå(this.adresselinje1(), this.adresselinje2(), this.adresselinje3(), this.adresselinje4()));
        strukturertAdresse.setPostnummer(this.postnr());
        strukturertAdresse.setPoststed(this.poststed());
        strukturertAdresse.setLandkode(this.landkode());
        return strukturertAdresse;
    }
}
