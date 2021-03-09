package no.nav.melosys.domain.folketrygden;

public class ValgtRepresentant {
    private final String representantnummer;
    private final boolean selvbetalende;
    private final String orgnr;
    private final String kontaktperson;

    public ValgtRepresentant(String representantnummer, boolean selvbetalende, String orgnr, String kontaktperson) {
        this.representantnummer = representantnummer;
        this.selvbetalende = selvbetalende;
        this.orgnr = orgnr;
        this.kontaktperson = kontaktperson;
    }

    public String getRepresentantnummer() {
        return representantnummer;
    }

    public boolean isSelvbetalende() {
        return selvbetalende;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public String getKontaktperson() {
        return kontaktperson;
    }
}
