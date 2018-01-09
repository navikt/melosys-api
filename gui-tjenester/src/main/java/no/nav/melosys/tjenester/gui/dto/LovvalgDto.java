package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.regler.api.lovvalg.rep.FastsettLovvalgReply;

public class LovvalgDto {

    private long behandlingId;

    private FastsettLovvalgReply vurdering;

    public LovvalgDto(long behandlingID, FastsettLovvalgReply fastsettLovvalgReply) {
        this.behandlingId = behandlingID;
        this.vurdering = fastsettLovvalgReply;
    }

    public long getBehandlingId() {
        return behandlingId;
    }

    public void setBehandlingId(long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public FastsettLovvalgReply getVurdering() {
        return vurdering;
    }

    public void setVurdering(FastsettLovvalgReply vurdering) {
        this.vurdering = vurdering;
    }
}
