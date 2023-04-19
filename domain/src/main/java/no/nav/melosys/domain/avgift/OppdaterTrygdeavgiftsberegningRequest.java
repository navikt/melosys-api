package no.nav.melosys.domain.avgift;

@Deprecated(since = "Skal fjernes med ny lagring av trygdeavgift: MELOSYS-5827")
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
