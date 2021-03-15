package no.nav.melosys.domain.avgift;

import java.util.Collection;
import java.util.stream.Collectors;

import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static org.springframework.util.ObjectUtils.isEmpty;

public class Trygdeavgiftsberegningsresultat {
    private final Long avgiftspliktigLønnNorge;
    private final Long avgiftspliktigLønnUtland;
    private final Collection<Avgiftsperiode> avgiftsperioder;
    private final boolean selvbetalendeBruker;

    public Trygdeavgiftsberegningsresultat(Long avgiftspliktigLønnNorge,
                                           Long avgiftspliktigLønnUtland,
                                           boolean selvbetalendeBruker,
                                           Collection<Avgiftsperiode> avgiftsperioder) {
        this.avgiftspliktigLønnNorge = avgiftspliktigLønnNorge;
        this.avgiftspliktigLønnUtland = avgiftspliktigLønnUtland;
        this.selvbetalendeBruker = selvbetalendeBruker;
        this.avgiftsperioder = avgiftsperioder;
    }

    public Long getAvgiftspliktigLønnNorge() {
        return avgiftspliktigLønnNorge;
    }

    public Long getAvgiftspliktigLønnUtland() {
        return avgiftspliktigLønnUtland;
    }

    public boolean erSelvbetalendeBruker() {
        return selvbetalendeBruker;
    }

    public Collection<Avgiftsperiode> getAvgiftsperioder() {
        return avgiftsperioder;
    }

    public boolean harAvgiftspliktigInntekt() {
        return !((this.getAvgiftspliktigLønnNorge() == null || this.getAvgiftspliktigLønnNorge() == 0) &&
            (this.getAvgiftspliktigLønnUtland() == null || this.getAvgiftspliktigLønnUtland() == 0));
    }

    public boolean ikkeSelvbetalendeBruker() {
        return !this.erSelvbetalendeBruker();
    }

    public static Trygdeavgiftsberegningsresultat lag(MedlemAvFolketrygden medlemAvFolketrygden) {
        final var fastsattTrygdeavgift = medlemAvFolketrygden.getFastsattTrygdeavgift() != null
            ? medlemAvFolketrygden.getFastsattTrygdeavgift()
            : new FastsattTrygdeavgift();

        return new Trygdeavgiftsberegningsresultat(
            fastsattTrygdeavgift.getAvgiftspliktigNorskInntektMnd(),
            fastsattTrygdeavgift.getAvgiftspliktigUtenlandskInntektMnd(),
            isEmpty(fastsattTrygdeavgift.getBetalesAv()),
            medlemAvFolketrygden.getMedlemskapsperioder()
                .stream()
                .flatMap(m -> Avgiftsperiode.lagAvgiftsperioder(m).stream())
                .collect(Collectors.toSet())
        );
    }

}