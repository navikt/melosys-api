package no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsGrunnlagType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;

public class BehandlingsgrunnlagGetDto {

    private BehandlingsgrunnlagData data;

    private BehandlingsGrunnlagType type;

    public BehandlingsgrunnlagGetDto(Behandlingsgrunnlag behandlingsgrunnlag) {
        this(behandlingsgrunnlag.getBehandlingsgrunnlagdata(), behandlingsgrunnlag.getType());
    }

    public BehandlingsgrunnlagGetDto(BehandlingsgrunnlagData data, BehandlingsGrunnlagType type) {
        this.data = data;
        this.type = type;
    }

    public BehandlingsgrunnlagData getData() {
        return data;
    }

    public void setData(BehandlingsgrunnlagData data) {
        this.data = data;
    }

    public BehandlingsGrunnlagType getType() {
        return type;
    }

    public void setType(BehandlingsGrunnlagType type) {
        this.type = type;
    }
}
