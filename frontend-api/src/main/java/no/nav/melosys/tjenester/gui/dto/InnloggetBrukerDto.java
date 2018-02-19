package no.nav.melosys.tjenester.gui.dto;

public class InnloggetBrukerDto {

    private String brukernavn;

    private String navn;

    public InnloggetBrukerDto(String brukernavn, String navn) {
        this.brukernavn = brukernavn;
        this.navn = navn;
    }

    public String getBrukernavn() {
        return brukernavn;
    }

    public String getNavn() {
        return navn;
    }
}
