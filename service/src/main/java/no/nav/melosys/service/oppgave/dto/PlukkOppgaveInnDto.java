package no.nav.melosys.service.oppgave.dto;

import java.util.List;

public class PlukkOppgaveInnDto {

    private String oppgavetype;
    private List<String> sakstyper;
    private List<String> behandlingstyper;

    public String getOppgavetype() {
        return oppgavetype;
    }

    public void setOppgavetype(String oppgavetype) {
        this.oppgavetype = oppgavetype;
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
