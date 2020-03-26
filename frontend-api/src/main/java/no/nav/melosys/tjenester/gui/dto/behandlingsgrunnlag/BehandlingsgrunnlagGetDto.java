package no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsGrunnlagType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.tjenester.gui.dto.BehandlingsgrunnlagTilleggsData;
import no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.data.BehandlingsgrunnlagDataDto;
import no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.data.SedGrunnlagDto;
import no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.data.SoeknadDokumentDto;

public class BehandlingsgrunnlagGetDto {

    private final BehandlingsgrunnlagDataDto data;
    private final BehandlingsGrunnlagType type;
    private final BehandlingsgrunnlagTilleggsData tilleggsData;

    public BehandlingsgrunnlagGetDto(Behandlingsgrunnlag behandlingsgrunnlag, BehandlingsgrunnlagTilleggsData tilleggsData) {
        this(hentBehandlingsgrunnlagData(behandlingsgrunnlag), behandlingsgrunnlag.getType(), tilleggsData);
    }

    private static BehandlingsgrunnlagDataDto  hentBehandlingsgrunnlagData(Behandlingsgrunnlag behandlingsgrunnlag) {
        if (behandlingsgrunnlag.erSøknad()) {
            return new SoeknadDokumentDto((SoeknadDokument) behandlingsgrunnlag.getBehandlingsgrunnlagdata());
        } else if (behandlingsgrunnlag.erSedGrunnlag()) {
            return new SedGrunnlagDto((SedGrunnlag) behandlingsgrunnlag.getBehandlingsgrunnlagdata());
        }

        return new BehandlingsgrunnlagDataDto(behandlingsgrunnlag.getBehandlingsgrunnlagdata());
    }

    private BehandlingsgrunnlagGetDto(BehandlingsgrunnlagDataDto data, BehandlingsGrunnlagType type, BehandlingsgrunnlagTilleggsData tilleggsData) {
        this.data = data;
        this.type = type;
        this.tilleggsData = tilleggsData;
    }

    public BehandlingsgrunnlagDataDto getData() {
        return data;
    }

    public BehandlingsGrunnlagType getType() {
        return type;
    }

    public BehandlingsgrunnlagTilleggsData getTilleggsData() {
        return tilleggsData;
    }
}
