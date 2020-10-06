package no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsGrunnlagType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;

public class BehandlingsgrunnlagGetDto {

    private final BehandlingsgrunnlagData data;
    private final BehandlingsGrunnlagType type;

    public BehandlingsgrunnlagGetDto(Behandlingsgrunnlag behandlingsgrunnlag) {
        this(behandlingsgrunnlag.getBehandlingsgrunnlagdata(), behandlingsgrunnlag.getType());
    }

    private BehandlingsgrunnlagGetDto(BehandlingsgrunnlagData data, BehandlingsGrunnlagType type) {
        this.data = data;
        this.type = type;
    }

    public BehandlingsgrunnlagData getData() {
        return data;
    }

    public BehandlingsGrunnlagType getType() {
        return type;
    }
}
