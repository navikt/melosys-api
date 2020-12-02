package no.nav.melosys.integrasjon.trygdeavgift.dto;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Saerligeavgiftsgrupper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public class BeregningsgrunnlagDto {
    private final Boolean arbeidsgiverBetalerAvgift;
    private final Boolean sokerErSkattepliktig;
    private final Trygdedekninger trygdedekning;
    private final Folketrygdloven_kap2_bestemmelser bestemmelse;
    private final long maanedsbelop;
    private final LocalDate beregningsdato;
    private final Saerligeavgiftsgrupper saerligAvgiftsGruppe;

    public BeregningsgrunnlagDto(Boolean arbeidsgiverBetalerAvgift,
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
}