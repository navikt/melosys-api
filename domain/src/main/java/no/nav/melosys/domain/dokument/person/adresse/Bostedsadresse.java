package no.nav.melosys.domain.dokument.person.adresse;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonView;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.DokumentView;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.util.LandkoderUtils;
import org.apache.commons.lang3.StringUtils;

public class Bostedsadresse {
    private Gateadresse gateadresse;
    private String tilleggsadresse;
    private String tilleggsadresseType;
    private String postnr;
    private String poststed;
    private Land land;

    public Bostedsadresse() {
        this.gateadresse = new Gateadresse();
        this.land = new Land();
    }

    public Gateadresse getGateadresse() {
        return gateadresse;
    }

    public void setGateadresse(Gateadresse gateadresse) {
        this.gateadresse = gateadresse;
    }

    @JsonView(DokumentView.Database.class)
    public String getTilleggsadresse() {
        return tilleggsadresse;
    }

    public void setTilleggsadresse(String tilleggsadresse) {
        this.tilleggsadresse = tilleggsadresse;
    }

    @JsonView(DokumentView.Database.class)
    public String getTilleggsadresseType() {
        return tilleggsadresseType;
    }

    public void setTilleggsadresseType(String tilleggsadresseType) {
        this.tilleggsadresseType = tilleggsadresseType;
    }

    public String getPostnr() {
        return postnr;
    }

    public void setPostnr(String postnr) {
        this.postnr = postnr;
    }

    public String getPoststed() {
        return poststed;
    }

    public void setPoststed(String poststed) {
        this.poststed = poststed;
    }

    public Land getLand() {
        return land;
    }

    public void setLand(Land land) {
        this.land = land;
    }

    public boolean erTom() {
        return (gateadresse == null || gateadresse.erTom())
            && StringUtils.isEmpty(postnr)
            && StringUtils.isEmpty(poststed)
            && StringUtils.isEmpty(land.getKode());
    }

    public StrukturertAdresse tilStrukturertAdresse() {
        StrukturertAdresse adresse = new StrukturertAdresse();
        if (gateadresse != null) {
            adresse.setGatenavn(gateadresse.getGatenavn());
            adresse.setHusnummerEtasjeLeilighet(Objects.toString(gateadresse.getHusnummer(), ""));
            adresse.setHusnummerEtasjeLeilighet(
                    adresse.getHusnummerEtasjeLeilighet() + Objects.toString(gateadresse.getHusbokstav(), ""));
        }

        adresse.setPostnummer(getPostnr());
        adresse.setPoststed(getPoststed());
        if (StringUtils.isNotEmpty(getLand().getKode())) {
            adresse.setLandkode(LandkoderUtils.tilIso2(getLand().getKode()));
        }

        return adresse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Bostedsadresse that = (Bostedsadresse) o;
        return Objects.equals(gateadresse, that.gateadresse) && Objects.equals(tilleggsadresse,
            that.tilleggsadresse) && Objects.equals(tilleggsadresseType, that.tilleggsadresseType) && Objects.equals(
            postnr, that.postnr) && Objects.equals(poststed, that.poststed) && Objects.equals(land, that.land);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gateadresse, tilleggsadresse, tilleggsadresseType, postnr, poststed, land);
    }
}
