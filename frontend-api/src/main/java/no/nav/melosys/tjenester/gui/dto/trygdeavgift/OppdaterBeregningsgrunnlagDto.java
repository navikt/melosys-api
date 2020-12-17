package no.nav.melosys.tjenester.gui.dto.trygdeavgift;

import no.nav.melosys.domain.avgift.OppdaterTrygdeavgiftsberegningRequest;

public class OppdaterBeregningsgrunnlagDto {
    private final Long avgiftspliktigLønnNorge;
    private final Long avgiftspliktigLønnUtland;

    public OppdaterBeregningsgrunnlagDto(Long avgiftspliktigLønnNorge, Long avgiftspliktigLønnUtland) {
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
