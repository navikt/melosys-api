package no.nav.melosys.service.journalforing.dto;

public class JournalfoeringTilordneDto extends JournalfoeringDto {
    private String saksnummer; // Melosys saksnummer

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }
}
