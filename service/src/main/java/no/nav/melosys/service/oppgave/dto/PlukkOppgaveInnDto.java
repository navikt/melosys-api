package no.nav.melosys.service.oppgave.dto;

import java.util.List;

public class PlukkOppgaveInnDto {

    private String oppgavetype;
    private String fagomrade;
    private List<String> sakstyper;
    private List<String> behandlingstyper;
    private List<String> behandlingstema;

    public String getOppgavetype() {
        return oppgavetype;
    }

    public void setOppgavetype(String oppgavetype) {
        this.oppgavetype = oppgavetype;
    }

    public String getFagomrade() {
        return fagomrade;
    }

    public void setFagomrade(String fagomrade) {
        this.fagomrade = fagomrade;
    }

    public List<String> getSakstyper() {
        return sakstyper;
    }

    public void setSakstyper(List<String> sakstyper) {
        this.sakstyper = sakstyper;
    }

    public List<String> getBehandlingstyper() {
        return behandlingstyper;
    }

    public void setBehandlingstyper(List<String> behandlingstyper) {
        this.behandlingstyper = behandlingstyper;
    }
}
