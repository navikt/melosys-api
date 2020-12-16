package no.nav.melosys.domain.avgift;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public class Avgiftsperiode {
    private final LocalDate fom;
    private final LocalDate tom;
    private final Trygdedekninger trygdedekning;
    private final BigDecimal avgiftssats;
    private final BigDecimal avgiftPerMd;
    private final boolean forNorskInntekt;

    public Avgiftsperiode(LocalDate fom,
                          LocalDate tom,
                          Trygdedekninger trygdedekning,
                          BigDecimal avgiftssats,
                          BigDecimal avgiftPerMd,
                          boolean forNorskInntekt) {
        this.fom = fom;
        this.tom = tom;
        this.trygdedekning = trygdedekning;
        this.avgiftssats = avgiftssats;
        this.avgiftPerMd = avgiftPerMd;
        this.forNorskInntekt = forNorskInntekt;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public Trygdedekninger getTrygdedekning() {
        return trygdedekning;
    }

    public BigDecimal getAvgiftssats() {
        return avgiftssats;
    }

    public BigDecimal getAvgiftPerMd() {
        return avgiftPerMd;
    }

    public boolean isForNorskInntekt() {
        return forNorskInntekt;
    }

    public static Collection<Avgiftsperiode> lagAvgiftsperioder(Medlemskapsperiode medlemskapsperiode) {
        return medlemskapsperiode.getTrygdeavgift()
            .stream()
            .map(trygdeavgift -> lagAvgiftsperiode(medlemskapsperiode, trygdeavgift))
            .collect(Collectors.toSet());
    }

    private static Avgiftsperiode lagAvgiftsperiode(Medlemskapsperiode medlemskapsperiode,
                                                    Trygdeavgift trygdeavgift) {
        return new Avgiftsperiode(
            medlemskapsperiode.getFom(),
            medlemskapsperiode.getTom(),
            medlemskapsperiode.getTrygdedekning(),
            trygdeavgift.getTrygdesats(),
            trygdeavgift.getTrygdeavgiftsbeløpMd(),
            trygdeavgift.erAvgiftForNorskInntekt()
        );
    }
}