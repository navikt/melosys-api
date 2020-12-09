package no.nav.melosys.tjenester.gui.dto.trygdeavgift;

import java.time.LocalDate;

import no.nav.melosys.domain.avgift.Avgiftsperiode;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public class AvgiftsperiodeDto {
    private final LocalDate fom;
    private final LocalDate tom;
    private final Trygdedekninger trygdedekning;
    private final double avgiftssats;
    private final double avgiftPerMd;

    public AvgiftsperiodeDto(LocalDate fom,
                             LocalDate tom,
                             Trygdedekninger trygdedekning,
                             double avgiftssats,
                             double avgiftPerMd) {
        this.fom = fom;
        this.tom = tom;
        this.trygdedekning = trygdedekning;
        this.avgiftssats = avgiftssats;
        this.avgiftPerMd = avgiftPerMd;
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

    public double getAvgiftssats() {
        return avgiftssats;
    }

    public double getAvgiftPerMd() {
        return avgiftPerMd;
    }

    static AvgiftsperiodeDto av(Avgiftsperiode avgiftsperiode) {
        return new AvgiftsperiodeDto(
            avgiftsperiode.getFom(),
            avgiftsperiode.getTom(),
            avgiftsperiode.getTrygdedekning(),
            avgiftsperiode.getAvgiftssats(),
            avgiftsperiode.getAvgiftPerMd()
        );
    }
}
