package no.nav.melosys.integrasjon.joark.journalpostapi.dto;

public class Sak {
    private static final String SAKSTYPE = "FAGSAK";
    private static final String FAGSAKSYSTEM = "FS38";
    private String fagsakId;

    public Sak(String saksnummer) {
        this.fagsakId = saksnummer;
    }

    public Sak() {
    }

    public String getFagsakId() {
        return fagsakId;
    }

    public String getSakstype() {
        return SAKSTYPE;
    }

    public String getFagsaksystem() {
        return FAGSAKSYSTEM;
    }
}
