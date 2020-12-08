package no.nav.melosys.tjenester.gui.dto.trygdeavgift;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.avgift.OppdaterTrygdeavgiftsgrunnlagRequest;
import no.nav.melosys.domain.kodeverk.Loenn_forhold;

public class OppdaterAvgiftsgrunnlagDto {
    private final Loenn_forhold lønnsforhold;
    private final AvgiftsgrunnlagInfoDto inntektsinformasjonNorge;
    private final AvgiftsgrunnlagInfoDto inntektsinformasjonUtland;

    @JsonCreator
    public OppdaterAvgiftsgrunnlagDto(@JsonProperty("lønnsforhold") Loenn_forhold lønnsforhold,
                                      @JsonProperty("inntektsinformasjonNorge") AvgiftsgrunnlagInfoDto inntektsinformasjonNorge,
                                      @JsonProperty("inntektsinformasjonUtland") AvgiftsgrunnlagInfoDto inntektsinformasjonUtland) {
        this.lønnsforhold = lønnsforhold;
        this.inntektsinformasjonNorge = inntektsinformasjonNorge;
        this.inntektsinformasjonUtland = inntektsinformasjonUtland;
    }

    public Loenn_forhold getLønnsforhold() {
        return lønnsforhold;
    }

    public AvgiftsgrunnlagInfoDto getInntektsinformasjonNorge() {
        return inntektsinformasjonNorge;
    }

    public AvgiftsgrunnlagInfoDto getInntektsinformasjonUtland() {
        return inntektsinformasjonUtland;
    }

    public OppdaterTrygdeavgiftsgrunnlagRequest til() {
        return new OppdaterTrygdeavgiftsgrunnlagRequest(
            this.lønnsforhold,
            this.inntektsinformasjonNorge != null ? this.inntektsinformasjonNorge.til() : null,
            this.inntektsinformasjonUtland != null ? this.inntektsinformasjonUtland.til() : null
        );
    }
}
