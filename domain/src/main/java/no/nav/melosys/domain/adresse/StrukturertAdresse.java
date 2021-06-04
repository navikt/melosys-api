package no.nav.melosys.domain.adresse;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.kodeverk.Landkoder;
import org.apache.commons.lang3.StringUtils;

import static no.nav.melosys.domain.adresse.Adresse.sammenslå;

public class StrukturertAdresse implements Adresse {
    private String tillegsnavn;
    private String gatenavn;
    private String husnummerEtasjeLeilighet;
    private String postboks;
    private String postnummer;
    private String poststed;
    private String region;
    private String landkode;

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

    public String getTillegsnavn() {
        return tillegsnavn;
    }

    public void setTillegsnavn(String tillegsnavn) {
        this.tillegsnavn = tillegsnavn;
    }

    public String getGatenavn() {
        return gatenavn;
    }

    public void setGatenavn(String gatenavn) {
        this.gatenavn = gatenavn;
    }

    public String getHusnummerEtasjeLeilighet() {
        return husnummerEtasjeLeilighet;
    }

    public void setHusnummerEtasjeLeilighet(String husnummerEtasjeLeilighet) {
        this.husnummerEtasjeLeilighet = husnummerEtasjeLeilighet;
    }

    public String getPostboks() {
        return postboks;
    }

    public void setPostboks(String postboks) {
        this.postboks = postboks;
    }

    public String getPostnummer() {
        return postnummer;
    }

    public void setPostnummer(String postnummer) {
        this.postnummer = postnummer;
    }

    public String getPoststed() {
        return poststed;
    }

    public void setPoststed(String poststed) {
        this.poststed = poststed;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setLandkode(String landkode) {
        this.landkode = landkode;
    }
}
