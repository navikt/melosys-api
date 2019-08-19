package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;

public class BehandlingsstatusDto {

    private Behandlingsstatus behandlingsstatus;

    public Behandlingsstatus getBehandlingsstatus() {
        return behandlingsstatus;
    }

    public void setBehandlingsstatus(Behandlingsstatus behandlingsstatus) {
        this.behandlingsstatus = behandlingsstatus;
    }
}
