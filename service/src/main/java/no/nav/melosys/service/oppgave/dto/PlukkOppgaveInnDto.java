package no.nav.melosys.service.oppgave.dto;

public class PlukkOppgaveInnDto {

    private String sakstype;
    private String behandlingstype;

    public String getSakstype() {
        return sakstype;
    }

    public void setSakstype(String sakstype) {
        this.sakstype = sakstype;
    }

    public String getBehandlingstype() {
        return behandlingstype;
    }

    public void setBehandlingstype(String behandlingstype) {
        this.behandlingstype = behandlingstype;
    }
}
