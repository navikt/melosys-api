package no.nav.melosys.tjenester.gui.dto;

public class PersonDto {
    private String personnummer;
    private String sammensattNavn;

    public PersonDto(String personnummer, String sammensattNavn) {
        this.personnummer = personnummer;
        this.sammensattNavn = sammensattNavn;
    }

    public String getPersonnummer() {
        return personnummer;
    }

    public void setPersonnummer(String personnummer) {
        this.personnummer = personnummer;
    }

    public String getSammensattNavn() {
        return sammensattNavn;
    }

    public void setSammensattNavn(String sammensattNavn) {
        this.sammensattNavn = sammensattNavn;
    }
}
