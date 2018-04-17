package no.nav.melosys.tjenester.gui.dto;

import java.util.List;

import no.nav.melosys.service.kodeverk.KodeDto;

public class KodeverkDto {
    private List<KodeDto> behandlingstyper;
    private List<KodeDto> behandlingsstatus;
    private List<KodeDto> dokumentkategorier;
    private List<KodeDto> landkoder;
    private List<KodeDto> oppgavetyper;
    private List<KodeDto> sakstyper;

    public List<KodeDto> getBehandlingstyper() {
        return behandlingstyper;
    }

    public void setBehandlingstyper(List<KodeDto> behandlingstyper) {
        this.behandlingstyper = behandlingstyper;
    }

    public List<KodeDto> getBehandlingsstatus() {
        return behandlingsstatus;
    }

    public void setBehandlingsstatus(List<KodeDto> behandlingsstatus) {
        this.behandlingsstatus = behandlingsstatus;
    }

    public List<KodeDto> getDokumentkategorier() {
        return dokumentkategorier;
    }

    public void setDokumentkategorier(List<KodeDto> dokumentkategorier) {
        this.dokumentkategorier = dokumentkategorier;
    }

    public List<KodeDto> getLandkoder() {
        return landkoder;
    }

    public void setLandkoder(List<KodeDto> landkoder) {
        this.landkoder = landkoder;
    }

    public List<KodeDto> getOppgavetyper() {
        return oppgavetyper;
    }

    public void setOppgavetyper(List<KodeDto> oppgavetyper) {
        this.oppgavetyper = oppgavetyper;
    }

    public List<KodeDto> getSakstyper() {
        return sakstyper;
    }

    public void setSakstyper(List<KodeDto> sakstyper) {
        this.sakstyper = sakstyper;
    }
}
