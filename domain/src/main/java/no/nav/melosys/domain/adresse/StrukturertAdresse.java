package no.nav.melosys.domain.adresse;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.kodeverk.Landkoder;
import org.apache.commons.lang3.StringUtils;

import static no.nav.melosys.domain.adresse.Adresse.sammenslå;

public class StrukturertAdresse implements Adresse {
    public String gatenavn;
    public String husnummer; // Sammensatt av husnummer og husbokstav
    public String postboks;
    public String tillegsnavn;
    public String postnummer;
    public String poststed;
    public String region;
    public String landkode;

    public StrukturertAdresse() {
    }

    public StrukturertAdresse(String gatenavn,
                              String husnummer,
                              String postnummer,
                              String poststed,
                              String region,
                              String landkode) {
        this.gatenavn = gatenavn;
        this.husnummer = husnummer;
        this.region = region;
        this.postnummer = postnummer;
        this.poststed = poststed;
        this.landkode = landkode;
    }

    public StrukturertAdresse(String gatenavn,
                              String husnummer,
                              String tillegsnavn,
                              String postboks,
                              String postnummer,
                              String poststed,
                              String region,
                              String landkode) {
        this.gatenavn = gatenavn;
        this.husnummer = husnummer;
        this.tillegsnavn = tillegsnavn;
        this.postboks = postboks;
        this.region = region;
        this.postnummer = postnummer;
        this.poststed = poststed;
        this.landkode = landkode;
    }

    @Override
    public boolean erTom() {
        return StringUtils.isAllEmpty(gatenavn, husnummer, tillegsnavn, postboks, postnummer, poststed, region,
            landkode);
    }

    @Override
    public String getLandkode() {
        return landkode;
    }

    @Override
    public String toString() {
        return Stream.of(sammenslå(gatenavn, husnummer),
                tillegsnavn, postboks, postnummer, poststed, region,
                Landkoder.valueOf(landkode).getBeskrivelse())
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(", "));
    }
}
