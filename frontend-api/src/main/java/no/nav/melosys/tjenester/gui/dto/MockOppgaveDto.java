package no.nav.melosys.tjenester.gui.dto;

public class MockOppgaveDto {
    private String ansvarligID;
    private String fnr;
    private String journalpostID;
    private String oppgavetype;
    private String saksnummer;

    public String getAnsvarligID() {
        return ansvarligID;
    }

    public void setAnsvarligID(String ansvarligID) {
        this.ansvarligID = ansvarligID;
    }

    public String getFnr() {
        return fnr;
    }

    public void setFnr(String fnr) {
        this.fnr = fnr;
    }

    public String getJournalpostID() {
        return journalpostID;
    }

    public void setJournalpostID(String journalpostID) {
        this.journalpostID = journalpostID;
    }

    public String getOppgavetype() {
        return oppgavetype;
    }

    public void setOppgavetype(String oppgavetype) {
        this.oppgavetype = oppgavetype;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }
}
