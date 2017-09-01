package no.nav.melosys.tjenester.gui.dto;

import no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse;

public class GateadresseDto {

    private String gatenavn;

    private Integer gatenummer;

    private Integer husnummer;

    private String husbokstav;

    public static GateadresseDto tilDto(Gateadresse gateadresse) {
        GateadresseDto gateDto = new GateadresseDto();
        gateDto.setGatenavn(gateadresse.getGatenavn() != null ? gateadresse.getGatenavn() : null);
        gateDto.setGatenummer(gateadresse.getGatenummer() != null ? gateadresse.getGatenummer() : null);
        gateDto.setHusnummer(gateadresse.getHusnummer() != null ? gateadresse.getHusnummer() : null);
        gateDto.setHusbokstav(gateadresse.getHusbokstav() != null ? gateadresse.getHusbokstav() : null);

        return gateDto;
    }

    public String getGatenavn() {
        return gatenavn;
    }

    public void setGatenavn(String gatenavn) {
        this.gatenavn = gatenavn;
    }

    public Integer getGatenummer() {
        return gatenummer;
    }

    public void setGatenummer(Integer gatenummer) {
        this.gatenummer = gatenummer;
    }

    public Integer getHusnummer() {
        return husnummer;
    }

    public void setHusnummer(Integer husnummer) {
        this.husnummer = husnummer;
    }

    public String getHusbokstav() {
        return husbokstav;
    }

    public void setHusbokstav(String husbokstav) {
        this.husbokstav = husbokstav;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GateadresseDto)) return false;

        GateadresseDto that = (GateadresseDto) o;

        if (gatenavn != null ? !gatenavn.equals(that.gatenavn) : that.gatenavn != null) return false;
        if (gatenummer != null ? !gatenummer.equals(that.gatenummer) : that.gatenummer != null) return false;
        if (husnummer != null ? !husnummer.equals(that.husnummer) : that.husnummer != null) return false;
        return husbokstav != null ? husbokstav.equals(that.husbokstav) : that.husbokstav == null;
    }

    @Override
    public int hashCode() {
        int result = gatenavn != null ? gatenavn.hashCode() : 0;
        result = 31 * result + (gatenummer != null ? gatenummer.hashCode() : 0);
        result = 31 * result + (husnummer != null ? husnummer.hashCode() : 0);
        result = 31 * result + (husbokstav != null ? husbokstav.hashCode() : 0);
        return result;
    }
}