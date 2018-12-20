package no.nav.melosys.tjenester.gui.dto.kodeverk;

import java.util.List;

import no.nav.melosys.service.kodeverk.KodeDto;

public class KodeverkDto {
    private BegrunnelserDto begrunnelser = new BegrunnelserDto();
    private BehandlingerDto behandlinger = new BehandlingerDto();
    private BrevDto brev = new BrevDto();
    private LovvalgsBestemmelserDto lovvalgsbestemmelser = new LovvalgsBestemmelserDto();
    private YrkerDto yrker = new YrkerDto();
    private List<KodeDto> aktoerroller;
    private List<KodeDto> dokumenttitler;
    private List<KodeDto> fartsomrader;
    private List<KodeDto> finansiering;
    private List<KodeDto> landkoder;
    private List<KodeDto> medlemskapstyper;
    private List<KodeDto> mottaksretning;
    private List<KodeDto> oppgavetyper;
    private List<KodeDto> representerer;
    private List<KodeDto> saksstatuser;
    private List<KodeDto> sakstyper;
    private List<KodeDto> trygdedekninger;

    public BegrunnelserDto getBegrunnelser() {
        return begrunnelser;
    }

    public void setBegrunnelser(BegrunnelserDto begrunnelser) {
        this.begrunnelser = begrunnelser;
    }

    public BehandlingerDto getBehandlinger() {
        return behandlinger;
    }

    public void setBehandlinger(BehandlingerDto behandlinger) {
        this.behandlinger = behandlinger;
    }

    public BrevDto getBrev() {
        return brev;
    }

    public void setBrev(BrevDto brev) {
        this.brev = brev;
    }

    public LovvalgsBestemmelserDto getLovvalgsbestemmelser() {
        return lovvalgsbestemmelser;
    }

    public void setLovvalgsbestemmelser(LovvalgsBestemmelserDto lovvalgsbestemmelser) { this.lovvalgsbestemmelser = lovvalgsbestemmelser; }

    public YrkerDto getYrker() {
        return yrker;
    }

    public void setYrker(YrkerDto yrker) {
        this.yrker = yrker;
    }

    public List<KodeDto> getAktoerroller() {
        return aktoerroller;
    }

    public void setAktoerroller(List<KodeDto> aktoerroller) {
        this.aktoerroller = aktoerroller;
    }

    public List<KodeDto> getDokumenttitler() {
        return dokumenttitler;
    }

    public void setDokumenttitler(List<KodeDto> dokumenttitler) {
        this.dokumenttitler = dokumenttitler;
    }

    public List<KodeDto> getFartsomrader() {
        return fartsomrader;
    }

    public void setFartsomrader(List<KodeDto> fartsomrader) {
        this.fartsomrader = fartsomrader;
    }

    public List<KodeDto> getFinansiering() { return finansiering; }

    public void setFinansiering(List<KodeDto> finansiering) { this.finansiering = finansiering; }

    public List<KodeDto> getLandkoder() {
        return landkoder;
    }

    public void setLandkoder(List<KodeDto> landkoder) {
        this.landkoder = landkoder;
    }

    public List<KodeDto> getMedlemskapstyper() {
        return medlemskapstyper;
    }

    public void setMedlemskapstyper(List<KodeDto> medlemskapstyper) {
        this.medlemskapstyper = medlemskapstyper;
    }

    public List<KodeDto> getMottaksretning() {
        return mottaksretning;
    }

    public void setMottaksretning(List<KodeDto> mottaksretning) {
        this.mottaksretning = mottaksretning;
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

    public List<KodeDto> getSaksstatuser() {
        return saksstatuser;
    }

    public void setSaksstatuser(List<KodeDto> saksstatuser) {
        this.saksstatuser = saksstatuser;
    }

    public List<KodeDto> getSakstyper() {
        return sakstyper;
    }

    public void setSakstyper(List<KodeDto> sakstyper) {
        this.sakstyper = sakstyper;
    }

    public List<KodeDto> getTrygdedekninger() {
        return trygdedekninger;
    }

    public void setTrygdedekninger(List<KodeDto> trygdedekninger) {
        this.trygdedekninger = trygdedekninger;
    }
}
