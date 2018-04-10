package no.nav.melosys.tjenester.gui.dto;

import java.util.List;

public class KodeverkDto {
    private List<KodeverdiDto> behandlingstyper;
    private List<KodeverdiDto> behandlingsstatus;
    private List<KodeverdiDto> landkoder;
    private List<KodeverdiDto> oppgavetyper;
    private List<KodeverdiDto> sakstyper;

    public List<KodeverdiDto> getBehandlingstyper() {
        return behandlingstyper;
    }

    public void setBehandlingstyper(List<KodeverdiDto> behandlingstyper) {
        this.behandlingstyper = behandlingstyper;
    }

    public List<KodeverdiDto> getBehandlingsstatus() {
        return behandlingsstatus;
    }

    public void setBehandlingsstatus(List<KodeverdiDto> behandlingsstatus) {
        this.behandlingsstatus = behandlingsstatus;
    }

    public List<KodeverdiDto> getLandkoder() {
        return landkoder;
    }

    public void setLandkoder(List<KodeverdiDto> landkoder) {
        this.landkoder = landkoder;
    }

    public List<KodeverdiDto> getOppgavetyper() {
        return oppgavetyper;
    }

    public void setOppgavetyper(List<KodeverdiDto> oppgavetyper) {
        this.oppgavetyper = oppgavetyper;
    }

    public List<KodeverdiDto> getSakstyper() {
        return sakstyper;
    }

    public void setSakstyper(List<KodeverdiDto> sakstyper) {
        this.sakstyper = sakstyper;
    }
}
