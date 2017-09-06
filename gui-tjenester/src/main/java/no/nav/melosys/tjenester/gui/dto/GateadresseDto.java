package no.nav.melosys.tjenester.gui.dto;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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
        if (this == o) {return true;}
        if (!(o instanceof GateadresseDto)) {return false;}

        GateadresseDto that = (GateadresseDto) o;

        return new EqualsBuilder().append(gatenavn, that.gatenavn).append(gatenummer, that.gatenummer).append(husnummer, that.husnummer).append(husbokstav, that.husbokstav).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(gatenavn).append(gatenummer).append(husnummer).append(husbokstav).toHashCode();
    }
}