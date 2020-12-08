package no.nav.melosys.domain.avgift;

import java.util.Collection;
import java.util.stream.Collectors;

import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;

public class Trygdeavgiftsberegningsresultat {
    private final Long avgiftspliktigLønnNorge;
    private final Long avgiftspliktigLønnUtland;
    private final Collection<Avgiftsperiode> avgiftsperioder;

    public Trygdeavgiftsberegningsresultat(Long avgiftspliktigLønnNorge,
                                           Long avgiftspliktigLønnUtland,
                                           Collection<Avgiftsperiode> avgiftsperioder) {
        this.avgiftspliktigLønnNorge = avgiftspliktigLønnNorge;
        this.avgiftspliktigLønnUtland = avgiftspliktigLønnUtland;
        this.avgiftsperioder = avgiftsperioder;
    }

    public Long getAvgiftspliktigLønnNorge() {
        return avgiftspliktigLønnNorge;
    }

    public Long getAvgiftspliktigLønnUtland() {
        return avgiftspliktigLønnUtland;
    }

    public Collection<Avgiftsperiode> getAvgiftsperioder() {
        return avgiftsperioder;
    }

    public static Trygdeavgiftsberegningsresultat lag(MedlemAvFolketrygden medlemAvFolketrygden) {
        final var fastsattTrygdeavgift = medlemAvFolketrygden.getFastsattTrygdeavgift() != null
            ? medlemAvFolketrygden.getFastsattTrygdeavgift()
            : new FastsattTrygdeavgift();

        return new Trygdeavgiftsberegningsresultat(
            fastsattTrygdeavgift.getAvgiftspliktigNorskInntektMnd(),
            fastsattTrygdeavgift.getAvgiftspliktigUtenlandskInntektMnd(),
            medlemAvFolketrygden.getMedlemskapsperioder()
                .stream()
                .flatMap(m -> Avgiftsperiode.lagAvgiftsperioder(m).stream())
                .collect(Collectors.toSet())
        );
    }

}