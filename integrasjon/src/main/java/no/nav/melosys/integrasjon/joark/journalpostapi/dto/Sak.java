package no.nav.melosys.integrasjon.joark.journalpostapi.dto;

public class Sak {
    private static final String ARKIVSAKSYSTEM = "GSAK";
    private static final String SAKSTYPE = "FAGSAK";
    private String fagsakId;

    public Sak(String saksnummer) {
        this.fagsakId = saksnummer;
    }

    public Sak() {
    }

    public static SakBuilder builder() {
        return new SakBuilder();
    }

    public String getFagsakId() {
        return fagsakId;
    }

    public String getSakstype() {
        return SAKSTYPE;
    }

    public String getArkivsaksystem() {
        return ARKIVSAKSYSTEM;
    }

    public static class SakBuilder {
        private String fagsakId;

        SakBuilder() {
        }

        public Sak.SakBuilder fagsakId(String saksnummer) {
            this.fagsakId = saksnummer;
            return this;
        }

        public Sak build() {
            return new Sak(fagsakId);
        }
    }
}
