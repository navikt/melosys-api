package no.nav.melosys.integrasjon.joark.journalpostapi.dto;

public class Sak {
    private static final String arkivsaksystem = "GSAK";
    private String arkivsaksnummer;

    public Sak(String arkivsaksnummer) {
        this.arkivsaksnummer = arkivsaksnummer;
    }

    public Sak() {
    }

    public static SakBuilder builder() {
        return new SakBuilder();
    }

    public String getArkivsaksnummer() {
        return this.arkivsaksnummer;
    }

    public String getArkivsaksystem() {
        return this.arkivsaksystem;
    }

    public static class SakBuilder {
        private String arkivsaksnummer;

        SakBuilder() {
        }

        public Sak.SakBuilder arkivsaksnummer(String arkivsaksnummer) {
            this.arkivsaksnummer = arkivsaksnummer;
            return this;
        }

        public Sak build() {
            return new Sak(arkivsaksnummer);
        }
    }
}
