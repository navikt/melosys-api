package no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag;

import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.kodeverk.Behandlingsgrunnlagtyper;

public class BehandlingsgrunnlagGetDto {

    private final BehandlingsgrunnlagData data;
    private final Behandlingsgrunnlagtyper type;

    public BehandlingsgrunnlagGetDto(Behandlingsgrunnlag behandlingsgrunnlag) {
        this.data = behandlingsgrunnlag.getBehandlingsgrunnlagdata();
        this.type = behandlingsgrunnlag.getType();
    }

    public BehandlingsgrunnlagData getData() {
        return data;
    }

    public Behandlingsgrunnlagtyper getType() {
        return type;
    }

}
