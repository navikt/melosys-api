package no.nav.melosys.domain.dokument.person;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import no.nav.melosys.domain.dokument.DokumentView;
import no.nav.melosys.domain.dokument.felles.Land;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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

    @JsonIgnore
    public boolean erTom() {
        return gateadresse.erTom() &&
            StringUtils.isEmpty(postnr) &&
            StringUtils.isEmpty(poststed) &&
            StringUtils.isEmpty(land.getKode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (!(o instanceof Bostedsadresse)) {return false;}

        Bostedsadresse that = (Bostedsadresse) o;

        return new EqualsBuilder().append(gateadresse, that.gateadresse).append(postnr, that.postnr).append(poststed, that.poststed).append(land, that.land).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(gateadresse).append(postnr).append(poststed).append(land).toHashCode();
    }
}
