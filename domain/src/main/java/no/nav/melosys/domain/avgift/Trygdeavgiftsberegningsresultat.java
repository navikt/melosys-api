package no.nav.melosys.domain.avgift;

import java.util.Collection;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;

@Deprecated(since = "Skal fjernes med ny lagring av trygdeavgift: MELOSYS-5827")
public class Trygdeavgiftsberegningsresultat {
    private final Long avgiftspliktigLønnNorge;
    private final Long avgiftspliktigLønnUtland;
    private final Aktoer betalesAv;
    private final Collection<Avgiftsperiode> avgiftsperioder;

    public Trygdeavgiftsberegningsresultat(Long avgiftspliktigLønnNorge,
                                           Long avgiftspliktigLønnUtland,
                                           Aktoer betalesAv, Collection<Avgiftsperiode> avgiftsperioder) {
        this.avgiftspliktigLønnNorge = avgiftspliktigLønnNorge;
        this.avgiftspliktigLønnUtland = avgiftspliktigLønnUtland;
        this.betalesAv = betalesAv;
        this.avgiftsperioder = avgiftsperioder;
    }

    public Long getAvgiftspliktigLønnNorge() {
        return avgiftspliktigLønnNorge;
    }

    public Long getAvgiftspliktigLønnUtland() {
        return avgiftspliktigLønnUtland;
    }

    public Aktoer getBetalesAv() {
        return betalesAv;
    }

    public Collection<Avgiftsperiode> getAvgiftsperioder() {
        return avgiftsperioder;
    }

    public boolean harAvgiftspliktigInntekt() {
        return !((getAvgiftspliktigLønnNorge() == null || getAvgiftspliktigLønnNorge() == 0) &&
            (getAvgiftspliktigLønnUtland() == null || getAvgiftspliktigLønnUtland() == 0));
    }

    public boolean erIkkeSelvbetalendeBruker() {
        return (getBetalesAv() != null && getBetalesAv().getRolle() != BRUKER);
    }

    public static Trygdeavgiftsberegningsresultat lag(MedlemAvFolketrygden medlemAvFolketrygden) {
        final var fastsattTrygdeavgift = medlemAvFolketrygden.getFastsattTrygdeavgift() != null
            ? medlemAvFolketrygden.getFastsattTrygdeavgift()
            : new FastsattTrygdeavgift();

        return new Trygdeavgiftsberegningsresultat(
            fastsattTrygdeavgift.getAvgiftspliktigNorskInntektMnd(),
            fastsattTrygdeavgift.getAvgiftspliktigUtenlandskInntektMnd(),
            fastsattTrygdeavgift.getBetalesAv(),
            medlemAvFolketrygden.getMedlemskapsperioder()
                .stream()
                .flatMap(m -> Avgiftsperiode.lagAvgiftsperioder(m).stream())
                .collect(Collectors.toSet())
        );
    }

}
