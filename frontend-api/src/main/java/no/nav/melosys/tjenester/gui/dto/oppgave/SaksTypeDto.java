package no.nav.melosys.tjenester.gui.dto.oppgave;

public class SaksTypeDto {
    private FagSakTypeDto fagSakTypeDto;
    private BehandlingDto behandlingDto;
    private Status status;

    public FagSakTypeDto getFagSakTypeDto() {
        return fagSakTypeDto;
    }

    public void setFagSakTypeDto(FagSakTypeDto fagSakTypeDto) {
        this.fagSakTypeDto = fagSakTypeDto;
    }

    public BehandlingDto getBehandlingDto() {
        return behandlingDto;
    }

    public void setBehandlingDto(BehandlingDto behandlingDto) {
        this.behandlingDto = behandlingDto;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }


}
