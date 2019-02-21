package no.nav.melosys.tjenester.gui.dto;

import java.util.List;

public class BehandlingDto {

    private BehandlingOppsummeringDto oppsummering;

    private SaksopplysningerDto saksopplysninger;

    private List<BehandlingHistorikkDto> behandlingshistorikk;

    private boolean redigerbart = false;

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

    public boolean isRedigerbart() {
        return redigerbart;
    }

    public void setRedigerbart(boolean redigerbart) {
        this.redigerbart = redigerbart;
    }

}
