package no.nav.melosys.tjenester.gui.dto;

public class AdresseDto {

    private GateadresseDto gateadresse;

    private String postnr;

    private String poststed;

    private String land;

    public GateadresseDto getGateadresse() {
        return gateadresse;
    }

    public void setGateadresse(GateadresseDto gateadresse) {
        this.gateadresse = gateadresse;
    }

    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
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

}
