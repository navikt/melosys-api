package no.nav.melosys.service.oppgave.dto;

public class BehandlingDto {
    private KodeverdiDto type;
    private KodeverdiDto status;

    public KodeverdiDto getType() {
        return type;
    }

    public void setType(KodeverdiDto type) {
        this.type = type;
    }

    public KodeverdiDto getStatus() {
        return status;
    }

    public void setStatus(KodeverdiDto status) {
        this.status = status;
    }
}
