package no.nav.melosys.domain.adresse;

import no.nav.melosys.domain.kodeverk.Land_iso2;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Stream;

import static no.nav.melosys.domain.adresse.Adresse.sammenslå;

public class StrukturertAdresse implements Adresse {
    private String tilleggsnavn;
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

    public StrukturertAdresse(String tilleggsnavn, String gatenavn, String husnummerEtasjeLeilighet, String postboks,
                              String postnummer, String poststed, String region, String landkode) {
        this.gatenavn = gatenavn;
        this.husnummerEtasjeLeilighet = husnummerEtasjeLeilighet;
        this.tilleggsnavn = tilleggsnavn;
        this.postboks = postboks;
        this.region = region;
        this.postnummer = postnummer;
        this.poststed = poststed;
        this.landkode = landkode;
    }

    @Override
    public String getLandkode() {
        return landkode;
    }

    @Override
    public String toString() {
        return String.join(", ", toList());
    }

    public String getTilleggsnavn() {
        return tilleggsnavn;
    }

    public void setTilleggsnavn(String tilleggsnavn) {
        this.tilleggsnavn = tilleggsnavn;
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

    @Override
    public List<String> toList() {
        return Stream.of(tilleggsnavn, sammenslå(gatenavn, husnummerEtasjeLeilighet),
                        postboks, postnummer, poststed, region,
                        landkode != null ? Land_iso2.valueOf(landkode).getBeskrivelse() : "")
                .filter(StringUtils::isNotEmpty)
                .toList();
    }

    @Override
    public boolean erTom() {
        return StringUtils.isAllEmpty(tilleggsnavn, gatenavn, husnummerEtasjeLeilighet, postboks, postnummer, poststed,
                region, landkode);
    }

    public boolean erGyldig() {
        if (erTom()) return false;
        if (StringUtils.isBlank(postnummer)) return false;
        return !StringUtils.isBlank(gatenavn) || !StringUtils.isBlank(tilleggsnavn);
    }
}
