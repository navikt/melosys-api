package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.regler.api.lovvalg.rep.FastsettLovvalgReply;

public class LovvalgDto {

    private long behandlingID;

    private FastsettLovvalgReply vurdering;

    public LovvalgDto(long behandlingID, FastsettLovvalgReply fastsettLovvalgReply) {
        this.behandlingID = behandlingID;
        this.vurdering = fastsettLovvalgReply;
        // FIXME: Midlertidig løsning for å støtte kontrakten med frontend. Implementeres i regelmodul?
        this.vurdering.feilmeldinger.forEach(feilmelding -> {
            feilmelding.alvorlighetsgrad = "INFO";
        });
    }

    public long getBehandlingID() {
        return behandlingID;
    }

    public void setBehandlingID(long behandlingId) {
        this.behandlingID = behandlingId;
    }

    public FastsettLovvalgReply getVurdering() {
        return vurdering;
    }

    public void setVurdering(FastsettLovvalgReply vurdering) {
        this.vurdering = vurdering;
    }
}
