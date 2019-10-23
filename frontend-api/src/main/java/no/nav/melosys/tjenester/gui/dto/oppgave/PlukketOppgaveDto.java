package no.nav.melosys.tjenester.gui.dto.oppgave;

public class PlukketOppgaveDto {

    private String oppgaveID;
    private String behandlingstype;
    private String saksnummer;
    private String journalpostID;
    private Long behandlingID;

    public String getOppgaveID() {
        return oppgaveID;
    }

    public void setOppgaveID(String oppgaveId) {
        this.oppgaveID = oppgaveId;
    }

    public String getBehandlingstype() {
        return behandlingstype;
    }

    public void setBehandlingstype(String behandlingstype) {
        this.behandlingstype = behandlingstype;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public String getJournalpostID() {
        return journalpostID;
    }

    public void setJournalpostID(String journalpostId) {
        this.journalpostID = journalpostId;
    }

    public void setBehandlingID(Long behandlingID) {
        this.behandlingID = behandlingID;
    }

    public Long getBehandlingID() {
        return behandlingID;
    }
}
