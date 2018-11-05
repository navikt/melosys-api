package no.nav.melosys.tjenester.gui.dto;

import java.util.List;

import no.nav.melosys.service.kodeverk.KodeDto;

public class KodeverkDto {
    private BegrunnelserDto begrunnelser = new BegrunnelserDto();
    private LovvalgsBestemmelserDto lovvalgsbestemmelser = new LovvalgsBestemmelserDto();
    private List<KodeDto> aktoerroller;
    private List<KodeDto> behandlingstyper;
    private List<KodeDto> behandlingsstatus;
    private List<KodeDto> dokumenttitler;
    private List<KodeDto> dokumenttyper;
    private List<KodeDto> finansiering;
    private List<KodeDto> landkoder;
    private List<KodeDto> lovvalgsunntak;
    private List<KodeDto> oppgavetyper;
    private List<KodeDto> representerer;
    private List<KodeDto> sakstyper;
    private List<KodeDto> vedleggstitler;

    public BegrunnelserDto getBegrunnelser() {
        return begrunnelser;
    }

    public LovvalgsBestemmelserDto getLovvalgsbestemmelser() {
        return lovvalgsbestemmelser;
    }

    public void setBegrunnelser(BegrunnelserDto begrunnelser) {
        this.begrunnelser = begrunnelser;
    }

    public void setLovvalgsbestemmelser(LovvalgsBestemmelserDto lovvalgsbestemmelser) { this.lovvalgsbestemmelser = lovvalgsbestemmelser; }

    public List<KodeDto> getAktoerroller() {
        return aktoerroller;
    }

    public void setAktoerroller(List<KodeDto> aktoerroller) {
        this.aktoerroller = aktoerroller;
    }

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

    public List<KodeDto> getDokumenttitler() {
        return dokumenttitler;
    }

    public void setDokumenttitler(List<KodeDto> dokumenttitler) {
        this.dokumenttitler = dokumenttitler;
    }

    public List<KodeDto> getDokumenttyper() {
        return dokumenttyper;
    }

    public void setDokumenttyper(List<KodeDto> dokumenttyper) {
        this.dokumenttyper = dokumenttyper;
    }

    public List<KodeDto> getFinansiering() { return finansiering; }

    public void setFinansiering(List<KodeDto> finansiering) { this.finansiering = finansiering; }

    public List<KodeDto> getLandkoder() {
        return landkoder;
    }

    public void setLandkoder(List<KodeDto> landkoder) {
        this.landkoder = landkoder;
    }

    public List<KodeDto> getLovvalgsunntak() {
        return lovvalgsunntak;
    }

    public void setLovvalgsunntak(List<KodeDto> lovvalgsunntak) {
        this.lovvalgsunntak = lovvalgsunntak;
    }

    public List<KodeDto> getOppgavetyper() {
        return oppgavetyper;
    }

    public void setOppgavetyper(List<KodeDto> oppgavetyper) {
        this.oppgavetyper = oppgavetyper;
    }

    public List<KodeDto> getRepresenterer() {
        return representerer;
    }

    public void setRepresenterer(List<KodeDto> representerer) {
        this.representerer = representerer;
    }

    public List<KodeDto> getSakstyper() {
        return sakstyper;
    }

    public void setSakstyper(List<KodeDto> sakstyper) {
        this.sakstyper = sakstyper;
    }

    public List<KodeDto> getVedleggstitler() {
        return vedleggstitler;
    }

    public void setVedleggstitler(List<KodeDto> vedleggstitler) {
        this.vedleggstitler = vedleggstitler;
    }
}
