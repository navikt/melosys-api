package no.nav.melosys.tjenester.gui.dto.trygdeavgift;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.avgift.OppdaterTrygdeavgiftsberegningRequest;

@Deprecated(since = "Skal fjernes med ny lagring av trygdeavgift: MELOSYS-5827")
public class OppdaterBeregningsgrunnlagDto {
    private final Long avgiftspliktigLønnNorge;
    private final Long avgiftspliktigLønnUtland;

    @JsonCreator
    public OppdaterBeregningsgrunnlagDto(@JsonProperty("avgiftspliktigLønnNorge") Long avgiftspliktigLønnNorge,
                                         @JsonProperty("avgiftspliktigLønnUtland") Long avgiftspliktigLønnUtland) {
        this.avgiftspliktigLønnNorge = avgiftspliktigLønnNorge;
        this.avgiftspliktigLønnUtland = avgiftspliktigLønnUtland;
    }

    public Long getAvgiftspliktigLønnNorge() {
        return avgiftspliktigLønnNorge;
    }

    public Long getAvgiftspliktigLønnUtland() {
        return avgiftspliktigLønnUtland;
    }

    public OppdaterTrygdeavgiftsberegningRequest til() {
        return new OppdaterTrygdeavgiftsberegningRequest(
            this.avgiftspliktigLønnNorge,
            this.avgiftspliktigLønnUtland
        );
    }
}
