package no.nav.melosys.tjenester.gui.dto;

import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bostedsadresse;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.StedsadresseNorge;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.StrukturertAdresse;

public class BostedsadresseDto {

    private GateadresseDto gateadresse;

    private String postnr;

    private String poststed;

    private String land;

    public static BostedsadresseDto tilDto(Bostedsadresse bostedsadresse) {
        BostedsadresseDto dto = new BostedsadresseDto();

        StrukturertAdresse strukturertAdresse =
                bostedsadresse.getStrukturertAdresse();

        if (strukturertAdresse instanceof StedsadresseNorge) {
            StedsadresseNorge adresseNorge = ((StedsadresseNorge) strukturertAdresse);
            String postNr = adresseNorge.getPoststed().getValue();
            dto.setPostnr(postNr);

            //TODO Kodeverk
            String postSted = "Kodeverk for " + postNr;
            dto.setPoststed(postSted);
        }

        if (strukturertAdresse instanceof Gateadresse) {
            Gateadresse gateadresse = (Gateadresse) strukturertAdresse;
            GateadresseDto gateDto = GateadresseDto.tilDto(gateadresse);
            dto.setGateadresse(gateDto);
        }

        return dto;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BostedsadresseDto)) return false;

        BostedsadresseDto that = (BostedsadresseDto) o;

        if (gateadresse != null ? !gateadresse.equals(that.gateadresse) : that.gateadresse != null) return false;
        if (postnr != null ? !postnr.equals(that.postnr) : that.postnr != null) return false;
        if (poststed != null ? !poststed.equals(that.poststed) : that.poststed != null) return false;
        return land != null ? land.equals(that.land) : that.land == null;
    }

    @Override
    public int hashCode() {
        int result = gateadresse != null ? gateadresse.hashCode() : 0;
        result = 31 * result + (postnr != null ? postnr.hashCode() : 0);
        result = 31 * result + (poststed != null ? poststed.hashCode() : 0);
        result = 31 * result + (land != null ? land.hashCode() : 0);
        return result;
    }
}
