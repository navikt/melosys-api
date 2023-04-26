package no.nav.melosys.integrasjon.trygdeavgift.dto;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Saerligeavgiftsgrupper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public class MelosysTrygdeavgfitBeregningV1Dto {
    private final Boolean arbeidsgiverBetalerAvgift;
    private final Boolean sokerErSkattepliktig;
    private final Trygdedekninger trygdedekning;
    private final Folketrygdloven_kap2_bestemmelser bestemmelse;
    private final long maanedsbelop;
    private final Saerligeavgiftsgrupper saerligAvgiftsGruppe;
    private final LocalDate beregningsperiodeFom;
    private final LocalDate beregningsperiodeTom;

    public MelosysTrygdeavgfitBeregningV1Dto(Boolean arbeidsgiverBetalerAvgift,
                                             Boolean sokerErSkattepliktig,
                                             Trygdedekninger trygdedekning,
                                             Folketrygdloven_kap2_bestemmelser bestemmelse,
                                             long maanedsbelop,
                                             Saerligeavgiftsgrupper saerligAvgiftsGruppe,
                                             LocalDate beregningsperiodeFom,
                                             LocalDate beregningsperiodeTom) {
        this.arbeidsgiverBetalerAvgift = arbeidsgiverBetalerAvgift;
        this.sokerErSkattepliktig = sokerErSkattepliktig;
        this.trygdedekning = trygdedekning;
        this.bestemmelse = bestemmelse;
        this.maanedsbelop = maanedsbelop;
        this.saerligAvgiftsGruppe = saerligAvgiftsGruppe;
        this.beregningsperiodeFom = beregningsperiodeFom;
        this.beregningsperiodeTom = beregningsperiodeTom;
    }

    public Boolean getArbeidsgiverBetalerAvgift() {
        return arbeidsgiverBetalerAvgift;
    }

    public Boolean getSokerErSkattepliktig() {
        return sokerErSkattepliktig;
    }

    public Trygdedekninger getTrygdedekning() {
        return trygdedekning;
    }

    public Folketrygdloven_kap2_bestemmelser getBestemmelse() {
        return bestemmelse;
    }

    public long getMaanedsbelop() {
        return maanedsbelop;
    }

    public Saerligeavgiftsgrupper getSaerligAvgiftsGruppe() {
        return saerligAvgiftsGruppe;
    }

    public LocalDate getBeregningsperiodeFom() {
        return beregningsperiodeFom;
    }

    public LocalDate getBeregningsperiodeTom() {
        return beregningsperiodeTom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MelosysTrygdeavgfitBeregningV1Dto that = (MelosysTrygdeavgfitBeregningV1Dto) o;
        return maanedsbelop == that.maanedsbelop &&
            Objects.equals(arbeidsgiverBetalerAvgift, that.arbeidsgiverBetalerAvgift) &&
            Objects.equals(sokerErSkattepliktig, that.sokerErSkattepliktig) &&
            trygdedekning == that.trygdedekning &&
            bestemmelse == that.bestemmelse &&
            saerligAvgiftsGruppe == that.saerligAvgiftsGruppe &&
            Objects.equals(beregningsperiodeFom, that.beregningsperiodeFom) &&
            Objects.equals(beregningsperiodeTom, that.beregningsperiodeTom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiverBetalerAvgift, sokerErSkattepliktig, trygdedekning, bestemmelse, maanedsbelop, saerligAvgiftsGruppe, beregningsperiodeFom, beregningsperiodeTom);
    }
}
