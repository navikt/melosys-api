package no.nav.melosys.tjenester.gui.dto.trygdeavgift;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.avgift.OppdaterTrygdeavgiftsgrunnlagRequest;
import no.nav.melosys.domain.kodeverk.Loenn_forhold;

@Deprecated(since = "Skal fjernes med ny lagring av trygdeavgift: MELOSYS-5827")
public class OppdaterAvgiftsgrunnlagDto {
    private final Loenn_forhold lønnsforhold;
    private final AvgiftsgrunnlagInfoDto trygdeavgiftsgrunnlagNorge;
    private final AvgiftsgrunnlagInfoDto trygdeavgiftsgrunnlagUtland;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OppdaterAvgiftsgrunnlagDto(@JsonProperty("lønnsforhold") Loenn_forhold lønnsforhold,
                                      @JsonProperty("trygdeavgiftsgrunnlagNorge") AvgiftsgrunnlagInfoDto trygdeavgiftsgrunnlagNorge,
                                      @JsonProperty("trygdeavgiftsgrunnlagUtland") AvgiftsgrunnlagInfoDto trygdeavgiftsgrunnlagUtland) {
        this.lønnsforhold = lønnsforhold;
        this.trygdeavgiftsgrunnlagNorge = trygdeavgiftsgrunnlagNorge;
        this.trygdeavgiftsgrunnlagUtland = trygdeavgiftsgrunnlagUtland;
    }

    public Loenn_forhold getLønnsforhold() {
        return lønnsforhold;
    }

    public AvgiftsgrunnlagInfoDto getTrygdeavgiftsgrunnlagNorge() {
        return trygdeavgiftsgrunnlagNorge;
    }

    public AvgiftsgrunnlagInfoDto getTrygdeavgiftsgrunnlagUtland() {
        return trygdeavgiftsgrunnlagUtland;
    }

    public OppdaterTrygdeavgiftsgrunnlagRequest til() {
        return new OppdaterTrygdeavgiftsgrunnlagRequest(
            this.lønnsforhold,
            this.trygdeavgiftsgrunnlagNorge != null ? this.trygdeavgiftsgrunnlagNorge.til() : null,
            this.trygdeavgiftsgrunnlagUtland != null ? this.trygdeavgiftsgrunnlagUtland.til() : null
        );
    }
}
