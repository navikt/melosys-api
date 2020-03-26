package no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsGrunnlagType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.tjenester.gui.dto.BehandlingsgrunnlagTilleggsData;

public class BehandlingsgrunnlagGetDto {

    private final BehandlingsgrunnlagData data;
    private final BehandlingsGrunnlagType type;
    private final BehandlingsgrunnlagTilleggsData tilleggsData;

    public BehandlingsgrunnlagGetDto(Behandlingsgrunnlag behandlingsgrunnlag, BehandlingsgrunnlagTilleggsData tilleggsData) {
        this(hentBehandlingsgrunnlagData(behandlingsgrunnlag), behandlingsgrunnlag.getType(), tilleggsData);
    }

    private static BehandlingsgrunnlagData hentBehandlingsgrunnlagData(Behandlingsgrunnlag behandlingsgrunnlag) {
        if (behandlingsgrunnlag.erSedGrunnlag()) {
            return new SedGrunnlagDto((SedGrunnlag) behandlingsgrunnlag.getBehandlingsgrunnlagdata());
        }

        return behandlingsgrunnlag.getBehandlingsgrunnlagdata();
    }

    private BehandlingsgrunnlagGetDto(BehandlingsgrunnlagData data, BehandlingsGrunnlagType type, BehandlingsgrunnlagTilleggsData tilleggsData) {
        this.data = data;
        this.type = type;
        this.tilleggsData = tilleggsData;
    }

    public BehandlingsgrunnlagData getData() {
        return data;
    }

    public BehandlingsGrunnlagType getType() {
        return type;
    }

    public BehandlingsgrunnlagTilleggsData getTilleggsData() {
        return tilleggsData;
    }
}
