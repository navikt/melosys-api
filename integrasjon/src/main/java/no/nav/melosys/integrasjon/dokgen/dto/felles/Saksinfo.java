package no.nav.melosys.integrasjon.dokgen.dto.felles;

public class Saksinfo {
    private final String saksnummer;

    public Saksinfo(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public String getSaksnummer() {
        return saksnummer;
    }
}
