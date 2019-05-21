package no.nav.melosys.tjenester.gui.dto;

public class BehandlingDto {

    private Long behandlingID;

    private BehandlingOppsummeringDto oppsummering;

    private SaksopplysningerDto saksopplysninger;

    private boolean redigerbart = false;

    public Long getBehandlingID() {
        return behandlingID;
    }

    public void setBehandlingID(Long behandlingID) {
        this.behandlingID = behandlingID;
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
    public boolean isRedigerbart() {
        return redigerbart;
    }

    public void setRedigerbart(boolean redigerbart) {
        this.redigerbart = redigerbart;
    }

}
