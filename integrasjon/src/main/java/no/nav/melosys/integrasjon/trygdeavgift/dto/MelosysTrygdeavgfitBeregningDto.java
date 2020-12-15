package no.nav.melosys.integrasjon.trygdeavgift.dto;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Saerligeavgiftsgrupper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public class MelosysTrygdeavgfitBeregningDto {
    private final Boolean arbeidsgiverBetalerAvgift;
    private final Boolean sokerErSkattepliktig;
    private final Trygdedekninger trygdedekning;
    private final Folketrygdloven_kap2_bestemmelser bestemmelse;
    private final long maanedsbelop;
    private final LocalDate beregningsdato;
    private final Saerligeavgiftsgrupper saerligAvgiftsGruppe;

    public MelosysTrygdeavgfitBeregningDto(Boolean arbeidsgiverBetalerAvgift,
                                           Boolean sokerErSkattepliktig,
                                           Trygdedekninger trygdedekning,
                                           Folketrygdloven_kap2_bestemmelser bestemmelse,
                                           long maanedsbelop,
                                           LocalDate beregningsdato,
                                           Saerligeavgiftsgrupper saerligAvgiftsGruppe) {
        this.arbeidsgiverBetalerAvgift = arbeidsgiverBetalerAvgift;
        this.sokerErSkattepliktig = sokerErSkattepliktig;
        this.trygdedekning = trygdedekning;
        this.bestemmelse = bestemmelse;
        this.maanedsbelop = maanedsbelop;
        this.beregningsdato = beregningsdato;
        this.saerligAvgiftsGruppe = saerligAvgiftsGruppe;
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

    public LocalDate getBeregningsdato() {
        return beregningsdato;
    }

    public Saerligeavgiftsgrupper getSaerligAvgiftsGruppe() {
        return saerligAvgiftsGruppe;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MelosysTrygdeavgfitBeregningDto that = (MelosysTrygdeavgfitBeregningDto) o;
        return maanedsbelop == that.maanedsbelop &&
            Objects.equals(arbeidsgiverBetalerAvgift, that.arbeidsgiverBetalerAvgift) &&
            Objects.equals(sokerErSkattepliktig, that.sokerErSkattepliktig) &&
            trygdedekning == that.trygdedekning &&
            bestemmelse == that.bestemmelse &&
            Objects.equals(beregningsdato, that.beregningsdato) &&
            saerligAvgiftsGruppe == that.saerligAvgiftsGruppe;
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiverBetalerAvgift, sokerErSkattepliktig, trygdedekning, bestemmelse, maanedsbelop, beregningsdato, saerligAvgiftsGruppe);
    }
}