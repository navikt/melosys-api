package no.nav.melosys.domain.adresse;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.kodeverk.Landkoder;
import org.apache.commons.lang3.StringUtils;

import static no.nav.melosys.domain.adresse.Adresse.sammenslå;

public class StrukturertAdresse implements Adresse {
    public String tillegsnavn;
    public String gatenavn;
    public String husnummerEtasjeLeilighet;
    public String postboks;
    public String postnummer;
    public String poststed;
    public String region;
    public String landkode;

    public StrukturertAdresse() {
    }

    public StrukturertAdresse(String gatenavn,
                              String husnummerEtasjeLeilighet,
                              String postnummer,
                              String poststed,
                              String region,
                              String landkode) {
        this.gatenavn = gatenavn;
        this.husnummerEtasjeLeilighet = husnummerEtasjeLeilighet;
        this.region = region;
        this.postnummer = postnummer;
        this.poststed = poststed;
        this.landkode = landkode;
    }

    public StrukturertAdresse(String tillegsnavn, String gatenavn, String husnummerEtasjeLeilighet, String postboks,
                              String postnummer, String poststed, String region, String landkode) {
        this.gatenavn = gatenavn;
        this.husnummerEtasjeLeilighet = husnummerEtasjeLeilighet;
        this.tillegsnavn = tillegsnavn;
        this.postboks = postboks;
        this.region = region;
        this.postnummer = postnummer;
        this.poststed = poststed;
        this.landkode = landkode;
    }

    @Override
    public boolean erTom() {
        return StringUtils.isAllEmpty(tillegsnavn, gatenavn, husnummerEtasjeLeilighet, postboks, postnummer, poststed,
            region, landkode);
    }

    @Override
    public String getLandkode() {
        return landkode;
    }

    @Override
    public String toString() {
        return Stream.of(tillegsnavn, sammenslå(gatenavn, husnummerEtasjeLeilighet),
                postboks, postnummer, poststed, region,
                Landkoder.valueOf(landkode).getBeskrivelse())
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(", "));
    }
}
