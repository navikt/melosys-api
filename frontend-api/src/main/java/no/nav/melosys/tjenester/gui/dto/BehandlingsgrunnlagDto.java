package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsGrunnlagType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;

public class BehandlingsgrunnlagDto {

    private BehandlingsgrunnlagData behandlingsgrunnlagData;

    private BehandlingsGrunnlagType behandlingsGrunnlagType;

    public BehandlingsgrunnlagDto(Behandlingsgrunnlag behandlingsgrunnlag) {
        this(behandlingsgrunnlag.getBehandlingsgrunnlagdata(), behandlingsgrunnlag.getType());
    }

    public BehandlingsgrunnlagDto(BehandlingsgrunnlagData behandlingsgrunnlagData, BehandlingsGrunnlagType behandlingsGrunnlagType) {
        this.behandlingsgrunnlagData = behandlingsgrunnlagData;
        this.behandlingsGrunnlagType = behandlingsGrunnlagType;
    }

    public BehandlingsgrunnlagData getBehandlingsgrunnlagData() {
        return behandlingsgrunnlagData;
    }

    public void setBehandlingsgrunnlagData(BehandlingsgrunnlagData behandlingsgrunnlagData) {
        this.behandlingsgrunnlagData = behandlingsgrunnlagData;
    }

    public BehandlingsGrunnlagType getBehandlingsGrunnlagType() {
        return behandlingsGrunnlagType;
    }

    public void setBehandlingsGrunnlagType(BehandlingsGrunnlagType behandlingsGrunnlagType) {
        this.behandlingsGrunnlagType = behandlingsGrunnlagType;
    }
}
