package no.nav.melosys.integrasjon.gsak.dto;

public class OppgaveDTO {

    private String oppgaveID;

    public OppgaveDTO(String oppgaveID) {
        this.oppgaveID = oppgaveID;
    }

    public String getOppgaveID() {
        return oppgaveID;
    }

    public void setOppgaveID(String oppgaveID) {
        this.oppgaveID = oppgaveID;
    }

}
