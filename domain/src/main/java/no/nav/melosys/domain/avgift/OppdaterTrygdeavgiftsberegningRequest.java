package no.nav.melosys.domain.avgift;

public class OppdaterTrygdeavgiftsberegningRequest {
    private final Long avgiftspliktigLønnNorge;
    private final Long avgiftspliktigLønnUtland;

    public OppdaterTrygdeavgiftsberegningRequest(Long avgiftspliktigLønnNorge, Long avgiftspliktigLønnUtland) {
        this.avgiftspliktigLønnNorge = avgiftspliktigLønnNorge;
        this.avgiftspliktigLønnUtland = avgiftspliktigLønnUtland;
    }

    public Long getAvgiftspliktigLønnNorge() {
        return avgiftspliktigLønnNorge;
    }

    public Long getAvgiftspliktigLønnUtland() {
        return avgiftspliktigLønnUtland;
    }
}