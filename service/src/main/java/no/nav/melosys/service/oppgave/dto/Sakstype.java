package no.nav.melosys.service.oppgave.dto;

public class Sakstype {
    private KodeverdiDto fagSakType;
    private KodeverdiDto behandling;
    private KodeverdiDto status;

    public KodeverdiDto getFagSakType() {
        return fagSakType;
    }

    public void setFagSakType(KodeverdiDto fagSakType) {
        this.fagSakType = fagSakType;
    }

    public KodeverdiDto getBehandling() {
        return behandling;
    }

    public void setBehandlingDto(KodeverdiDto behandling) {
        this.behandling = behandling;
    }

    public KodeverdiDto getStatus() {
        return status;
    }

    public void setStatus(KodeverdiDto status) {
        this.status = status;
    }


}
