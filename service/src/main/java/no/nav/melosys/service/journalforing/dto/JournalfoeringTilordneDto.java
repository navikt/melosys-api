package no.nav.melosys.service.journalforing.dto;

public class JournalfoeringTilordneDto extends JournalfoeringDto {
    private String saksnummer; // Melosys saksnummer
    private boolean ingenVurdering;

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public boolean isIngenVurdering() {
        return ingenVurdering;
    }

    public void setIngenVurdering(boolean ingenVurdering) {
        this.ingenVurdering = ingenVurdering;
    }
}
