package no.nav.melosys.tjenester.gui.dto;

import java.util.List;

public class BehandlingDto {

    private long id;

    private BehandlingOppsummeringDto oppsummering;

    private SaksopplysningerDto saksopplysninger;

    private List<BehandlingHistorikkDto> behandlingshistorikk;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public BehandlingOppsummeringDto getOppsummering() {
        return oppsummering;
    }

    public void setOppsummering(BehandlingOppsummeringDto oppsummering) {
        this.oppsummering = oppsummering;
    }

    public SaksopplysningerDto getSaksopplysninger() {
        return saksopplysninger;
    }

    public void setSaksopplysninger(SaksopplysningerDto saksopplysninger) {
        this.saksopplysninger = saksopplysninger;
    }

    public List<BehandlingHistorikkDto> getBehandlingshistorikk() {
        return behandlingshistorikk;
    }

    public void setBehandlingshistorikk(List<BehandlingHistorikkDto> behandlingshistorikk) {
        this.behandlingshistorikk = behandlingshistorikk;
    }

}
